package de.unijena.bioinf.FragmentationTreeConstruction.computation;

import de.unijena.bioinf.ChemistryBase.ms.ft.Beautified;
import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.Decomposition;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.sirius.ProcessedInput;

import java.util.List;

public abstract class AbstractTreeComputationInstance extends BasicMasterJJob<AbstractTreeComputationInstance.FinalResult> {

    protected final FragmentationPatternAnalysis analyzer;
    protected ProcessedInput pinput;

    public static final double MAX_TREESIZE = 2.5d;
    public static final double MAX_TREESIZE_INCREASE = 3d;
    public static final double TREE_SIZE_INCREASE = 1d;
    public static final int MIN_NUMBER_OF_EXPLAINED_PEAKS = 15;
    public static final double MIN_EXPLAINED_INTENSITY = 0.7d;
    public static final int MIN_NUMBER_OF_TREES_CHECK_FOR_INTENSITY = 5;

    public ProcessedInput getProcessedInput() {
        return pinput;
    }

    public final static class FinalResult {
    protected final boolean canceledDueToLowScore;
    protected final List<FTree> results;

    public FinalResult(List<FTree> results) {
        this.canceledDueToLowScore = false;
        this.results = results;
    }

    public FinalResult() {
        this.canceledDueToLowScore = true;
        this.results = null;
    }

    public List<FTree> getResults() {
        return results;
    }
}

    public AbstractTreeComputationInstance(FragmentationPatternAnalysis analyzer) {
        super(JJob.JobType.CPU);
        this.analyzer = analyzer;
    }

    protected boolean checkForTreeQuality(List<ExactResult> results, boolean addAnnotation) {
        boolean any = false;
        for (ExactResult r : results) {
            final FTree tree = r.tree;
            if (analyzer.getIntensityRatioOfExplainedPeaksFromUnanotatedTree(pinput, tree) >= MIN_EXPLAINED_INTENSITY && tree.numberOfVertices() >= Math.min(pinput.getMergedPeaks().size() - 2, MIN_NUMBER_OF_EXPLAINED_PEAKS)) {
                any = true;
                if (addAnnotation) tree.setAnnotation(Beautified.class,Beautified.beautified(0d));
                else return true;
            } else if (addAnnotation) tree.setAnnotation(Beautified.class, Beautified.IS_UGGLY);
        }
        return any;
    }


    protected final static class ExactResult implements Comparable<ExactResult> {

        protected final Decomposition decomposition;
        protected final double score;
        protected FGraph graph;
        protected FTree tree;

        public ExactResult(Decomposition decomposition, FGraph graph, FTree tree, double score) {
            this.decomposition = decomposition;
            this.score = score;
            this.tree = tree;
            this.graph = graph;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof ExactResult) return equals((ExactResult) o);
            else return false;
        }

        public boolean equals(ExactResult o) {
            return score == o.score && decomposition.getCandidate().equals(o.decomposition.getCandidate());
        }

        @Override
        public int compareTo(ExactResult o) {
            final int a = Double.compare(score, o.score);
            if (a != 0) return a;
            return decomposition.getCandidate().compareTo(o.decomposition.getCandidate());
        }
    }
}
