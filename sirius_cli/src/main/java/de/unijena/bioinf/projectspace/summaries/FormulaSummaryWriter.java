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
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.fp.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.TreeStatistics;
import de.unijena.bioinf.ChemistryBase.utils.Utils;
import de.unijena.bioinf.GibbsSampling.ZodiacScore;
import de.unijena.bioinf.canopus.CanopusResult;
import de.unijena.bioinf.elgordo.LipidSpecies;
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

public class FormulaSummaryWriter extends CandidateSummarizer {
    public static double getCanopusScore(FormulaResult r) {
        return r.getAnnotation(CanopusResult.class).map(c -> {
            ProbabilityFingerprint fp = c.getCanopusFingerprint();
            FingerprintVersion v = fp.getFingerprintVersion();
            if (v instanceof MaskedFingerprintVersion)
                v = ((MaskedFingerprintVersion) v).getMaskedFingerprintVersion();
            return fp.getProbability(((ClassyFireFingerprintVersion) v).
                    getIndexOfMolecularProperty(((ClassyFireFingerprintVersion) v).getPrimaryClass(fp)));
        }).orElse(0d);
    }

    final static List<Class<? extends FormulaScore>> RANKING_SCORES = List.of(ZodiacScore.class, SiriusScore.class, TreeScore.class, IsotopeScore.class, TopCSIScore.class);

    public final static Comparator<FormulaResult> FROMULA_COMPARATOR = ((Comparator<FormulaResult>) (o1, o2) ->
            FormulaScoring.comparingMultiScore(RANKING_SCORES, true)
                    .compare(o1.getAnnotationOrNull(FormulaScoring.class),
                            o2.getAnnotationOrNull(FormulaScoring.class))
    ).thenComparing(Comparator.comparing(FormulaSummaryWriter::getCanopusScore).reversed());


    private final Lock lock = new ReentrantLock();

    final LinkedHashMap<Class<? extends FormulaScore>, String> globalTypes = new LinkedHashMap<>();
    final Set<ResultEntry> globalResults;
    final Set<ResultEntry> globalResultsWithAdducts;
    final Map<ResultEntry, List<ResultEntry>> globalResultsAll;

    public FormulaSummaryWriter(boolean writeTopHitGlobal, boolean writeTopHitWithAdductsGlobal, boolean writeFullGlobal) {
        super(writeTopHitGlobal, writeTopHitWithAdductsGlobal, writeFullGlobal);
        globalResults = writeTopHitGlobal ? new HashSet<>() : null;
        globalResultsWithAdducts = writeTopHitWithAdductsGlobal ? new HashSet<>() : null;
        globalResultsAll = writeFullGlobal ? new HashMap<>() : null;
    }

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

        List<FormulaResult> res = formulaResults.stream().map(SScored::getCandidate).sorted(FROMULA_COMPARATOR).toList();
        List<ResultEntry> results = new ArrayList<>(res.size());
        int rank = 0;
        MolecularFormula preFormula = null;
        for (FormulaResult c : res) {
            if (preFormula == null || !c.getId().getPrecursorFormula().equals(preFormula))
                rank++;
            preFormula = c.getId().getPrecursorFormula();

            results.add(ResultEntry.of(c, exp, rank));
        }
        List<ResultEntry> topResultWithAdducts = extractAllTopScoringResults(results);


        writer.inDirectory(exp.getId().getDirectoryName(), () -> {
            writer.textFile(SummaryLocations.FORMULA_CANDIDATES, w -> {
                LinkedHashMap<Class<? extends FormulaScore>, String> types = new LinkedHashMap<>();

                final AtomicBoolean first = new AtomicBoolean(true);

                results.forEach(c -> {
                    FormulaScoring s = c.getScoring();
                    if (first.getAndSet(false))
                        if (globalResults != null)
                            withLock(() -> this.globalResults.add(c));

                    s.annotations().forEach((key, value) -> {
                        if (value != null && !value.isNa()) {
                            types.putIfAbsent(value.getClass(), value.name());
                            withLock(() -> this.globalTypes.putIfAbsent(value.getClass(), value.name()));
                        }
                    });
                });

                //writing stuff
                types.remove(TopCSIScore.class);
                types.remove(ConfidenceScore.class);
                List<ResultEntry> r = results.stream()
                        .sorted((i1, i2) -> FormulaScoring.comparingMultiScore(
                                        SiriusProjectSpaceManager.scorePriorities()
                                                .stream().filter(types::containsKey).toList())
                                .compare(i1.scoring, i2.scoring))
                        .toList();
                writeCSV(w, types, r, false);
            });

            return true;
        });

