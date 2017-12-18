package de.unijena.bioinf.FragmentationTreeConstruction.computation.tree;

import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;

public interface TreeBuilder {

    public FluentInterface computeTree();

    public Result computeTree(ProcessedInput input, FGraph graph, FluentInterface options);

    public boolean isThreadSafe();

    public static class FluentInterface {
        private final TreeBuilder treeBuilder;
        private final double minimalScore;
        private final double timeLimitsInSeconds;
        private final int numberOfCPUS;
        private final FTree template;

        public FluentInterface(TreeBuilder treeBuilder) {
            this(treeBuilder, Double.NEGATIVE_INFINITY, 0, 1, null);
        }

        public FluentInterface(TreeBuilder treeBuilder, double minimalScore, double timeout, int numberOfCPUS, FTree template) {
            this.treeBuilder = treeBuilder;
            this.minimalScore = minimalScore;
            this.timeLimitsInSeconds = timeout;
            this.numberOfCPUS = numberOfCPUS;
            this.template = template;
        }

        public double getMinimalScore() {
            return minimalScore;
        }

        public double getTimeLimitsInSeconds() {
            return timeLimitsInSeconds;
        }

        public int getNumberOfCPUS() {
            return numberOfCPUS;
        }

        public FTree getTemplate() {
            return template;
        }

        public FluentInterface withMinimalScore(double score) {
            return new FluentInterface(treeBuilder, score, timeLimitsInSeconds, numberOfCPUS, template);
        }

        public FluentInterface withTimeLimit(double seconds) {
            return new FluentInterface(treeBuilder, minimalScore, seconds, numberOfCPUS, template);
        }

        public FluentInterface withMultithreading(int numberOfCPUS) {
            return new FluentInterface(treeBuilder, minimalScore, timeLimitsInSeconds, numberOfCPUS, template);
        }

        public FluentInterface withTemplate(FTree tree) {
            return new FluentInterface(treeBuilder, minimalScore, timeLimitsInSeconds, numberOfCPUS, tree);
        }

        public Result solve(ProcessedInput input, FGraph graph) {
            return treeBuilder.computeTree(input,graph,this);
        }
    }

    public static enum AbortReason {
        COMPUTATION_CORRECT, // when everything is fine
        INFEASIBLE,     // should never happen
        TIMEOUT,        // is used when timeout reached
        NO_SOLUTION     // is used when no tree with reasonable score is found
    };

    public static class Result {

        public final boolean isOptimal;
        public final AbortReason error;
        public final FTree tree;

        public Result(FTree tree, boolean isOptimal, AbortReason error) {
            this.isOptimal = isOptimal;
            this.error = error;
            this.tree = tree;
        }
    }

}
