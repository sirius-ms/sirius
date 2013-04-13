package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.FragmentationTreeConstruction.model.*;

/**
 * @author Kai Dührkop
 */
public class ParentToChildRatioScorer implements EdgeScorer {

    private final AbstractDecompositionScorer decompositionScorer;

    public ParentToChildRatioScorer(AbstractDecompositionScorer decompositionScorer) {
        this.decompositionScorer = decompositionScorer;
    }

    @Override
    public double score(Loss loss, ProcessedInput input, Object precomputed) {
        final double[] vertexScores = (double[])precomputed;
        final double parentScore = vertexScores[loss.getHead().getIndex()];
        if (Double.isInfinite(parentScore)) return 0;
        final double childScore = vertexScores[loss.getTail().getIndex()];
        final double ratio = childScore - parentScore;
        assert !Double.isNaN(ratio) : decompositionScorer.getClass().getName() + " returns NaN as score for " + loss;
        return Math.min(ratio, 0);
    }

    @Override
    public Object prepare(ProcessedInput input, FragmentationPathway graph) {
        final int n;
        if (graph instanceof FragmentationTree) {
            int max = 0;
            for (Fragment f : graph.getFragments()) max = Math.max(f.getIndex(), max);
            n = max+1;
        } else {
            n = graph.numberOfVertices();
        }
        final double[] vertexScores = new double[n];
        for (Fragment f : graph.getFragmentsWithoutRoot()) {
            vertexScores[f.getIndex()] = decompositionScorer.score(f.getDecomposition().getFormula(), f.getPeak(), input);
        }
        return vertexScores;
    }
}
