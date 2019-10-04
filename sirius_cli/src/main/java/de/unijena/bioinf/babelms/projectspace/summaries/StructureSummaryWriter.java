package de.unijena.bioinf.babelms.projectspace.summaries;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.fingerid.ConfidenceScore;
import de.unijena.bioinf.fingerid.blast.FingerblastResult;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.projectspace.FormulaScoring;
import de.unijena.bioinf.projectspace.ProjectWriter;
import de.unijena.bioinf.projectspace.StandardMSFilenameFormatter;
import de.unijena.bioinf.projectspace.Summarizer;
import de.unijena.bioinf.projectspace.sirius.CompoundContainer;
import de.unijena.bioinf.projectspace.sirius.FormulaResult;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class StructureSummaryWriter implements Summarizer {
    private String header;
    private List<Scored<String>> topHits = new ArrayList<>();

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
                    fileWriter.write(StructureCSVExporter.HEADER);
                    for (SScored<FormulaResult, ? extends FormulaScore> results : formulaResults) {
                        if (results.getCandidate().hasAnnotation(FingerblastResult.class)) {
                            final List<Scored<CompoundCandidate>> frs = results.getCandidate().getAnnotationOrThrow(FingerblastResult.class).getResults();
                            final StringWriter w = new StringWriter(128);
                            new StructureCSVExporter().exportFingerIdResults(w, frs, false);
                            final String hits = w.toString();

                            // write summary file
                            fileWriter.write(hits);

                            // collect data for project wide summary
                            final double confidence = results.getCandidate().getAnnotation(FormulaScoring.class).
                                    map(s -> s.getAnnotation(ConfidenceScore.class).orElse(new ConfidenceScore(Double.NaN))).
                                    map(ConfidenceScore::score).orElse(Double.NaN);

                            final @NotNull Ms2Experiment experimentResult = exp.getAnnotationOrThrow(Ms2Experiment.class);
                            final String[] lines = hits.split("\n", 3);

                            if (lines.length >= 2)
                                topHits.add(new Scored<>(StandardMSFilenameFormatter.simplifyURL(experimentResult.getSource().getFile()) + "\t" + StandardMSFilenameFormatter.simplify(experimentResult.getName()) + "\t" + confidence + "\t" + lines[1] + "\n", confidence));
                            if (header == null)
                                header = "source\texperimentName\tconfidence\t" + lines[0];
                        }
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
            writer.textFile(SummaryLocations.IDENTIFICATIONS_SUMMARY, w -> {
                topHits.sort(Collections.reverseOrder());
                w.write(header);
                for (Scored<String> s : topHits)
                    w.write(s.getCandidate());
            });
        }

    }
}
