package de.unijena.bioinf.ms.frontend.io.projectspace.summaries;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.TreeStatistics;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.projectspace.FormulaScoring;
import de.unijena.bioinf.projectspace.ProjectWriter;
import de.unijena.bioinf.projectspace.Summarizer;
import de.unijena.bioinf.projectspace.sirius.CompoundContainer;
import de.unijena.bioinf.projectspace.sirius.FormulaResult;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

public class FormulaSummaryWriter implements Summarizer {

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
        if (results == null || results.isEmpty())
            return;

        writer.inDirectory(exp.getId().getDirectoryName(), () -> {
//            writer.delete(SummaryLocations.FORMULA_SUMMARY);
            writer.textFile(SummaryLocations.FORMULA_SUMMARY, w -> {
                final StringBuilder headerBuilder = new StringBuilder("formula\tadduct\tprecursorFormula\trank\trankingScore");
                LinkedHashSet<Class<? extends FormulaScore>> types = new LinkedHashSet<>();
                results.stream().forEach(r -> {
                    r.getCandidate().getAnnotation(FormulaScoring.class)
                            .ifPresent(s -> s.annotations().forEach((key, value) -> {
                                if (value instanceof FormulaScore) {
                                    if (types.add(value.getClass()))
                                        headerBuilder.append("\t").append(value.name());
                                }
                            }));
                });


                headerBuilder.append("\texplainedPeaks\texplainedIntensity\n");

                //writing stuff
                w.write(headerBuilder.toString());
                int rank = 0;
                for (SScored<FormulaResult, ? extends FormulaScore> s : results) {
                    FormulaResult r = s.getCandidate();
                    PrecursorIonType ion = r.getId().getIonType();
                    FormulaScoring scores = r.getAnnotationOrThrow(FormulaScoring.class);
                    FTree tree = r.getAnnotationOrNull(FTree.class);

                    w.write(r.getId().getMolecularFormula().toString());
                    w.write('\t');
                    w.write(ion != null ? ion.toString() : "?");
                    w.write('\t');

                    w.write(r.getId().getPrecursorFormula().toString());
                    w.write('\t');

                    w.write(String.valueOf(++rank));
                    w.write('\t');
                    w.write(String.valueOf(s.getScore()));
                    w.write('\t');
                    //writing different Scores to file e.g. sirius and zodiac
                    for (Class<? extends FormulaScore> k : types) {
                        w.write(String.valueOf(scores.getAnnotation(k).map(FormulaScore::score).orElse(Double.NaN)));
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

