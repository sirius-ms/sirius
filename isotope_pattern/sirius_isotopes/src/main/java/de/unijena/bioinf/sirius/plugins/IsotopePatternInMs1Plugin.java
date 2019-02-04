package de.unijena.bioinf.sirius.plugins;

import de.unijena.bioinf.ChemistryBase.algorithm.Called;
import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.IntergraphMapping;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.IsotopeSettings;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.Whiteset;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.SiriusPlugin;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.DecompositionScorer;
import de.unijena.bioinf.IsotopePatternAnalysis.ExtractedIsotopePattern;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePattern;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;

import java.util.Map;

/**
 * If an isotope pattern analysis assigns an ExtractedIsotopePattern annotation to the processed input,
 * this plugin will integrate the corresponding scores into the fragmentation pattern analysis
 */
public class IsotopePatternInMs1Plugin extends SiriusPlugin {


    @Override
    public void initializePlugin(PluginInitializer initializer) {
        initializer.addRootScorer(new Ms1IsotopePatternScorer());
    }

    @Override
    protected void transferAnotationsFromInputToGraph(ProcessedInput input, FGraph graph) {
        final ExtractedIsotopePattern pattern = input.getAnnotation(ExtractedIsotopePattern.class);
        if (pattern!=null) {
            final IsotopePattern iso = pattern.getExplanations().get(graph.getRoot().getChildren(0).getFormula());
            if (iso!=null) {
                graph.setAnnotation(IsotopePattern.class, iso);
            }
        }
    }

    @Override
    protected void transferAnotationsFromGraphToTree(ProcessedInput input, FGraph graph, FTree tree, IntergraphMapping graph2treeFragments) {
        final IsotopePattern pattern = graph.getAnnotation(IsotopePattern.class);
        if (pattern!=null) {
            tree.setAnnotation(IsotopePattern.class, pattern);
        }
    }

    @Override
    protected void beforeDecomposing(ProcessedInput input) {
        if (input.getExperimentInformation().getMolecularFormula()!=null) return;
        IsotopeSettings isotopeS = input.getAnnotation(IsotopeSettings.class);
        if (isotopeS!=null && isotopeS.isFiltering()) {
            final ExtractedIsotopePattern extractedIsotopePattern = input.getAnnotation(ExtractedIsotopePattern.class);
            if (extractedIsotopePattern!=null) {
                // find all high-scoring isotope pattern
                final MolecularFormula[] formulas = filterFormulasByIsotopeScore(extractedIsotopePattern);
                final Whiteset whiteset = input.getAnnotation(Whiteset.class);
                if (whiteset==null || whiteset.getFormulas().isEmpty()) {
                    input.setAnnotation(Whiteset.class, Whiteset.of(formulas));
                } else {
                    input.setAnnotation(Whiteset.class, whiteset.intersection(formulas));
                }

            }
        }
    }

    private MolecularFormula[] filterFormulasByIsotopeScore(ExtractedIsotopePattern pattern) {
        int isoPeaks = 0;
        double maxScore = Double.NEGATIVE_INFINITY;
        double scoreThresholdForFiltering = 0d;
        boolean doFilter = false;
        for (IsotopePattern pat : pattern.getExplanations().values()) {
            maxScore = Math.max(pat.getScore(), maxScore);
            final int numberOfIsoPeaks = pat.getPattern().size() - 1;
            if (pat.getScore() >= 2 * numberOfIsoPeaks) {
                isoPeaks = Math.max(pat.getPattern().size(), isoPeaks);
                scoreThresholdForFiltering = isoPeaks * 1d;
                doFilter = true;
            }
        }
        final double SCORE_THRESHOLD = scoreThresholdForFiltering;
        return pattern.getExplanations().entrySet().stream().filter(e -> e.getValue().getScore() >= SCORE_THRESHOLD).map(Map.Entry::getKey).toArray(MolecularFormula[]::new);
    }

    @Called("MS-Isotopes")
    protected static class Ms1IsotopePatternScorer implements DecompositionScorer<IsotopePatternInMs1Plugin.Prepared> {

        @Override
        public IsotopePatternInMs1Plugin.Prepared prepare(ProcessedInput input) {
            return new IsotopePatternInMs1Plugin.Prepared(input.getAnnotationOrDefault(IsotopeSettings.class), input.getAnnotation(ExtractedIsotopePattern.class, null));
        }

        @Override
        public double score(MolecularFormula formula, Ionization ion, ProcessedPeak peak, ProcessedInput input, IsotopePatternInMs1Plugin.Prepared precomputed) {
            if (precomputed.pattern!=null && precomputed.pattern.getExplanations().get(formula)!=null) {
                return precomputed.weight.getMultiplier() * precomputed.pattern.getExplanations().get(formula).getScore();
            } else return 0d;
        }

        @Override
        public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

        }

        @Override
        public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

        }
    }


    protected static class Prepared {
        private final IsotopeSettings weight;
        private final ExtractedIsotopePattern pattern;

        public Prepared(IsotopeSettings weight, ExtractedIsotopePattern pattern) {
            this.weight = weight;
            this.pattern = pattern;
        }
    }

}
