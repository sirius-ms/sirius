/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.FragmentationTreeConstruction.computation;

import com.google.common.base.Function;
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
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

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

    public MultipleTreeComputation onlyWithIons(Iterable<MolecularFormula> formulas) {
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

    public MultipleTreeComputation onlyWith(Iterable<MolecularFormula> formulas) {
        final HashSet<MolecularFormula> whitelist = new HashSet<MolecularFormula>();
        final Iterator<MolecularFormula> iter = formulas.iterator();
        while (iter.hasNext()) whitelist.add(input.getExperimentInformation().getPrecursorIonType().neutralMoleculeToMeasuredNeutralMolecule(iter.next()));
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
        while (iter.hasNext()) blacklist.add(input.getExperimentInformation().getPrecursorIonType().neutralMoleculeToMeasuredNeutralMolecule(iter.next()));
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
        return inParallel(2);
    }

    public FTree optimalTree() {
        double lb = lowerbound;
        FTree opt = null;
        final Iterator<FGraph> queue = numberOfThreads > 1 ? new MultithreadedGraphBuildingQueue() : new SinglethreadedGraphBuildingQueue();
        try {
            while (queue.hasNext()) {
                final FGraph graph = queue.next();
                final FTree tree = computeTree(graph, lb, recalibration);
                if (tree != null && (opt == null || tree.getAnnotationOrThrow(TreeScoring.class).getOverallScore() > opt.getAnnotationOrThrow(TreeScoring.class).getOverallScore())) {
                    opt = tree;
                }
            }
            return opt;
        } catch (RuntimeException e) {
            if (queue instanceof MultithreadedGraphBuildingQueue) {
                ((MultithreadedGraphBuildingQueue)queue).kill();
            }
            throw e;
        }
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
            private final Iterator<FGraph> queue = numberOfThreads > 1 ? new MultithreadedGraphBuildingQueue() : new SinglethreadedGraphBuildingQueue();
            private double lb = lowerbound;
            private BoundedDoubleQueue scores = maximalNumber < formulas.size() ? new BoundedDoubleQueue(maximalNumber) : null;
            private FGraph lastGraph;

            @Override
            public boolean hasNext() {
                return queue.hasNext();
            }

            @Override
            public FTree next() {
                try {
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
                } catch (RuntimeException e) {
                    if (queue instanceof MultithreadedGraphBuildingQueue) {
                        ((MultithreadedGraphBuildingQueue)queue).kill();
                    }
                    throw e;
                }
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
        final Iterator<FGraph> queue = numberOfThreads > 1 ? new MultithreadedGraphBuildingQueue() : new SinglethreadedGraphBuildingQueue();
        try {
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
        } catch (RuntimeException e) {
        if (queue instanceof MultithreadedGraphBuildingQueue) {
            ((MultithreadedGraphBuildingQueue)queue).kill();
        }
        throw e;
    }
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
     * at max 512 GB memory, so multiple threads are only allowed if there is enough memory.
     */
    protected int guessNumberOfThreads() {
        final int maxNumber = Runtime.getRuntime().availableProcessors();
        final long gigabytes = Math.round(((double) Runtime.getRuntime().maxMemory()) / (512 * 1024 * 1024));
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

        @Override
        public String getDescription() {
            return "stub";
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

    class MultithreadedGraphBuildingQueue extends MultithreadedWorkingQueue<ScoredMolecularFormula, FGraph> {
        MultithreadedGraphBuildingQueue() {
            super(formulas, new Function<ScoredMolecularFormula, FGraph>() {
                @Override
                public FGraph apply(ScoredMolecularFormula mf) {
                    return analyzer.buildGraph(input, mf);
                }
            }, numberOfThreads, 1); // TODO: better documentation. in general it doesn't make sense to use more than one thread for graph building
            start();
        }
    }

    private static class MultithreadedWorkingQueue<In, Out> implements Runnable, Iterator<Out> {
        private final ArrayDeque<In> todoList;
        private int pendingJobs;
        private final ArrayDeque<Out> resultList;
        private final Function<In, Out> worker;
        private final Thread[] threads;

        private final int maxNumberOfResults;
        private final int maxNumberOfThreads;

        private final ReentrantLock lock;
        private final Condition notFull, notEmpty;

        private boolean shutdown = false;

        public MultithreadedWorkingQueue(List<In> todoList, Function<In, Out> worker, int maxNumberOfResults, int maxNumberOfThreads) {
            this.todoList = new ArrayDeque<In>(todoList);
            this.pendingJobs = 0;
            this.resultList = new ArrayDeque<Out>(maxNumberOfResults);
            this.maxNumberOfResults = maxNumberOfResults;
            this.maxNumberOfThreads = maxNumberOfThreads;
            this.worker = worker;
            this.threads = new Thread[maxNumberOfThreads];
            this.lock = new ReentrantLock();
            this.notFull = lock.newCondition();
            this.notEmpty = lock.newCondition();
            for (int k=0; k < threads.length; ++k) {
                threads[k] = new Thread(this);
            }
        }

        void kill() {
            lock.lock();
            try {
                shutdown=true;
                notEmpty.signalAll();
                notFull.signalAll();
            } finally {
                lock.unlock();
            }
        }

        public void start() {
            for (Thread t : threads) t.start();
        }

        @Override
        public void run() {
            outerLoop:
            while (!shutdown) {
                final In input;
                // pull a job out of the todolist
                lock.lock();
                try {
                    if (todoList.size() > 0) {
                        input = todoList.pollFirst();
                        ++pendingJobs;
                    } else return;
                } finally {
                    lock.unlock();
                }
                // process pending job
                final Out output = worker.apply(input);
                // put result into result list
                lock.lock();
                try {
                    while (resultList.size() >= maxNumberOfResults) {
                        if (shutdown) return;
                        notFull.await();
                    }
                    resultList.offerLast(output);
                    --pendingJobs;
                    notEmpty.signalAll();
                } catch (InterruptedException e) {
                    return;
                } finally {
                    lock.unlock();
                }
            }
        }

        @Override
        public boolean hasNext() {
            lock.lock();
            try {
                return resultList.size() > 0 || pendingJobs > 0 || todoList.size() > 0;
            } finally {
                lock.unlock();
            }
        }

        @Override
        public Out next() {
            lock.lock();
            try {
                while (resultList.isEmpty()) {
                    notEmpty.await();
                }
                final Out out = resultList.pollFirst();
                notFull.signalAll();
                return out;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                lock.unlock();
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

}