        if (globalResultsWithAdducts != null || globalResultsAll != null)
            withLock(() -> {
                if (globalResultsWithAdducts != null)
                    this.globalResultsWithAdducts.addAll(topResultWithAdducts);
                if (globalResultsAll != null)
                    globalResultsAll.put(results.get(0), new ArrayList<>(results));
            });
    }

    private List<ResultEntry> extractAllTopScoringResults(List<ResultEntry> sortedResults) {
        if (sortedResults.isEmpty()) return Collections.emptyList();
        if (sortedResults.size() == 1) return Collections.singletonList(sortedResults.get(0));

        ResultEntry best = sortedResults.get(0);
        //candidates with same score should have the same adduct.
        return sortedResults.stream().takeWhile(r -> best.preFormula.equals(r.preFormula)).toList();
    }

    @Override
    public void writeProjectSpaceSummary(ProjectWriter writer) throws IOException {
        lock.lock();
        try {
            globalTypes.remove(ConfidenceScore.class);
            globalTypes.remove(TopCSIScore.class);

            if (globalResults != null) {
                final List<ResultEntry> r = globalResults.stream()
                        .sorted(Comparator.comparing(s -> s.dirName, Utils.ALPHANUMERIC_COMPARATOR_NULL_LAST)).toList();
                writer.textFile(SummaryLocations.FORMULA_SUMMARY, w -> writeCSV(w, globalTypes, r, true));
            }

            if (globalResultsWithAdducts != null) {
                final List<ResultEntry> rAdducts = globalResultsWithAdducts.stream()
                        .sorted(Comparator.comparing(s -> s.dirName, Utils.ALPHANUMERIC_COMPARATOR_NULL_LAST)).toList();
                writer.textFile(SummaryLocations.FORMULA_SUMMARY_ADDUCTS, w -> writeCSV(w, globalTypes, rAdducts, true));
            }

            if (globalResultsAll != null) {
                List<ResultEntry> all = globalResultsAll.keySet().stream()
                        .sorted(Comparator.comparing(s -> s.dirName, Utils.ALPHANUMERIC_COMPARATOR_NULL_LAST))
                        .flatMap(s -> globalResultsAll.get(s).stream()).toList();
                writer.textFile(SummaryLocations.FORMULA_SUMMARY_ALL, w -> writeCSV(w, globalTypes, all, true));
            }
        } finally {
            lock.unlock();
        }
    }

    private String makeHeader(String scorings) {
        final StringBuilder headerBuilder = new StringBuilder("molecularFormula\tadduct\tprecursorFormula");/*	rankingScore*/
        if (scorings != null && !scorings.isEmpty())
            headerBuilder.append("\t").append(scorings);
        headerBuilder.append("\tnumExplainedPeaks\texplainedIntensity\tmedianMassErrorFragmentPeaks(ppm)\tmedianAbsoluteMassErrorFragmentPeaks(ppm)\tmassErrorPrecursor(ppm)\tlipidClass");
        return headerBuilder.toString();
    }

    private void writeCSV(Writer w, LinkedHashMap<Class<? extends FormulaScore>, String> types, List<ResultEntry> results, boolean suffix) throws IOException {
        final List<Class<? extends FormulaScore>> scoreOrder = SiriusProjectSpaceManager.scorePriorities()
                .stream().filter(types::containsKey).toList();

        String header = makeHeader(scoreOrder.stream().map(types::get).collect(Collectors.joining("\t")));
        if (suffix)
            header = header + "\tionMass" + "\tretentionTimeInSeconds" + "\tid" + "\tfeatureId";

        w.write("formulaRank\t" + header + "\n");

        for (ResultEntry r : results) {
            FormulaScoring scores = r.scoring;

            w.write(String.valueOf(r.formulaRank));
            w.write('\t');
            w.write(r.molecularFormula.toString());
            w.write('\t');
            w.write(r.ion);
            w.write('\t');

            w.write(r.preFormula.toString());
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
            w.write('\t');
            w.write(r.lipidClass);
            if (suffix) {
                w.write('\t');
                w.write(r.ionMass);
                w.write('\t');
                w.write(r.retentionTimeSeconds);
                w.write('\t');
                w.write(r.dirName);
                w.write('\t');
                w.write(r.featureId);
            }

            w.write('\n');
        }
    }

    private static class ResultEntry {
        private final int formulaRank;
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

        public String lipidClass = "";

        public String featureId;

        public ResultEntry(FormulaResult r, CompoundContainer exp, int formulaRank) {
            this.formulaRank = formulaRank;
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
                lipidClass = tree.getAnnotation(LipidSpecies.class).map(LipidSpecies::toString).orElse("");
            });

            ionMass = BigDecimal.valueOf(exp.getId().getIonMass().orElse(Double.NaN)).setScale(5, RoundingMode.HALF_UP).toString();
            retentionTimeSeconds = String.valueOf(exp.getId().getRt().orElse(RetentionTime.NA()).getRetentionTimeInSeconds());
            dirName = exp.getId().getDirectoryName();
            featureId = exp.getId().getFeatureId().orElse("N/A");
        }

        public FormulaScoring getScoring() {
            return scoring;
        }

        public static ResultEntry of(@NotNull FormulaResult r, @NotNull CompoundContainer exp, int formulaRank) {
            return new ResultEntry(r, exp, formulaRank);
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

