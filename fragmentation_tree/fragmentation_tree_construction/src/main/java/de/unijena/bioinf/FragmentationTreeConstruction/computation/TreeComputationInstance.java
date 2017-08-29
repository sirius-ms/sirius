package de.unijena.bioinf.FragmentationTreeConstruction.computation;

import com.google.common.collect.Iterables;
import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.TreeScoring;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.SinglethreadedTreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.ftheuristics.solver.CriticalPathSolver;
import de.unijena.bioinf.FragmentationTreeConstruction.model.DecompositionList;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Whiteset;
import gnu.trove.list.array.TDoubleArrayList;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

public class TreeComputationInstance {

    protected final ExecutorService service;
    protected final int numberOfWorkers;
    protected final Ms2Experiment input;
    protected final FragmentationPatternAnalysis analyzer;
    protected final int numberOfResultsToKeep, capacity;

    protected volatile boolean canceled;

    // whiteset
    protected final Set<MolecularFormula> neutralFormulaWhiteset;

    // intermediate results
    protected final Ionization[] ionizations;
    protected final ProcessedInput[] processedInputs;

    // Multithreading queues
    protected final HashMap<MolecularFormula, WeakReference<FGraph>> graphCache;
    protected final ConcurrentLinkedQueue<TreeJob> jobsToCompute;

    protected ReentrantLock resultLock = new ReentrantLock();

    // results
    protected final ConcurrentLinkedQueue<TreeJob> intermediates;
    protected final TreeSet<TreeJob> resultList;

