package de.unijena.bioinf.projectspace.summaries;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.GibbsSampling.ZodiacScore;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.fingerid.ConfidenceScore;
import de.unijena.bioinf.fingerid.blast.FBCandidates;
import de.unijena.bioinf.fingerid.blast.TopCSIScore;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.projectspace.FormulaScoring;
import de.unijena.bioinf.projectspace.ProjectWriter;
import de.unijena.bioinf.projectspace.Summarizer;
import de.unijena.bioinf.projectspace.sirius.CompoundContainer;
import de.unijena.bioinf.projectspace.sirius.FormulaResult;
import de.unijena.bioinf.sirius.scores.SiriusScore;
import gnu.trove.map.hash.TIntIntHashMap;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class StructureSummaryWriter implements Summarizer {
    private List<Hit> compoundTopHits = new ArrayList<>();
    private List<Hit> compoundTopHitsAdducts = new ArrayList<>();

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
            if (!writer.exists(exp.getId().getDirectoryName()))
                return;
            if (formulaResults == null || formulaResults.isEmpty())
                return;

            final List<Hit> topHits = new ArrayList<>();


            final List<SScored<FormulaResult, ? extends FormulaScore>> results =
                    FormulaScoring.reRankBy(formulaResults, List.of(SiriusScore.class), true); //sorted by SiriusScore to detect adducts


            writer.inDirectory(exp.getId().getDirectoryName(), () -> {
                writer.textFile(SummaryLocations.STRUCTURE_CANDIDATES, fileWriter -> {
                    fileWriter.write("rank\tformulaRank\t");
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

                                    topHits.add(new Hit(/*formulaRank + "\t" + */confidence + "\t" + lines.get(0).get(0) + "\t" + zodiacScore + "\t" + siriusScore + "\t" + String.join("\t", lines.get(0).subList(1, lines.get(0).size())) + "\t" + exp.getId().getDirectoryName() + "\n", confidence, csiScore, formulaRank));
                                }
                            }
                        }
                    }
                    topHits.forEach(hit -> hit.numberOfAdducts = adductCounts.get(hit.formulaRank));
                    topHits.forEach(hit -> hit.numberOfFps = topHits.size());
                    topHits.sort(Hit.compareByFingerIdScore().reversed());
                });


//                writer.textFile(SummaryLocations.STRUCTURE_CANDIDATES_TOP, fileWriter -> write(fileWriter, topHits));
                return true;
            });


            if (!topHits.isEmpty()) {
                compoundTopHits.add(topHits.get(0));
                List<Hit> toadd = topHits.stream().filter(hit -> hit.formulaRank == 1).collect(Collectors.toList());
                toadd.forEach(h -> h.numberOfAdducts = toadd.size());
                compoundTopHitsAdducts.addAll(toadd);
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void writeProjectSpaceSummary(ProjectWriter writer) throws IOException {
        if (!compoundTopHits.isEmpty()) {
            compoundTopHits.sort(Hit.compareByConfidence().reversed());
            writer.textFile(SummaryLocations.COMPOUND_SUMMARY, w -> write(w, compoundTopHits));
        }

        if (!compoundTopHitsAdducts.isEmpty()) {
            compoundTopHitsAdducts.sort(Hit.compareByConfidence().reversed());
            writer.textFile(SummaryLocations.COMPOUND_SUMMARY_ADDUCTS, w -> write(w, compoundTopHitsAdducts));
        }
    }

    static void write(BufferedWriter w, List<Hit> data) throws IOException {
        w.write("rank\t" + "#adducts\t" + "#predictedFPs\t" + new ConfidenceScore(0).name() + "\t" + StructureCSVExporter.HEADER_LIST.get(0) + "\t" + new ZodiacScore(0).name() + "\t" + new SiriusScore(0).name() + "\t" + String.join("\t", StructureCSVExporter.HEADER_LIST.subList(1, StructureCSVExporter.HEADER_LIST.size())) + "\tid" + "\n");
        int rank = 0;
        for (Hit s : data) {
            w.write(String.valueOf(++rank));
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
