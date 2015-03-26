package de.unijena.bioinf.FragmentationTreeConstruction.computation;

import de.unijena.bioinf.ChemistryBase.algorithm.BoundedDoubleQueue;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.ScoredMolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.TreeScoring;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp.GurobiSolver;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

public class MultipleTreeComputation {

    private final double lowerbound;
    private final int maximalNumber;
    private final int numberOfThreads;
    private final List<ScoredMolecularFormula> formulas;
    private final FragmentationPatternAnalysis analyzer;
    private final ProcessedInput input;
    private final boolean recalibration;
    private final HashMap<MolecularFormula, FTree> backbones;

    MultipleTreeComputation(FragmentationPatternAnalysis analyzer, ProcessedInput input, List<ScoredMolecularFormula> formulas, double lowerbound, int maximalNumber, int numberOfThreads, boolean recalibration, HashMap<MolecularFormula, FTree> backbones) {
        this.analyzer = analyzer;
        this.input = input;
        this.formulas = formulas;
        this.lowerbound = lowerbound;
        this.maximalNumber = maximalNumber;
        this.numberOfThreads = numberOfThreads;
        this.recalibration = recalibration;
        this.backbones = backbones;
    }

    public MultipleTreeComputation withBackbones(FTree... backbones) {
        final HashMap<MolecularFormula, FTree> trees = new HashMap<MolecularFormula, FTree>();
        if (this.backbones != null) trees.putAll(this.backbones);
        for (FTree t : backbones) trees.put(t.getRoot().getFormula(), t);
        return new MultipleTreeComputation(analyzer, input, formulas, lowerbound, maximalNumber, numberOfThreads, recalibration, trees);
    }

    public MultipleTreeComputation withLowerbound(double lowerbound) {
        return new MultipleTreeComputation(analyzer, input, formulas, lowerbound, maximalNumber, numberOfThreads, recalibration, backbones);
    }

    public MultipleTreeComputation computeMaximal(int maximalNumber) {
        return new MultipleTreeComputation(analyzer, input, formulas, lowerbound, maximalNumber, numberOfThreads, recalibration, backbones);
    }

    public MultipleTreeComputation inParallel(int numberOfThreads) {
        return new MultipleTreeComputation(analyzer, input, formulas, lowerbound, maximalNumber, Math.max(1, Math.min(guessNumberOfThreads(), numberOfThreads)), recalibration, backbones);
    }

    public MultipleTreeComputation onlyWith(Iterable<MolecularFormula> formulas) {
        final HashSet<MolecularFormula> whitelist = new HashSet<MolecularFormula>();
        final Iterator<MolecularFormula> iter = formulas.iterator();
        while (iter.hasNext()) whitelist.add(iter.next());
        final List<ScoredMolecularFormula> pmds = new ArrayList<ScoredMolecularFormula>(whitelist.size());
        for (ScoredMolecularFormula f : this.formulas) {
            if (whitelist.contains(f.getFormula())) {
                pmds.add(f);
            }
        }
        return new MultipleTreeComputation(analyzer, input, pmds, lowerbound, maximalNumber, numberOfThreads, recalibration, backbones);
    }

    public MultipleTreeComputation withRoots(Collection<ScoredMolecularFormula> formulas) {
        return new MultipleTreeComputation(analyzer, input, new ArrayList<ScoredMolecularFormula>(formulas), lowerbound, maximalNumber, numberOfThreads, recalibration, backbones);
    }

    public MultipleTreeComputation without(Iterable<MolecularFormula> formulas) {
        final HashSet<MolecularFormula> blacklist = new HashSet<MolecularFormula>();
        final Iterator<MolecularFormula> iter = formulas.iterator();
        while (iter.hasNext()) blacklist.add(iter.next());
        if (blacklist.isEmpty()) return this;
        final List<ScoredMolecularFormula> pmds = new ArrayList<ScoredMolecularFormula>(Math.max(0, this.formulas.size() - blacklist.size()));
        for (ScoredMolecularFormula f : this.formulas) {
            if (!blacklist.contains(f.getFormula())) {
                pmds.add(f);
            }
        }
        return new MultipleTreeComputation(analyzer, input, pmds, lowerbound, maximalNumber, numberOfThreads, recalibration, backbones);
    }

