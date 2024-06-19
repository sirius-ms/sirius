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
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.GibbsSampling.ZodiacScore;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.fingerid.ConfidenceScore;
import de.unijena.bioinf.fingerid.blast.FBCandidates;
import de.unijena.bioinf.fingerid.blast.TopCSIScore;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.projectspace.*;
import de.unijena.bioinf.sirius.scores.SiriusScore;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class StructureSummaryWriter extends CandidateSummarizer {
    private final Lock lock = new ReentrantLock();
    private final List<Hit> compoundTopHits;
    private final Map<Hit, List<Hit>> compoundTopHitsAdducts;
    private final Map<Hit, List<Hit>> compoundAllHits;


    public StructureSummaryWriter(boolean writeTopHitGlobal, boolean writeTopHitWithAdductsGlobal, boolean writeFullGlobal) {
        super(writeTopHitGlobal, writeTopHitWithAdductsGlobal, writeFullGlobal);
        compoundTopHits = writeTopHitGlobal ? new ArrayList<>() : null;
        compoundTopHitsAdducts = writeTopHitWithAdductsGlobal ? new HashMap<>() : null;
        compoundAllHits = writeFullGlobal ? new HashMap<>() : null;
    }

    @Override
    public List<Class<? extends DataAnnotation>> requiredFormulaResultAnnotations() {
        return Arrays.asList(
                FormulaScoring.class,
                FBCandidates.class
        );
    }

    @Override
    public void addWriteCompoundSummary(ProjectWriter writer, @NotNull CompoundContainer exp, List<? extends SScored<FormulaResult, ? extends FormulaScore>> formulaResults) throws IOException {
        try {
            if (formulaResults == null || formulaResults.isEmpty())
                return;

            final List<Hit> topHits = new ArrayList<>();
            final List<Hit> allHits = new ArrayList<>();

            final List<SScored<FormulaResult, ? extends FormulaScore>> results =
                    FormulaScoring.reRankBy(formulaResults, List.of(SiriusScore.class), true); //sorted by SiriusScore to detect adducts

            if (results.stream().anyMatch(c -> c.getCandidate().hasAnnotation(FBCandidates.class))) {
                writer.inDirectory(exp.getId().getDirectoryName(), () -> {
                    writer.textFile(SummaryLocations.STRUCTURE_CANDIDATES, fileWriter -> {
                        fileWriter.write("structureRankPerFormula\tformulaRank\t" + new ConfidenceScore(0).name() + "\t");
                        fileWriter.write(StructureCSVExporter.HEADER);

                        int formulaRank = 0;
                        Int2IntOpenHashMap adductCounts = new Int2IntOpenHashMap();
                        final AtomicInteger fpCounts = new AtomicInteger(0);
                        MolecularFormula preFormula = null;
                        for (SScored<FormulaResult, ? extends FormulaScore> result : results) {
                            if (preFormula == null || !result.getCandidate().getId().getPrecursorFormula().equals(preFormula))
                                adductCounts.put(++formulaRank, 1);
                            else
                                adductCounts.addTo(formulaRank, 1);

                            preFormula = result.getCandidate().getId().getPrecursorFormula();

                            if (result.getCandidate().hasAnnotation(FBCandidates.class)) {
                                fpCounts.incrementAndGet();
                                final List<Scored<CompoundCandidate>> frs = result.getCandidate().getAnnotationOrThrow(FBCandidates.class).getResults();

                                //create buffer
                                final StringWriter w = new StringWriter(128);
                                for (Scored<CompoundCandidate> res : frs)
                                    new StructureCSVExporter().exportFingerIdResult(w, res, result.getCandidate().getId(), false, null);

                                final List<List<String>> lines = w.toString().isBlank() ? null :
                                        Arrays.stream(w.toString().split("\n"))
                                                .map(l -> Arrays.asList(l.split("\t"))).toList();

                                if (lines != null && !lines.isEmpty()) {
                                    fileWriter.write("\n");
                                    // write summary file
                                    int rank = 0;
                                    for (List<String> line : lines) {
                                        rank++;
                                        if (!line.isEmpty()) {
                                            fileWriter.write(String.valueOf(rank));
                                            fileWriter.write("\t");
                                            fileWriter.write(String.valueOf(formulaRank));
                                            fileWriter.write("\t");
                                            fileWriter.write(
                                                    rank != 1 ? FormulaScore.NA()
                                                            : result.getCandidate().getAnnotation(FormulaScoring.class).
                                                            map(s -> s.getAnnotationOr(ConfidenceScore.class, FormulaScore::NA)).orElse(FormulaScore.NA(ConfidenceScore.class)).toString()
                                            );
                                            fileWriter.write("\t");
                                            fileWriter.write(String.join("\t", line));
                                            if (rank < lines.size())
                                                fileWriter.write("\n");
                                        }
                                    }

                                    // collect data for project wide summary
                                    if (!lines.get(0).isEmpty()) {
                                        final ConfidenceScore confidence = result.getCandidate().getAnnotation(FormulaScoring.class).
                                                map(s -> s.getAnnotationOr(ConfidenceScore.class, FormulaScore::NA)).orElse(FormulaScore.NA(ConfidenceScore.class));
                                        final TopCSIScore csiScore = result.getCandidate().getAnnotation(FormulaScoring.class).
                                                map(s -> s.getAnnotationOr(TopCSIScore.class, FormulaScore::NA)).orElse(FormulaScore.NA(TopCSIScore.class));
                                        final Hit topHit = toHit(exp.getId(), result, lines.get(0), confidence, csiScore.score(), formulaRank);
                                        topHits.add(topHit);

                                        if (compoundAllHits != null) {
                                            int finalFormulaRank = formulaRank;
                                            Iterator<List<String>> linesIt = lines.iterator();
                                            linesIt.next();
                                            allHits.add(topHit);
                                            while (linesIt.hasNext()) {
                                                List<String> l = linesIt.next();
                                                allHits.add(toHit(exp.getId(), result, l, FormulaScore.NA(ConfidenceScore.class), Double.parseDouble(l.get(0)), finalFormulaRank));
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        topHits.forEach(hit -> hit.numberOfAdducts = adductCounts.get(hit.formulaRank));
                        topHits.forEach(hit -> hit.numberOfFps = fpCounts.get());
                        topHits.sort(Hit.compareByFingerIdScore().reversed());

                        allHits.forEach(hit -> hit.numberOfAdducts = adductCounts.get(hit.formulaRank));
                        allHits.forEach(hit -> hit.numberOfFps = fpCounts.get());
                        allHits.sort(Hit.compareByFingerIdScore().reversed());
                    });

                    return true;
                });
            }


            if (!topHits.isEmpty()) {
                final int topRank = topHits.stream().mapToInt(h -> h.formulaRank).min().getAsInt();
                List<Hit> toadd = topHits.stream().filter(hit -> hit.formulaRank == topRank).collect(Collectors.toList());
                toadd.forEach(h -> h.numberOfAdducts = toadd.size());
                lock.lock();
                try {
                    if (compoundTopHits != null)
                        compoundTopHits.add(topHits.get(0));
                    if (compoundTopHitsAdducts != null)
                        compoundTopHitsAdducts.put(topHits.get(0), toadd);
                    if (compoundAllHits != null)
                        compoundAllHits.put(allHits.get(0), allHits);
                } finally {
                    lock.unlock();
                }
            }


        } catch (
                IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void writeProjectSpaceSummary(ProjectWriter writer) throws IOException {
        lock.lock();
        try {
            if (compoundTopHits != null && !compoundTopHits.isEmpty()) {
                compoundTopHits.sort(Hit.compareByConfidence().reversed());
                writer.textFile(SummaryLocations.COMPOUND_SUMMARY, w -> write(w, compoundTopHits));
            }


            if (compoundTopHitsAdducts != null && !compoundTopHitsAdducts.isEmpty()) {
                final List<Hit> topHitList = new ArrayList<>();
                compoundTopHitsAdducts.keySet().stream().sorted(Hit.compareByConfidence().reversed()).forEach(leadHit -> {
                    List<Hit> hits = compoundTopHitsAdducts.get(leadHit);
                    hits.sort(Hit.compareByConfidence().reversed());
                    topHitList.addAll(hits);
                });

                writer.textFile(SummaryLocations.COMPOUND_SUMMARY_ADDUCTS, w -> write(w, topHitList));
            }

            if (compoundAllHits != null && !compoundAllHits.isEmpty()) {
                final List<Hit> topHitList = new ArrayList<>();
                compoundAllHits.keySet().stream().sorted(Hit.compareByConfidence().reversed()).forEach(leadHit -> {
                    List<Hit> hits = compoundAllHits.get(leadHit);
                    hits.sort(Hit.compareByConfidence().reversed());
                    topHitList.addAll(hits);
                });

                writer.textFile(SummaryLocations.COMPOUND_SUMMARY_ALL, w -> write(w, topHitList));
            }


        } finally {
            lock.unlock();
        }
    }

    static Hit toHit(CompoundContainerId id, SScored<FormulaResult, ? extends FormulaScore> result, List<String> line, ConfidenceScore confidence, double csiScore, int formulaRank) {
        final SiriusScore siriusScore = result.getCandidate().getAnnotation(FormulaScoring.class).
                map(s -> s.getAnnotationOr(SiriusScore.class, FormulaScore::NA)).orElse(FormulaScore.NA(SiriusScore.class));
        final ZodiacScore zodiacScore = result.getCandidate().getAnnotation(FormulaScoring.class).
                map(s -> s.getAnnotationOr(ZodiacScore.class, FormulaScore::NA)).orElse(FormulaScore.NA(ZodiacScore.class));

        return new Hit(confidence + "\t" + line.get(0) + "\t" + zodiacScore + "\t" + siriusScore + "\t" + String.join("\t", line.subList(1, line.size())) + "\t" + id.getIonMass().orElse(Double.NaN) + "\t" + id.getRt().orElse(RetentionTime.NA()).getRetentionTimeInSeconds() + "\t" + id.getDirectoryName(), confidence, csiScore, formulaRank, id.getDirectoryName(), id.getFeatureId().orElse("N/A"));
    }

    static void write(BufferedWriter w, List<Hit> data) throws IOException {
        w.write("confidenceRank\t" + "structurePerIdRank\t" + "formulaRank\t" + "#adducts\t" + "#predictedFPs\t" + new ConfidenceScore(0).name() + "\t" + StructureCSVExporter.HEADER_LIST.get(0) + "\t" + new ZodiacScore(0).name() + "\t" + new SiriusScore(0).name() + "\t" + String.join("\t", StructureCSVExporter.HEADER_LIST.subList(1, StructureCSVExporter.HEADER_LIST.size())) + "\t" + "ionMass\t" + "retentionTimeInSeconds\t" + "id\t" + "featureId" + "\n");
        int structureRank = 0;
        int confidenceRank = 0;
        String dirname = null;

        for (Hit s : data) {
            final String confidenceRankStr;
            if (!s.dirname.equals(dirname)){
                dirname = s.dirname;
                structureRank = 0;
                confidenceRank++;
                confidenceRankStr = String.valueOf(confidenceRank);
            }else {
                confidenceRankStr = "N/A";
            }

            w.write(confidenceRankStr);
            w.write("\t");
            w.write(String.valueOf(++structureRank));
            w.write("\t");
            w.write(String.valueOf(s.formulaRank));
            w.write("\t");
            w.write(String.valueOf(s.numberOfAdducts));
            w.write("\t");
            w.write(String.valueOf(s.numberOfFps));
            w.write("\t");
            w.write(s.line);
            w.write("\t");
            w.write(s.featureId);
            w.write("\n");
        }
    }

    static class Hit {
        final String line;
        final ConfidenceScore confidenceScore;
        final double csiScore;
        final int formulaRank;
        int numberOfAdducts = 1;
        int numberOfFps = 1;
        @NotNull
        final String dirname;
        final String featureId;

        Hit(String line, ConfidenceScore confidenceScore, double csiScore, int formulaRank, @NotNull String dirname, String featureId) {
            this.line = line;
            this.confidenceScore = confidenceScore;
            this.csiScore = csiScore;
            this.formulaRank = formulaRank;
            this.dirname = dirname;
            this.featureId = featureId;
        }

        static Comparator<Hit> compareByConfidence() {
            return Comparator.comparing(o -> o.confidenceScore);
        }

        static Comparator<Hit> compareByFingerIdScore() {
            return Comparator.comparing(o -> o.csiScore);
        }
    }
}