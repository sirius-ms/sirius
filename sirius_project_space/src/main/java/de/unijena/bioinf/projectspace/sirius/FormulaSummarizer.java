package de.unijena.bioinf.projectspace.sirius;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.TreeStatistics;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.projectspace.FormulaScoring;
import de.unijena.bioinf.projectspace.ProjectWriter;
import de.unijena.bioinf.projectspace.Summarizer;
import de.unijena.bioinf.sirius.scores.IsotopeScore;
import de.unijena.bioinf.sirius.scores.TreeScore;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FormulaSummarizer implements Summarizer {

    @Override
    public List<Class<? extends DataAnnotation>> requiredFormulaResultAnnotations() {
        return Arrays.asList(
                FormulaScoring.class,
                FTree.class
        );
    }

    @Override
    public void addWriteCompoundSummary(ProjectWriter writer, @NotNull CompoundContainer exp, List<? extends SScored<FormulaResult, ? extends FormulaScore>> results) throws IOException {
        if (!writer.exists(exp.getId().getDirectoryName()))
            return;

        writer.inDirectory(exp.getId().getDirectoryName(), () -> {
            writer.textFile(SiriusLocations.SIRIUS_SUMMARY, w -> {
                final StringBuilder headerBuilder = new StringBuilder("formula\tadduct\trank\trankingScore");

                List<Class<? extends FormulaScore>> types = new ArrayList<>();
                results.get(0).getCandidate().annotations().forEach((key, value) -> {
                    if (value instanceof FormulaScore) {
                        types.add(((FormulaScore) value).getClass());
                        headerBuilder.append("\t").append(value.getIdentifier());
                    }
                });

                headerBuilder.append("\texplainedPeaks\texplainedIntensity\n");

                //writing stuff
                w.write(headerBuilder.toString());
                int rank = 0;
                for (SScored<FormulaResult, ? extends FormulaScore> s : results) {
                    rank++;
                    FormulaResult r = s.getCandidate();
                    PrecursorIonType ion = r.getId().getIonType();
                    FormulaScoring scores = r.getAnnotationOrThrow(FormulaScoring.class);
                    FTree tree = r.getAnnotationOrNull(FTree.class);

                    w.write(r.getId().getFormula().toString());
                    w.write('\t');
                    w.write(ion != null ? ion.toString() : "?");
                    w.write('\t');
                    w.write(rank);
                    w.write('\t');
                    w.write(String.valueOf(s.getScore()));
                    w.write('\t');
                    w.write(String.valueOf(scores.getAnnotationOrNull(TreeScore.class)));
                    w.write('\t');
                    w.write(String.valueOf(scores.getAnnotationOrNull(IsotopeScore.class)));
                    w.write('\t');
                    //writing different Scores to file e.g. sirius and zodiac
                    for (Class<? extends FormulaScore> k : types) {
                        w.write(String.valueOf(scores.getAnnotationOrThrow(k).score()));
                        w.write('\t');
                    }

                    w.write(tree != null ? String.valueOf(tree.numberOfVertices()) : "");
                    w.write('\t');
                    w.write(tree != null ? String.valueOf(tree.getAnnotationOrThrow(TreeStatistics.class).getExplainedIntensity()) : "");
                    w.write('\n');
                }
            });
            return true;
        });
    }

    @Override
    public void writeProjectSpaceSummary(ProjectWriter writer) throws IOException {

    }
}

