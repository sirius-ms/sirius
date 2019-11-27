package de.unijena.bioinf.ms.frontend.io.projectspace.summaries;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Score;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.TreeStatistics;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.projectspace.FormulaScoring;
import de.unijena.bioinf.projectspace.ProjectWriter;
import de.unijena.bioinf.projectspace.Summarizer;
import de.unijena.bioinf.projectspace.sirius.CompoundContainer;
import de.unijena.bioinf.projectspace.sirius.FormulaResult;
import de.unijena.bioinf.sirius.scores.SiriusScore;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class FormulaSummaryWriter implements Summarizer {
    final LinkedHashMap<Class<? extends FormulaScore>, String> types = new LinkedHashMap<>();
    final Map<FormulaResult, Class<? extends FormulaScore>> results = new HashMap<>();

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
            writer.textFile(SummaryLocations.FORMULA_SUMMARY, w -> {
                final LinkedHashMap<Class<? extends FormulaScore>, String> types = new LinkedHashMap<>();
                final LinkedHashMap<FormulaResult, FormulaScoring> scorings = new LinkedHashMap<>();

                final AtomicBoolean first = new AtomicBoolean(true);
                results.forEach(r -> {
                    r.getCandidate().getAnnotation(FormulaScoring.class)
                            .ifPresent(s -> {
                                if (first.getAndSet(false))
                                    this.results.put(r.getCandidate(), r.getScoreObject().getClass());


                                scorings.put(r.getCandidate(), s);
                                s.annotations().forEach((key, value) -> {
                                    if (value != null) {
                                        types.putIfAbsent(value.getClass(), value.name());
                                        this.types.putIfAbsent(value.getClass(), value.name());
                                    }
                                });
                            });
                });

                //writing stuff
                writeCSV(w, types, results);
            });

            return true;
        });
    }

    @Override
    public void writeProjectSpaceSummary(ProjectWriter writer) throws IOException {
        final Map<Class<? extends FormulaScore>, AtomicInteger> counts = new HashMap<>();
        results.forEach((k, v) -> Objects.requireNonNull(counts.computeIfAbsent(v, (v2) -> new AtomicInteger(0))).incrementAndGet());
        Class<? extends FormulaScore> rankingScore = counts.keySet().stream().max(Comparator.comparingInt(k -> counts.get(k).get())).orElse(SiriusScore.class);

        List<Scored<FormulaResult>> r = results.keySet().stream().map(res -> new Scored<>(res, res.getAnnotationOrThrow(FormulaScoring.class).getAnnotation(rankingScore).map(FormulaScore::score).orElse(Double.NaN))).sorted().collect(Collectors.toList());

        writer.textFile(SummaryLocations.FORMULA_SUMMARY_GLOBAL, w -> {
            writeCSV(w, types, r);
        });
    }

    private String makeHeader(LinkedHashMap<Class<? extends FormulaScore>, String> scorings) {
        final StringBuilder headerBuilder = new StringBuilder("formula\tadduct\tprecursorFormula\trank\trankingScore");
        scorings.forEach((key, value) -> {
            if (value != null)
                headerBuilder.append("\t").append(value);
        });
        headerBuilder.append("\texplainedPeaks\texplainedIntensity\n");
        return headerBuilder.toString();
    }

    private void writeCSV(Writer w, LinkedHashMap<Class<? extends FormulaScore>, String> types, List<? extends SScored<FormulaResult, ? extends Score>> results) throws IOException {
        w.write(makeHeader(types));
        int rank = 0;
        for (SScored<FormulaResult, ? extends Score> s : results) {
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
            for (Class<? extends FormulaScore> k : types.keySet()) {
                w.write(String.valueOf(scores.getAnnotation(k).map(FormulaScore::score).orElse(Double.NaN)));
                w.write('\t');
            }
            w.write(tree != null ? String.valueOf(tree.numberOfVertices()) : "");
            w.write('\t');
            w.write(tree != null ? String.valueOf(tree.getAnnotationOrThrow(TreeStatistics.class).getExplainedIntensity()) : "");
            w.write('\n');
        }
    }
}

