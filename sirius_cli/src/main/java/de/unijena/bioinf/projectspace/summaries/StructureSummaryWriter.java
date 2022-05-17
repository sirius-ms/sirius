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
import gnu.trove.map.hash.TIntIntHashMap;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class StructureSummaryWriter implements Summarizer {
    private final Lock lock = new ReentrantLock();
    private final List<Hit> compoundTopHits = new ArrayList<>();
    private final Map<Hit, List<Hit>> compoundTopHitsAdducts = new HashMap<>();

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

            final List<SScored<FormulaResult, ? extends FormulaScore>> results =
                    FormulaScoring.reRankBy(formulaResults, List.of(SiriusScore.class), true); //sorted by SiriusScore to detect adducts

            if (results.stream().anyMatch(c -> c.getCandidate().hasAnnotation(FBCandidates.class))) {
                writer.inDirectory(exp.getId().getDirectoryName(), () -> {
                    writer.textFile(SummaryLocations.STRUCTURE_CANDIDATES, fileWriter -> {
                        fileWriter.write("rank\tformulaRank\t" + new ConfidenceScore(0).name() + "\t");
                        fileWriter.write(StructureCSVExporter.HEADER);

                        int formulaRank = 0;
                        TIntIntHashMap adductCounts = new TIntIntHashMap();
                        MolecularFormula preFormula = null;
                        for (SScored<FormulaResult, ? extends FormulaScore> result : results) {
                            if (preFormula == null || !result.getCandidate().getId().getPrecursorFormula().equals(preFormula))
                                adductCounts.put(++formulaRank, 1);
                            else
                                adductCounts.increment(formulaRank);

                            preFormula = result.getCandidate().getId().getPrecursorFormula();

                            if (result.getCandidate().hasAnnotation(FBCandidates.class)) {
                                final List<Scored<CompoundCandidate>> frs = result.getCandidate().getAnnotationOrThrow(FBCandidates.class).getResults();

                                //create buffer
                                final StringWriter w = new StringWriter(128);
                                for (Scored<CompoundCandidate> res : frs)
                                    new StructureCSVExporter().exportFingerIdResult(w, res, result.getCandidate().getId(), false, null);

                                final List<List<String>> lines = w.toString().isBlank() ? null :
                                        Arrays.stream(w.toString().split("\n")).map(l -> Arrays.asList(l.split("\t"))).collect(Collectors.toList());

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
                                        final SiriusScore siriusScore = result.getCandidate().getAnnotation(FormulaScoring.class).
                                                map(s -> s.getAnnotationOr(SiriusScore.class, FormulaScore::NA)).orElse(FormulaScore.NA(SiriusScore.class));
                                        final ZodiacScore zodiacScore = result.getCandidate().getAnnotation(FormulaScoring.class).
                                                map(s -> s.getAnnotationOr(ZodiacScore.class, FormulaScore::NA)).orElse(FormulaScore.NA(ZodiacScore.class));

                                        topHits.add(new Hit(confidence + "\t" + lines.get(0).get(0) + "\t" + zodiacScore + "\t" + siriusScore + "\t" + String.join("\t", lines.get(0).subList(1, lines.get(0).size())) + "\t" + exp.getId().getIonMass().orElse(Double.NaN) + "\t" + exp.getId().getRt().orElse(RetentionTime.NA()).getRetentionTimeInSeconds() + "\t" + exp.getId().getDirectoryName() + "\n", confidence, csiScore, formulaRank));
                                    }
                                }
                            }
                        }
                        topHits.forEach(hit -> hit.numberOfAdducts = adductCounts.get(hit.formulaRank));
                        topHits.forEach(hit -> hit.numberOfFps = topHits.size());
                        topHits.sort(Hit.compareByFingerIdScore().reversed());
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
                    compoundTopHits.add(topHits.get(0));
                    compoundTopHitsAdducts.put(topHits.get(0), toadd);
                } finally {
                    lock.unlock();
                }
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void writeProjectSpaceSummary(ProjectWriter writer) throws IOException {
        lock.lock();
        try {
            if (!compoundTopHits.isEmpty()) {
                compoundTopHits.sort(Hit.compareByConfidence().reversed());
                writer.textFile(SummaryLocations.COMPOUND_SUMMARY, w -> write(w, compoundTopHits));

                if (!compoundTopHitsAdducts.isEmpty()) {
                    final List<Hit> topHitList = new ArrayList<>();
                    compoundTopHits.forEach(leadHit -> {
                        List<Hit> hits = compoundTopHitsAdducts.get(leadHit);
                        hits.sort(Hit.compareByConfidence().reversed());
                        topHitList.addAll(hits);
                    });
                    writer.textFile(SummaryLocations.COMPOUND_SUMMARY_ADDUCTS, w -> write(w, topHitList));
                }
            }


        } finally {
            lock.unlock();
        }
    }

    static void write(BufferedWriter w, List<Hit> data) throws IOException {
        w.write("rank\t" + "formulaRank\t" + "#adducts\t" + "#predictedFPs\t" + new ConfidenceScore(0).name() + "\t" + StructureCSVExporter.HEADER_LIST.get(0) + "\t" + new ZodiacScore(0).name() + "\t" + new SiriusScore(0).name() + "\t" + String.join("\t", StructureCSVExporter.HEADER_LIST.subList(1, StructureCSVExporter.HEADER_LIST.size())) + "\t" + "ionMass\t" + "retentionTimeInSeconds\t" + "id" + "\n");
        int rank = 0;
        for (Hit s : data) {
            w.write(String.valueOf(++rank));
            w.write("\t");
            w.write(String.valueOf(s.formulaRank));
            w.write("\t");
            w.write(String.valueOf(s.numberOfAdducts));
            w.write("\t");
            w.write(String.valueOf(s.numberOfFps));
            w.write("\t");
            w.write(s.line);
        }
    }

    static class Hit {
        final String line;
        final ConfidenceScore confidenceScore;
        final TopCSIScore csiScore;
        final int formulaRank;
        int numberOfAdducts = 1;
        int numberOfFps = 1;

        Hit(String line, ConfidenceScore confidenceScore, TopCSIScore csiScore, int formulaRank) {
            this.line = line;
            this.confidenceScore = confidenceScore;
            this.csiScore = csiScore;
            this.formulaRank = formulaRank;
        }

        static Comparator<Hit> compareByConfidence() {
            return Comparator.comparing(o -> o.confidenceScore);
        }

        static Comparator<Hit> compareByFingerIdScore() {
            return Comparator.comparing(o -> o.csiScore);
        }
    }
}