package de.unijena.bioinf.ms.frontend.io.projectspace.summaries;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.fingerid.ConfidenceScore;
import de.unijena.bioinf.fingerid.blast.FingerblastResult;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.projectspace.FormulaScoring;
import de.unijena.bioinf.projectspace.ProjectWriter;
import de.unijena.bioinf.projectspace.Summarizer;
import de.unijena.bioinf.projectspace.sirius.CompoundContainer;
import de.unijena.bioinf.projectspace.sirius.FormulaResult;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

public class StructureSummaryWriter implements Summarizer {
    private List<SScored<String, ? extends FormulaScore>> topHits = new ArrayList<>();

    @Override
    public List<Class<? extends DataAnnotation>> requiredFormulaResultAnnotations() {
        return Arrays.asList(
                FormulaScoring.class,
                FingerblastResult.class
        );
    }

    @Override
    public void addWriteCompoundSummary(ProjectWriter writer, @NotNull CompoundContainer exp, List<? extends SScored<FormulaResult, ? extends FormulaScore>> formulaResults) throws IOException {
        try {
            if (!writer.exists(exp.getId().getDirectoryName()))
                return;
            if (formulaResults == null || formulaResults.isEmpty())
                return;

            writer.inDirectory(exp.getId().getDirectoryName(), () -> {
                writer.textFile(SummaryLocations.STRUCTURE_SUMMARY, fileWriter -> {
                    fileWriter.write("rank\t");
                    fileWriter.write(StructureCSVExporter.HEADER);

                    final List<SScored<String, ConfidenceScore>> topHits = new ArrayList<>();

                    for (SScored<FormulaResult, ? extends FormulaScore> result : formulaResults) {
                        if (result.getCandidate().hasAnnotation(FingerblastResult.class)) {
                            final List<Scored<FingerprintCandidate>> frs = result.getCandidate().getAnnotationOrThrow(FingerblastResult.class).getResults();

                            //create buffer
                            final StringWriter w = new StringWriter(128);
                            for (Scored<FingerprintCandidate> res : frs) {
                                new StructureCSVExporter().exportFingerIdResult(w, res, false, null);
                            }
                            final String[] lines = w.toString().split("\n");

                            if (lines.length > 0) {
                                fileWriter.write("\n");
                                // write summary file
                                for (int i = 0; i < lines.length; i++) {
                                    if (!lines[i].isEmpty()) {
                                        fileWriter.write(String.valueOf(i + 1));
                                        fileWriter.write("\t");
                                        fileWriter.write(lines[i]);
                                        if (i < lines.length - 1)
                                            fileWriter.write("\n");
                                    }
                                }


                                // collect data for project wide summary
                                final ConfidenceScore confidence = result.getCandidate().getAnnotation(FormulaScoring.class).
                                        map(s -> s.getAnnotationOr(ConfidenceScore.class, FormulaScore::NA)).orElse(FormulaScore.NA(ConfidenceScore.class));

                                if (!lines[0].isEmpty())
                                    topHits.add(new SScored<>(confidence + "\t" + lines[0] + "\t" + exp.getId().getDirectoryName() + "\n", confidence));
                            }
                        }
                    }
                    if (!topHits.isEmpty()){
                        topHits.sort(Comparator.reverseOrder());
                        this.topHits.add(topHits.get(0));
                    }
                });
                return true;
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void writeProjectSpaceSummary(ProjectWriter writer) throws IOException {
        if (topHits.size() > 0) {
            writer.textFile(SummaryLocations.STRUCTURE_SUMMARY_GLOBAL, w -> {
                topHits.sort(Collections.reverseOrder());
                w.write("rank\t" + new ConfidenceScore(0).name() + "\t" + StructureCSVExporter.HEADER + "\tid" + "\n");
                int rank = 0;
                for (SScored<String, ? extends FormulaScore> s : topHits) {
                    w.write(String.valueOf(++rank));
                    w.write("\t");
                    w.write(s.getCandidate());
                }
            });
        }
    }
}
