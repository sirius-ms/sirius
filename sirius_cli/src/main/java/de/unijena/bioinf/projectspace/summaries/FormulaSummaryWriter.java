/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.projectspace.summaries;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Score;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.TreeStatistics;
import de.unijena.bioinf.GibbsSampling.ZodiacScore;
import de.unijena.bioinf.fingerid.ConfidenceScore;
import de.unijena.bioinf.fingerid.blast.TopCSIScore;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.projectspace.*;
import de.unijena.bioinf.sirius.FTreeMetricsHelper;
import de.unijena.bioinf.sirius.scores.IsotopeScore;
import de.unijena.bioinf.sirius.scores.SiriusScore;
import de.unijena.bioinf.sirius.scores.TreeScore;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class FormulaSummaryWriter implements Summarizer {

    final static List<Class<? extends FormulaScore>> RANKING_SCORES = List.of(ZodiacScore.class, SiriusScore.class, TreeScore.class, IsotopeScore.class, TopCSIScore.class);
    final static List<Class<? extends FormulaScore>> RANKING_SCORES_SELECTING_TOP1 = List.of(ZodiacScore.class, SiriusScore.class, TreeScore.class, IsotopeScore.class);

    private final Lock lock = new ReentrantLock();

    final LinkedHashMap<Class<? extends FormulaScore>, String> globalTypes = new LinkedHashMap<>();
    final Set<ResultEntry> globalResults = new HashSet<>();
    final Set<ResultEntry> globalResultsAllAdducts = new HashSet<>();

    @Override
    public List<Class<? extends DataAnnotation>> requiredFormulaResultAnnotations() {
        return Arrays.asList(
                FormulaScoring.class,
                FTree.class
        );
    }


    @Override
    public void addWriteCompoundSummary(ProjectWriter writer, @NotNull CompoundContainer exp, List<? extends SScored<FormulaResult, ? extends FormulaScore>> formulaResults) throws IOException {
        if (formulaResults == null || formulaResults.isEmpty())
            return;

        List<SScored<FormulaResult, ? extends FormulaScore>> results = FormulaScoring.reRankBy(formulaResults, RANKING_SCORES, true);
        List<SScored<FormulaResult, ? extends FormulaScore>> topResultWithAdducts = extractAllTopScoringResults(results, RANKING_SCORES_SELECTING_TOP1);

        if (results.isEmpty())
            return;

        writer.inDirectory(exp.getId().getDirectoryName(), () -> {
            writer.textFile(SummaryLocations.FORMULA_CANDIDATES, w -> {
                LinkedHashMap<Class<? extends FormulaScore>, String> types = new LinkedHashMap<>();

                final AtomicBoolean first = new AtomicBoolean(true);

                results.forEach(r -> r.getCandidate().getAnnotation(FormulaScoring.class)
                        .ifPresent(s -> {
                            if (first.getAndSet(false)) {
                                FormulaResult bestResult = topResultWithAdducts.size()>1 ? resolveIonizationOnly(r.getCandidate()) : r.getCandidate();
                                withLock(() -> this.globalResults.add(ResultEntry.of(bestResult, exp)));
                            }
                            s.annotations().forEach((key, value) -> {
                                if (value != null && !value.isNa()) {
                                    types.putIfAbsent(value.getClass(), value.name());
                                    withLock(() -> this.globalTypes.putIfAbsent(value.getClass(), value.name()));
                                }
                            });
                        }));

                //writing stuff
                types.remove(TopCSIScore.class);
                types.remove(ConfidenceScore.class);
                writeCSV(w, types, results.stream().map(s -> new SScored<>(ResultEntry.of(s.getCandidate(),exp),s.getScoreObject())).collect(Collectors.toList()), false);
            });

            return true;
        });

        withLock(() -> topResultWithAdducts.forEach(r -> this.globalResultsAllAdducts.add(ResultEntry.of(r.getCandidate(), exp))));
    }

    private FormulaResult resolveIonizationOnly(FormulaResult r) {
        FormulaResultId rid = r.getId();
        FormulaResultId newRid = new FormulaResultId(rid.getParentId(), rid.getPrecursorFormula(), PrecursorIonType.getPrecursorIonType(rid.getIonType().getIonization()));
        FormulaResult newResult = new FormulaResult(newRid);
        r.annotations().forEach(newResult::setAnnotation);
        return newResult;
    }

    private List<SScored<FormulaResult, ? extends FormulaScore>> extractAllTopScoringResults(List<SScored<FormulaResult, ? extends FormulaScore>> sortedResults, List<Class<? extends FormulaScore>> rankingScores) {
        if (sortedResults.isEmpty()) return Collections.emptyList();
        if (sortedResults.size() == 1) return Collections.singletonList(sortedResults.get(0));

        SScored<FormulaResult, ? extends FormulaScore> best = sortedResults.get(0);
        FormulaScoring bestScore = best.getCandidate().getAnnotationOrNull(FormulaScoring.class);

        Comparator<FormulaScoring> comparator = FormulaScoring.comparingMultiScore(rankingScores, true);

        List<SScored<FormulaResult, ? extends FormulaScore>> topResultsWithAdducts = sortedResults.stream()
                .takeWhile(r -> comparator.compare(bestScore, r.getCandidate().getAnnotationOrNull(FormulaScoring.class))==0)
                .collect(Collectors.toList());

        //candidates with same score should have the same adduct.
        assert topResultsWithAdducts.stream().map(s->s.getCandidate().getId().getIonType().getIonization()).distinct().count()==1;
        return topResultsWithAdducts;
    }

    @Override
    public void writeProjectSpaceSummary(ProjectWriter writer) throws IOException {
        lock.lock();
        try {
            globalTypes.remove(ConfidenceScore.class);
            globalTypes.remove(TopCSIScore.class);

            final List<SScored<ResultEntry, ? extends FormulaScore>> r = FormulaScoring.rankBy(globalResults.stream(), RANKING_SCORES, true, ResultEntry::getScoring);
            writer.textFile(SummaryLocations.FORMULA_SUMMARY, w -> {
                writeCSV(w, globalTypes, r, true);
            });
            final List<SScored<ResultEntry, ? extends FormulaScore>> rAdducts = FormulaScoring.rankBy(globalResultsAllAdducts.stream(), RANKING_SCORES, true, ResultEntry::getScoring);
            writer.textFile(SummaryLocations.FORMULA_SUMMARY_ADDUCTS, w -> {
                writeCSV(w, globalTypes, rAdducts, true);
            });
        } finally {
            lock.unlock();
        }
    }

    private String makeHeader(String scorings) {
        final StringBuilder headerBuilder = new StringBuilder("molecularFormula\tadduct\tprecursorFormula");/*	rankingScore*/
        if (scorings != null && !scorings.isEmpty())
            headerBuilder.append("\t").append(scorings);
        headerBuilder.append("\tnumExplainedPeaks\texplainedIntensity\tmedianMassErrorFragmentPeaks(ppm)\tmedianAbsoluteMassErrorFragmentPeaks(ppm)\tmassErrorPrecursor(ppm)");
        return headerBuilder.toString();
    }

    private void writeCSV(Writer w, LinkedHashMap<Class<? extends FormulaScore>, String> types, List<? extends SScored<? extends ResultEntry, ? extends Score<?>>> results, boolean prefix) throws IOException {
        final List<Class<? extends FormulaScore>> scoreOrder = ProjectSpaceManager.scorePriorities().stream().filter(types::containsKey).collect(Collectors.toList());
        results = results.stream()
                .sorted((i1, i2) -> FormulaScoring.comparingMultiScore(scoreOrder).compare(
                        i1.getCandidate().scoring,
                        i2.getCandidate().scoring))
                .collect(Collectors.toList());


        String header = makeHeader(scoreOrder.stream().map(types::get).collect(Collectors.joining("\t")));
        if (prefix)
            header = header + "\tionMass" + "\tretentionTimeInSeconds" + "\tid";

        w.write("rank\t" + header + "\n");

        int rank = 0;
        MolecularFormula preFormula = null;
        for (SScored<? extends ResultEntry, ? extends Score<?>> s : results) {
            ResultEntry r = s.getCandidate();
            FormulaScoring scores = r.scoring;
            if (preFormula == null || !r.preFormula.equals(preFormula))
                rank++;
            preFormula = r.preFormula;


            w.write(String.valueOf(rank));
            w.write('\t');
            w.write(r.molecularFormula.toString());
            w.write('\t');
            w.write(r.ion);
            w.write('\t');

            w.write(preFormula.toString());
            w.write('\t');

            for (Class<? extends FormulaScore> k : scoreOrder) {
                w.write(scores.getAnnotationOr(k, FormulaScore::NA).toString());
                w.write('\t');
            }
            w.write(r.treeNumVertices);
            w.write('\t');
            w.write(r.treeExplInt);
            w.write('\t');
            w.write(r.medianMassDev);
            w.write('\t');
            w.write(r.medianAbsMassDev);
            w.write('\t');
            w.write(r.massErrorPrecursor);
            if (prefix) {
                w.write('\t');
                w.write(r.ionMass);
                w.write('\t');
                w.write(r.retentionTimeSeconds);
                w.write('\t');
                w.write(r.dirName);
            }

            w.write('\n');
        }
    }

    private static class ResultEntry {
        public final FormulaScoring scoring;
        public final MolecularFormula molecularFormula;
        public final MolecularFormula preFormula;
        public final String ion;
        private final String ionMass;
        private final String retentionTimeSeconds;
        private final String dirName;

        public String treeNumVertices = "N/A";
        public String treeExplInt = "N/A";
        public String medianMassDev = "N/A";
        public String medianAbsMassDev = "N/A";
        public String massErrorPrecursor = "N/A";

        public ResultEntry(FormulaResult r, CompoundContainer exp) {
            scoring = r.getAnnotationOrThrow(FormulaScoring.class);
            molecularFormula = r.getId().getMolecularFormula();
            preFormula = r.getId().getPrecursorFormula();
            ion = Optional.ofNullable(r.getId().getIonType()).map(PrecursorIonType::toString).orElse("N/A");

            //tree stuff
            r.getAnnotation(FTree.class).ifPresent(tree -> {
                treeNumVertices = String.valueOf(tree.numberOfVertices());
                treeExplInt = String.valueOf(tree.getAnnotationOrThrow(TreeStatistics.class).getExplainedIntensity());
                medianMassDev = String.valueOf(new FTreeMetricsHelper(tree).getMedianMassDeviation().getPpm());
                medianAbsMassDev = String.valueOf(new FTreeMetricsHelper(tree).getMedianAbsoluteMassDeviation().getPpm());
                massErrorPrecursor = r.getId().getParentId().getIonMass().map(e -> tree.getMassErrorTo(tree.getRoot(), e).getPpm()).map(String::valueOf).orElse("N/A");
            });

            ionMass = BigDecimal.valueOf(exp.getId().getIonMass().orElse(Double.NaN)).setScale(5, RoundingMode.HALF_UP).toString();
            retentionTimeSeconds = String.valueOf(exp.getId().getRt().orElse(RetentionTime.NA()).getRetentionTimeInSeconds());
            dirName = exp.getId().getDirectoryName();
        }

        public FormulaScoring getScoring() {
            return scoring;
        }

        public static ResultEntry of(@NotNull FormulaResult r, @NotNull CompoundContainer exp) {
            return new ResultEntry(r, exp);
        }
    }

    private void withLock(Runnable withLock) {
        lock.lock();
        try {
            withLock.run();
        } finally {
            lock.unlock();
        }
    }
}

