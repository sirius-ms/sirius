package de.unijena.bioinf.FragmentationTreeConstruction.computation;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.ScoredMolecularFormula;
import de.unijena.bioinf.FragmentationTreeConstruction.model.FragmentationGraph;
import de.unijena.bioinf.FragmentationTreeConstruction.model.FragmentationTree;
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

    MultipleTreeComputation(FragmentationPatternAnalysis analyzer, ProcessedInput input, List<ScoredMolecularFormula> formulas, double lowerbound, int maximalNumber, int numberOfThreads) {
        this.analyzer = analyzer;
        this.input = input;
        this.formulas = formulas;
        this.lowerbound = lowerbound;
        this.maximalNumber = maximalNumber;
        this.numberOfThreads = numberOfThreads;
    }

    public MultipleTreeComputation withLowerbound(double lowerbound) {
        return new MultipleTreeComputation(analyzer, input,formulas, lowerbound, maximalNumber, numberOfThreads);
    }

    public MultipleTreeComputation computeMaximal(int maximalNumber) {
        return new MultipleTreeComputation(analyzer, input, formulas, lowerbound, maximalNumber, numberOfThreads);
    }

    public MultipleTreeComputation inParallel(int numberOfThreads) {
        return new MultipleTreeComputation(analyzer, input, formulas, lowerbound, maximalNumber, Math.max(1, Math.min(guessNumberOfThreads(), numberOfThreads)));
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
        return new MultipleTreeComputation(analyzer, input, pmds, lowerbound, maximalNumber, numberOfThreads);
    }

    public MultipleTreeComputation without(Iterable<MolecularFormula> formulas) {
        final HashSet<MolecularFormula> blacklist = new HashSet<MolecularFormula>();
        final Iterator<MolecularFormula> iter = formulas.iterator();
        while (iter.hasNext()) blacklist.add(iter.next());
        if (blacklist.isEmpty()) return this;
        final List<ScoredMolecularFormula> pmds = new ArrayList<ScoredMolecularFormula>(Math.max(0,this.formulas.size()-blacklist.size()));
        for (ScoredMolecularFormula f : this.formulas) {
            if (!blacklist.contains(f.getFormula())) {
                pmds.add(f);
            }
        }
        return new MultipleTreeComputation(analyzer, input, pmds, lowerbound, maximalNumber, numberOfThreads);
    }

    public MultipleTreeComputation inParallel() {
        return inParallel(1);
    }

    public FragmentationTree optimalTree() {
        double lb = lowerbound;
        FragmentationTree opt = null;
        final GraphBuildingQueue queue =  numberOfThreads > 1 ? new MultithreadedGraphBuildingQueue() : new SinglethreadedGraphBuildingQueue();
        while (queue.hasNext()) {
            final FragmentationGraph graph = queue.next();
            final FragmentationTree tree = analyzer.computeTree(graph, lb);
            if (tree != null && (opt == null || tree.getScore() < opt.getScore())) {
                opt = tree;
            }
        }
        return opt;
    }

    public TreeIterator iterator() {
        return new TreeIterator() {
            private final GraphBuildingQueue queue =  numberOfThreads > 1 ? new MultithreadedGraphBuildingQueue() : new SinglethreadedGraphBuildingQueue();
            private double lb = lowerbound;
            @Override
            public boolean hasNext() {
                return queue.hasNext();
            }

            @Override
            public FragmentationTree next() {
                while (hasNext()) {
                    FragmentationTree tree = analyzer.computeTree(queue.next(), lb);
                    if (tree != null) return tree;
                }
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
        };
    }

    public List<FragmentationTree> list() {
        final NavigableSet<FragmentationTree> trees = new TreeSet<FragmentationTree>();
        double lb = lowerbound;
        final GraphBuildingQueue queue = numberOfThreads > 1 ? new MultithreadedGraphBuildingQueue() : new SinglethreadedGraphBuildingQueue();
        while (queue.hasNext()) {
            final FragmentationGraph graph = queue.next();
            final FragmentationTree tree = analyzer.computeTree(graph, lb);
            if (tree != null) {
                trees.add(tree);
                if (trees.size() > maximalNumber) {
                    trees.pollFirst();
                    final double newLowerbound = Math.max(0, trees.first().getScore());
                    assert newLowerbound >= lb : newLowerbound + " should be greater than " + lb;
                    lb = Math.max(lowerbound, newLowerbound);
                }
            }
        }
        return new ArrayList<FragmentationTree>(trees.descendingSet());
    }

    /**
     * returns the number of threads that can be used for graph computation. It is assumed that each graph needs
     * 1 GB memory, so multiple threads are only allowed if there is enough memory.
     */
    protected int guessNumberOfThreads() {
        final int maxNumber = Runtime.getRuntime().availableProcessors();
        final long gigabytes = Math.round(((double)Runtime.getRuntime().maxMemory())/(1024*1024*1024));
        return Math.max(1, Math.min(maxNumber, (int) gigabytes));
    }

    abstract class GraphBuildingQueue implements Iterator<FragmentationGraph> {

        protected final List<ScoredMolecularFormula> stack = new ArrayList<ScoredMolecularFormula>(formulas);

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
        public FragmentationGraph next() {
            return analyzer.buildGraph(input, stack.remove(stack.size()-1));
        }
    }

    class MultithreadedGraphBuildingQueue extends GraphBuildingQueue {
        private final ArrayBlockingQueue<FragmentationGraph> queue = new ArrayBlockingQueue<FragmentationGraph>(numberOfThreads);

        MultithreadedGraphBuildingQueue() {
            startComputation();
        }

        private void startComputation() {
            final int n = formulas.size();
            final ProcessedInput pinput = input;
            final FragmentationPatternAnalysis anal = analyzer;
            for (int k=0; k < numberOfThreads; ++k) {
                new Thread(new Runnable(){
                    @Override
                    public void run() {
                        while (true) {
                            final ScoredMolecularFormula f;
                            synchronized (stack) {
                                final int size = stack.size();
                                if (size==0) break;
                                else f = stack.remove(size-1);
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
        public FragmentationGraph next() {
            if (!hasNext()) throw new NoSuchElementException();
            try {
                return queue.take();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }
}