    public MultipleTreeComputation inParallel() {
        return inParallel(1);
    }

    public FTree optimalTree() {
        double lb = lowerbound;
        FTree opt = null;
        final GraphBuildingQueue queue = numberOfThreads > 1 ? new MultithreadedGraphBuildingQueue() : new SinglethreadedGraphBuildingQueue();
        while (queue.hasNext()) {
            final FGraph graph = queue.next();
            final FTree tree = computeTree(graph, lb, recalibration);
            if (tree != null && (opt == null || tree.getAnnotationOrThrow(TreeScoring.class).getOverallScore() > opt.getAnnotationOrThrow(TreeScoring.class).getOverallScore())) {
                opt = tree;
            }
        }
        return opt;
    }

    /**
     * @see de.unijena.bioinf.FragmentationTreeConstruction.computation.MultipleTreeComputation#iterator(boolean)
     */
    public TreeIterator iterator() {
        return iterator(false);
    }

    /**
     * returns an iterator over all trees. The tree computation is done in each iteration step.
     * The iteration is not in order of the score. To find the optimal tree, all trees have to be iterated.
     * However, if a maximum number of trees is given (for example: 1), a lowerbound will be increased to speed up
     * tree computation. Computation of trees with too low scores will be aborted early. The iterator will then return
     * null values.
     * @param oncePerNext if true, only one tree is computed with each invocation of next(). This tree might be null, if it's score is less than the lowerbound. If false, null values are skipped. However, the last invocation of next() might lead to a null value, as hasNext() does not know if the next value will be null.
     * @return
     */
    public TreeIterator iterator(final boolean oncePerNext) {
        return new TreeIterator() {
            private final GraphBuildingQueue queue = numberOfThreads > 1 ? new MultithreadedGraphBuildingQueue() : new SinglethreadedGraphBuildingQueue();
            private double lb = lowerbound;
            private BoundedDoubleQueue scores = maximalNumber < formulas.size() ? new BoundedDoubleQueue(maximalNumber) : null;
            private FGraph lastGraph;

            @Override
            public boolean hasNext() {
                return queue.hasNext();
            }

            @Override
            public FTree next() {
                while (hasNext()) {
                    lastGraph = queue.next();
                    FTree tree = computeTree(lastGraph, lb, recalibration);
                    if (tree != null) {
                        if (scores != null) {
                            scores.add(tree.getAnnotationOrThrow(TreeScoring.class).getOverallScore());
                            setLowerbound(Math.max(scores.min(), getLowerbound()));
                        }
                        return tree;
                    }
                    if (oncePerNext) return tree;
                }
                lastGraph = null;
                return null;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void setLowerbound(double lowerbound) {
                lb = lowerbound;
            }

            @Override
            public double getLowerbound() {
                return lb;
            }

            @Override
            public FGraph lastGraph() {
                return lastGraph;
            }
        };
    }

    public List<FTree> list() {
        final NavigableSet<FTree> trees = new TreeSet<FTree>(new Comparator<FTree>() {
            @Override
            public int compare(FTree o1, FTree o2) {
                return new Double(o1.getAnnotationOrThrow(TreeScoring.class).getOverallScore()).compareTo(o2.getAnnotationOrThrow(TreeScoring.class).getOverallScore());
            }
        });
        double lb = lowerbound;
        final GraphBuildingQueue queue = numberOfThreads > 1 ? new MultithreadedGraphBuildingQueue() : new SinglethreadedGraphBuildingQueue();
        while (queue.hasNext()) {
            final FGraph graph = queue.next();
            final FTree tree = computeTree(graph, lb, recalibration);
            if (tree != null) {
                trees.add(tree);
                if (trees.size() > maximalNumber) {
                    trees.pollFirst();
                    final double newLowerbound = Math.max(0, trees.first().getAnnotationOrThrow(TreeScoring.class).getOverallScore());
                    assert newLowerbound >= lb : newLowerbound + " should be greater than " + lb;
                    lb = Math.max(lowerbound, newLowerbound);
                }
            }
        }
        return new ArrayList<FTree>(trees.descendingSet());
    }

    private FTree computeTree(FGraph graph, double lb, boolean recalibration) {
        final TreeBuilder tb = analyzer.getTreeBuilder();
        final FTree backbone = backbones == null ? null : backbones.get(graph.getRoot().getFormula());
        if (backbone != null && tb instanceof GurobiSolver) {
            final GurobiSolver gb = (GurobiSolver) tb;
            try {
                gb.setFeasibleSolver(new BackboneTreeBuilder(backbone));
                return analyzer.computeTree(graph, lb, recalibration);
            } finally {
                gb.setFeasibleSolver(null);
            }
        } else {
            // does not support backbones
            return analyzer.computeTree(graph, lb, recalibration);
        }
    }

    /**
     * returns the number of threads that can be used for graph computation. It is assumed that each graph needs
     * 1 GB memory, so multiple threads are only allowed if there is enough memory.
     */
    protected int guessNumberOfThreads() {
        final int maxNumber = Runtime.getRuntime().availableProcessors();
        final long gigabytes = Math.round(((double) Runtime.getRuntime().maxMemory()) / (1024 * 1024 * 1024));
        return Math.max(1, Math.min(maxNumber, (int) gigabytes));
    }

    public MultipleTreeComputation withRecalibration(boolean recalibration) {
        return new MultipleTreeComputation(analyzer, input, formulas, lowerbound, maximalNumber, numberOfThreads, recalibration, backbones);
    }

    public MultipleTreeComputation withRecalibration() {
        return withRecalibration(true);
    }

    public MultipleTreeComputation withoutRecalibration() {
        return withRecalibration(false);
    }

    private static class BackboneTreeBuilder implements TreeBuilder {

        private final FTree backbone;

        private BackboneTreeBuilder(FTree backbone) {
            this.backbone = backbone;
        }

        @Override
        public Object prepareTreeBuilding(ProcessedInput input, FGraph graph, double lowerbound) {
            return null;
        }

        @Override
        public FTree buildTree(ProcessedInput input, FGraph graph, double lowerbound, Object preparation) {
            return buildTree(input, graph, lowerbound);
        }

        @Override
        public FTree buildTree(ProcessedInput input, FGraph graph, double lowerbound) {
            if (!graph.getRoot().getFormula().equals(backbone.getRoot().getFormula()))
                throw new RuntimeException("Backbone is not matching graph");
            return backbone;
        }

        @Override
        public List<FTree> buildMultipleTrees(ProcessedInput input, FGraph graph, double lowerbound, Object preparation) {
            throw new RuntimeException("Stub code");
        }

        @Override
        public List<FTree> buildMultipleTrees(ProcessedInput input, FGraph graph, double lowerbound) {
            throw new RuntimeException("Stub code");
        }
    }

    abstract class GraphBuildingQueue implements Iterator<FGraph> {

        protected final List<ScoredMolecularFormula> stack;

        protected GraphBuildingQueue() {
            stack = new ArrayList<ScoredMolecularFormula>(formulas);
            Collections.reverse(stack);
        }

        @Override
        public boolean hasNext() {
            synchronized (stack) {
                return !stack.isEmpty();
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public class SinglethreadedGraphBuildingQueue extends GraphBuildingQueue {

        @Override
        public FGraph next() {
            return analyzer.buildGraph(input, stack.remove(stack.size() - 1));
        }
    }

    class MultithreadedGraphBuildingQueue extends GraphBuildingQueue {
        private final ArrayBlockingQueue<FGraph> queue = new ArrayBlockingQueue<FGraph>(numberOfThreads);

        MultithreadedGraphBuildingQueue() {
            startComputation();
        }

        private void startComputation() {
            final int n = formulas.size();
            final ProcessedInput pinput = input;
            final FragmentationPatternAnalysis anal = analyzer;
            for (int k = 0; k < numberOfThreads; ++k) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (true) {
                            final ScoredMolecularFormula f;
                            synchronized (stack) {
                                final int size = stack.size();
                                if (size == 0) break;
                                else f = stack.remove(size - 1);
                            }
                            try {
                                queue.put(anal.buildGraph(pinput, f));
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }).start();
            }
        }

        @Override
        public FGraph next() {
            if (!hasNext()) throw new NoSuchElementException();
            try {
                return queue.take();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }
}