    public TreeComputationInstance(ExecutorService service, int numberOfWorkers, FragmentationPatternAnalysis analyzer, Ms2Experiment input, int numberOfResultsToKeep, Set<MolecularFormula> set) {
        this.service = service;
        this.numberOfWorkers = numberOfWorkers;
        this.analyzer = analyzer;
        this.input = input;
        this.numberOfResultsToKeep = numberOfResultsToKeep;
        this.canceled = false;
        this.heuristicGap = Double.NEGATIVE_INFINITY;

        this.neutralFormulaWhiteset = new HashSet<>();
        if (set != null) {
            neutralFormulaWhiteset.addAll(set);
        }

        ProcessedInput pinput = analyzer.performValidation(input);
        final PrecursorIonType ionType = pinput.getExperimentInformation().getPrecursorIonType();
        if (ionType.isIonizationUnknown()) {
            this.ionizations = Iterables.toArray(PeriodicTable.getInstance().getKnownIonModes(ionType.getCharge()), Ionization.class);
        } else {
            this.ionizations = new Ionization[]{ionType.getIonization()};
        }
        this.processedInputs = new ProcessedInput[ionizations.length];
        if (ionizations.length==1 && !ionType.isIonizationUnknown()) {
            processedInputs[0] = pinput;
        } else {
            for (int k=0; k < ionizations.length; ++k) {
                final Ms2ExperimentShallowCopy shallowCopy = new Ms2ExperimentShallowCopy(input, PrecursorIonType.getPrecursorIonType(ionizations[k]));
                processedInputs[k] = analyzer.performValidation(shallowCopy);
            }
        }
        for (ProcessedInput pr : processedInputs) {
            if (!neutralFormulaWhiteset.isEmpty()) {
                pr.setAnnotation(Whiteset.class, new Whiteset(neutralFormulaWhiteset));
            }
        }
        this.capacity = this.numberOfResultsToKeep+10;
        this.jobsToCompute = new ConcurrentLinkedQueue<>();
        this.graphCache = new HashMap<>(Math.min(20, numberOfResultsToKeep));
        this.intermediates = new ConcurrentLinkedQueue<>();
        this.resultList = new TreeSet<>(new Comparator<TreeJob>() {
            @Override
            public int compare(TreeJob o1, TreeJob o2) {
                return Double.compare(o1.exactScore, o2.exactScore);
            }
        });

        final Thread gcWatch = new Thread(new Runnable() {
            @Override
            public void run() {
                Reference<? extends FGraph> ref;
                try {
                    while ((ref=referenceQueue.remove())!=null) {
                        System.out.println("Reference is garbage collected!");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        gcWatch.start();



    }

    public List<FTree> getTrees() {
        final ArrayList<FTree> trees = new ArrayList<>();
        int k=0;
        final Iterator<TreeJob> iter = resultList.descendingIterator();
        while (iter.hasNext()) {
            trees.add(iter.next().tree);
            if (++k >= numberOfResultsToKeep) break;
        }
        return trees;
    }

    public void startComputingMultithreaded() {
        System.out.println("Start  computing");
        // first estimate number of all decompositions
        final List<Future<?>> futures = new ArrayList<>();
        for (int k=0; k < processedInputs.length; ++k) {
            final int K = k;
            futures.add(service.submit(new Runnable() {
                @Override
                public void run() {
                    initializeInputFor(K);
                }
            }));
        }
        for (Future f : futures) {
            try {
                f.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Heuristics and Graphs (" + jobsToCompute.size() + ")");
        // now compute all formulas in parallel
        futures.clear();
        for (final TreeJob job : jobsToCompute) {
            futures.add(service.submit(new Runnable() {
                @Override
                public void run() {
                    computeHeuristicTree(job, computeGraph(job));
                }
            }));
        }
        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Exact Trees");
        final TreeJob[] jobs = jobsToCompute.toArray(new TreeJob[0]);
        Arrays.sort(jobs, Collections.<TreeJob>reverseOrder());
        futures.clear();
        heuristicGap = Double.NEGATIVE_INFINITY;
        lowerbound = Double.NEGATIVE_INFINITY;
        // now compute exact solutions until lowerbound reached
        if (analyzer.getTreeBuilder() instanceof SinglethreadedTreeBuilder) {
            throw new UnsupportedOperationException();
        } else {
            for (final TreeJob job : jobs) {
                futures.add(service.submit(new Runnable() {
                    @Override
                    public void run() {
                        final double hgap = heuristicGap;
                        final int capa = Double.isInfinite(hgap) ? capacity : numberOfResultsToKeep;
                        if (job.heuristicScore + hgap < lowerbound) {
                            System.out.println("Skip " + job);
                            return;
                        }
                        computeExactTree(job, computeGraph(job));
                        resultLock.lock();
                        if (resultList.size() >= capa) {
                            if (Double.isInfinite(hgap)) {
                                resultList.add(job);
                                calculateLowerbound();
                                final Iterator<TreeJob> jiter = resultList.iterator();
                                for (int i=numberOfResultsToKeep; i < capa && jiter.hasNext(); ++i) {
                                    jiter.next();
                                    jiter.remove();
                                }
                                lowerbound = resultList.first().exactScore;
                            } else {
                                final TreeJob worstJob = resultList.last();
                                if (worstJob.exactScore < job.score) {
                                    resultList.add(job);
                                    resultList.pollFirst();
                                    lowerbound = resultList.first().exactScore;
                                }
                            }
                        } else {
                            resultList.add(job);
                        }
                        resultLock.unlock();
                    }
                }));
            }
        }

        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    private void initializeInputFor(int k) {
        processedInputs[k] = analyzer.preprocessInputForGraphBuilding(processedInputs[k]);
        for (Scored<MolecularFormula> formula : processedInputs[k].getAnnotationOrThrow(DecompositionList.class).getDecompositions()) {
            jobsToCompute.add(new TreeJob(formula, k));
        }
    }

    private final ReferenceQueue<FGraph> referenceQueue = new ReferenceQueue<>();
    protected FGraph computeGraph(TreeJob job) {
        WeakReference<FGraph> ref = graphCache.get(job.candidate.getCandidate());
        if (ref != null) {
            FGraph g = ref.get();
            if (g != null) {
                return g;
            } else System.out.println("Recompute graph " + job.candidate);
        }
        final long time1 = System.nanoTime();
        final FGraph graph = analyzer.buildGraph(processedInputs[job.ionType], job.candidate);
        synchronized (graphCache){
            final WeakReference<FGraph> reference =  new WeakReference<FGraph>(graph,   referenceQueue);
            graphCache.put(job.candidate.getCandidate(), reference);
        }
        final long time2 = System.nanoTime();
        System.out.println("Build graph " + job.candidate + " + in (" + (time2-time1)/1000000d + "ms");
        return graph;
    }

    protected void computeHeuristicTree(TreeJob job, FGraph graph) {
        final long time1 = System.nanoTime();
        final CriticalPathSolver solver = new CriticalPathSolver(graph);
        final FTree tree = solver.solve();
        final long time2 = System.nanoTime();
        analyzer.addTreeAnnotations(graph, tree);
        job.heuristicScore = job.score = tree.getAnnotationOrThrow(TreeScoring.class).getOverallScore();
        System.out.println("Heuristicaly solve graph " + job.candidate + " + in (" + (time2-time1)/1000000d + "ms");
    }

    protected void computeExactTree(TreeJob job, FGraph graph) {
        job.tree = analyzer.computeTree(graph, Double.isNaN(job.heuristicScore) ? Double.NEGATIVE_INFINITY : job.heuristicScore - 1e-16);
        job.exactScore = job.score = job.tree.getAnnotationOrThrow(TreeScoring.class).getOverallScore();
    }

    protected volatile double heuristicGap, lowerbound;


    private void calculateLowerbound() {
        TDoubleArrayList values = new TDoubleArrayList(resultList.size());
        for (TreeJob job : new ArrayList<>(resultList)) {
            values.add(job.exactScore - job.heuristicScore);
        }
        values.sort();
        heuristicGap = Math.max(heuristicGap, values.get(values.size()-5));
        System.out.println("SET GAP TO " + heuristicGap);
    }

    // 1. Multithreaded: Berechne ProcessedInput für alle Ionisierungen
    // 2. Multithreaded: Berechne Graphen für alle Ionisierungen, berechne Bäume via Heuristik
    // 3. evtl. Multithreaded: Berechne exakte Lösung für jeden Baum
    // 4. Breche ab, wenn ausreichend gute exakte Lösungen gefunden wurden

    protected static class TreeJob implements Comparable<TreeJob> {

        protected final Scored<MolecularFormula> candidate;
        protected final int ionType;
        protected double heuristicScore, exactScore, score;
        protected FTree tree;

        public TreeJob(Scored<MolecularFormula> formula, int ionType) {
            this.candidate = formula;
            this.ionType = ionType;
            this.heuristicScore = Double.NaN;
            this.exactScore = Double.NaN;
            this.tree = null;
        }

        public String toString() {
            return candidate.getCandidate() + ": " + score;
        }

        @Override
        public int compareTo(TreeJob o) {
            return Double.compare(score, o.score);
        }
    }
}
