package de.unijena.bioinf.FragmentationTreeConstruction.computation;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.*;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.recalibration.HypothesenDrivenRecalibration2;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.recalibration.SpectralRecalibration;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.TreeSizeScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.ftheuristics.ExtendedCriticalPathHeuristic;
import de.unijena.bioinf.FragmentationTreeConstruction.model.*;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.jjobs.MasterJJob;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

public class TreeComputationInstance extends MasterJJob<TreeComputationInstance.FinalResult> {
    //todo should we remove subjobs if they are finished?
    //todo we proof for cancellation??
    protected final JobManager jobManager;
    protected final FragmentationPatternAnalysis analyzer;
    protected final Ms2Experiment experiment;
    protected final int numberOfResultsToKeep;
    protected ProcessedInput pinput;
    // yet another workaround =/
    // 0 = unprocessed, 1 = validated, 2 =  preprocessed, 3 = scored
    protected int state = 0;
    protected AtomicInteger ticks;
    protected volatile int nextProgress;
    protected int ticksPerProgress, progressPerTick;

    protected long startTime;
    protected int restTime, secondsPerInstance, secondsPerTree;

    public TreeComputationInstance(JobManager manager, FragmentationPatternAnalysis analyzer, Ms2Experiment input, int numberOfResultsToKeep) {
        super(JJob.JobType.CPU);
        this.jobManager = manager;
        this.analyzer = analyzer;
        this.experiment = input;
        this.numberOfResultsToKeep = numberOfResultsToKeep;
        this.ticks = new AtomicInteger(0);
    }

    @Override
    protected JobManager jobManager() {
        return jobManager;
    }

    ////////////////

    public String testHeuristics() {

        final MolecularFormula neutralFormula = experiment.getPrecursorIonType().neutralMoleculeToMeasuredNeutralMolecule(experiment.getMolecularFormula());
        final TreeSizeScorer tss = FragmentationPatternAnalysis.getByClassName(TreeSizeScorer.class, analyzer.getFragmentPeakScorers());
        experiment.setAnnotation(Whiteset.class, Whiteset.of(experiment.getMolecularFormula()));
        precompute();
        final FGraph graph = analyzer.buildGraph(pinput, pinput.getAnnotationOrThrow(DecompositionList.class).getDecompositions().get(0));

        final FTree exact = analyzer.computeTree(graph);
        final double p1 = analyzer.getIntensityRatioOfExplainedPeaksFromUnanotatedTree(pinput, exact, experiment.getPrecursorIonType().getIonization());
        final FTree heuristic = new ExtendedCriticalPathHeuristic(graph).solve();

        // how many peaks are explained?
        final double p2 = analyzer.getIntensityRatioOfExplainedPeaksFromUnanotatedTree(pinput, heuristic, experiment.getPrecursorIonType().getIonization());
        final int n1 = exact.numberOfVertices(), n2 = heuristic.numberOfVertices();
        final double[] stats1 = sharedFragments(exact, heuristic);

        // now beautify tree!
        tss.fastReplace(pinput, new TreeSizeScorer.TreeSizeBonus(tss.getTreeSizeScore() + MAX_TREESIZE_INCREASE));
        final FGraph graph2 = analyzer.buildGraph(pinput, pinput.getAnnotationOrThrow(DecompositionList.class).getDecompositions().get(0));
        final FTree exact2 = analyzer.computeTree(graph2);
        final double p3 = analyzer.getIntensityRatioOfExplainedPeaks(pinput, exact2);
        final FTree heuristic2 = new ExtendedCriticalPathHeuristic(graph2).solve();
        final double p4 = analyzer.getIntensityRatioOfExplainedPeaks(pinput, heuristic2);
        final int n3 = exact2.numberOfVertices(), n4 = heuristic2.numberOfVertices();

        // how many fragments are the same
        final double[] stats2 = sharedFragments(exact2, heuristic2);
        //
        String header = "mass\theuristic.score\texact.score\theuristic.rpeaks\texact.rpeaks\theuristic.npeaks\texact.npeaks\tjaccard\tfraction\tshared\theuristic.bscore\texact.bscore\theuristic.brpeaks\texact.brpeaks\theuristic.bnpeaks\texact.bnpeaks\tb.jaccard\tb.fraction\tb.shared";
        return String.format(Locale.US,
                "%f\t%f\t%f\t%f\t%f\t%d\t%d\t%f\t%f\t%d\t%f\t%f\t%f\t%f\t%d\t%d\t%f\t%f\t%d",

                experiment.getMolecularFormula().getMass(),
                heuristic.getTreeWeight(),
                exact.getTreeWeight(),
                p1,
                p2,
                n1,
                n2,
                stats1[0], stats1[1], (int) stats1[2],
                heuristic2.getTreeWeight(),
                exact2.getTreeWeight(),
                p3,
                p4,
                n3,
                n4,
                stats2[0], stats2[1], (int) stats2[2]

        );

    }

    // jaccard, contains, absolute number
    protected double[] sharedFragments(FTree a, FTree b) {
        final HashSet<MolecularFormula> fs = new HashSet<>();
        for (Fragment f : a) fs.add(f.getFormula());
        final HashSet<MolecularFormula> gs = new HashSet<>();
        for (Fragment f : b) gs.add(f.getFormula());
        // remove root
        fs.remove(a.getRoot().getFormula());
        gs.remove(b.getRoot().getFormula());
        double union = gs.size();
        double intersection = 0;
        for (MolecularFormula f : fs) {
            if (gs.contains(f)) {
                ++intersection;
            } else ++union;
        }

        return new double[]{intersection / union, intersection / fs.size(), intersection};

    }

    /////////////////


    public ProcessedInput validateInput() {
        if (state <= 0) {
            pinput = analyzer.performValidation(experiment);
            state = 1;
        }
        return pinput;
    }

    public ProcessedInput precompute() {
        if (state <= 1) {
            this.pinput = analyzer.preprocessInputBeforeScoring(validateInput());
            state = 2;
        }
        return pinput;
    }

    private ProcessedInput score() {
        if (state <= 2) {
            this.pinput = analyzer.performPeakScoring(precompute());
            state = 3;
        }
        return pinput;
    }

    protected void tick() {
        tick(100);
    }

    protected void tick(int max) {
        final int t = ticks.incrementAndGet();
        if (t == nextProgress) {
            final int incrementation = (t * progressPerTick) / ticksPerProgress;
            updateProgress(Math.min(incrementation, max));
            while (true) {
                int x = ticks.get();
                nextProgress = (x * progressPerTick) + ticksPerProgress;
                if (ticks.get() == x) break;
            }
        }
    }

    protected void configureProgress(int from, int to, int numberOfTicks) {
        int span = to - from;
        if (numberOfTicks > span) {
            ticksPerProgress = numberOfTicks / span;
            progressPerTick = 1;
        } else {
            ticksPerProgress = 1;
            progressPerTick = span / numberOfTicks;
        }
        ticks.set(from * ticksPerProgress);
        nextProgress = (from + 1) * ticksPerProgress;
        updateProgress(from);
    }

    @Override
    protected FinalResult compute() throws Exception {
        score();
        startTime = System.currentTimeMillis();
        final Timeout timeout = pinput.getAnnotation(Timeout.class, Timeout.NO_TIMEOUT);
        secondsPerInstance = timeout.getNumberOfSecondsPerInstance();
        secondsPerTree = timeout.getNumberOfSecondsPerDecomposition();
        restTime = Math.min(secondsPerInstance, secondsPerTree);
        // preprocess input
        TreeSizeScorer.TreeSizeBonus treeSizeBonus;
        final TreeSizeScorer tss = FragmentationPatternAnalysis.getByClassName(TreeSizeScorer.class, analyzer.getFragmentPeakScorers());
        if (tss != null) {
            treeSizeBonus = new TreeSizeScorer.TreeSizeBonus(tss.getTreeSizeScore());
            pinput.setAnnotation(TreeSizeScorer.TreeSizeBonus.class, treeSizeBonus);
        } else {
            treeSizeBonus = null;
        }
        double inc = 0d;
        final int n = pinput.getAnnotationOrThrow(DecompositionList.class).getDecompositions().size();
        // as long as we do not find good quality results
        try {
            while (true) {
                final boolean retryWithHigherScore = inc < MAX_TREESIZE_INCREASE;
                // compute heuristics
                final List<IntermediateResult> intermediateResults = new ArrayList<>();
                final DecompositionList dlist = pinput.getAnnotationOrThrow(DecompositionList.class);
                configureProgress(0, 20, n);
                if (dlist.getDecompositions().size() > 100 && numberOfResultsToKeep < dlist.getDecompositions().size()/4) {
                    final List<HeuristicJob> heuristics = new ArrayList<>();
                    for (final Decomposition formula : dlist.getDecompositions()) {
                        final HeuristicJob heuristicJob = new HeuristicJob(formula);
                        submitSubJob(heuristicJob);
                        heuristics.add(heuristicJob);
                    }
                    // collect results
                    for (HeuristicJob job : heuristics) {
                        try {
                            intermediateResults.add(job.awaitResult());
                            tick();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                            throw e;
                        }
                    }
                    // sort by score
                    Collections.sort(intermediateResults, Collections.reverseOrder());
                } else {
                    for (final Decomposition formula : dlist.getDecompositions()) {
                        intermediateResults.add(new IntermediateResult(formula, 0d));
                    }
                }
                // now compute from best scoring compound to lowest scoring compound
                final FinalResult fr;
                if (analyzer.getTreeBuilder().isThreadSafe()) {
                    fr = computeExactTreesInParallel(intermediateResults, retryWithHigherScore);
                } else {
                    fr = computeExactTreesSinglethreaded(intermediateResults, retryWithHigherScore);
                }
                if (tss != null && retryWithHigherScore && fr.canceledDueToLowScore) {
                    inc += TREE_SIZE_INCREASE;
                    System.err.println("increase tree size to " + inc);
                    treeSizeBonus = new TreeSizeScorer.TreeSizeBonus(treeSizeBonus.score + TREE_SIZE_INCREASE);
                    tss.fastReplace(pinput, treeSizeBonus);
                } else return fr;
                //
            }
        } finally {
            updateProgress(100);
        }

        //return null;
    }

    private FinalResult computeExactTreesInParallel(List<IntermediateResult> intermediateResults, boolean earlyStopping) {
        return computeExactTreesSinglethreaded(intermediateResults, earlyStopping);
    }

    private final class ExactComputationWithThreshold extends BasicJJob<ExactResult> {

        private final double[] sharedVariable;
        private final JJob<FGraph> graphJJob;
        private final IntermediateResult intermediateResult;

        public ExactComputationWithThreshold(double[] sharedVariable, JJob<FGraph> graphJJob, IntermediateResult intermediateResult) {
            this.sharedVariable = sharedVariable;
            this.graphJJob = graphJJob;
            this.intermediateResult = intermediateResult;
        }

        protected ExactResult computeExact() {
            final double threshold = sharedVariable[0];
            if (intermediateResult.heuristicScore < threshold) {
                graphJJob.cancel(true);
                return null; // early stopping
            }
            final FGraph graph = graphJJob.takeResult();
            graph.setAnnotation(Timeout.class, Timeout.newTimeout(secondsPerInstance, restTime));
            final FTree tree = analyzer.computeTreeWithoutAnnotating(graph, intermediateResult.heuristicScore - 1e-3);
            if (tree == null) return null;
            return new ExactResult(intermediateResult.candidate, graph, tree, tree.getTreeWeight());
        }

        @Override
        protected ExactResult compute() throws Exception {
            return computeExact();
        }
    }

    private FinalResult computeExactTreesSinglethreaded(List<IntermediateResult> intermediateResults, boolean earlyStopping) throws TimeoutException {
        // compute in batches
        configureProgress(20, 80, (int) Math.ceil(intermediateResults.size() * 0.2));
        final int NCPUS = jobManager().getCPUThreads();
        final int BATCH_SIZE = Math.min(4 * NCPUS, Math.max(30, NCPUS));
        final int MAX_GRAPH_CACHE_SIZE = Math.max(30, BATCH_SIZE);

        final int n = Math.min(intermediateResults.size(), numberOfResultsToKeep);
        final DoubleEndWeightedQueue2<ExactResult> queue = new DoubleEndWeightedQueue2<>(Math.max(20, n + 10), new ExactResultComparator());
        final DoubleEndWeightedQueue2<ExactResult> graphCache;
        // store at maximum 30 graphs
        if (queue.capacity > MAX_GRAPH_CACHE_SIZE) {
            graphCache = new DoubleEndWeightedQueue2<>(MAX_GRAPH_CACHE_SIZE, new ExactResultComparator());
        } else {
            graphCache = null;
        }

        final double[] threshold = new double[]{Double.NEGATIVE_INFINITY, 0d};

        final boolean IS_SINGLETHREADED = intermediateResults.size() < 200 && !analyzer.getTreeBuilder().isThreadSafe();

        final List<ExactComputationWithThreshold> batchJobs = new ArrayList<>(BATCH_SIZE);
        int treesComputed = 0;
        outerLoop:
        for (int i = 0; i < intermediateResults.size(); i += BATCH_SIZE) {
            checkTimeout();
            final List<IntermediateResult> batch = intermediateResults.subList(i, Math.min(intermediateResults.size(), i + BATCH_SIZE));
            if (batch.isEmpty()) break outerLoop;
            if (batch.get(0).heuristicScore < threshold[0]) {
                break outerLoop;
            }
            final List<GraphBuildingJob> graphs = computeGraphBatches(batch);
            batchJobs.clear();
            for (int j = 0; j < graphs.size(); ++j) {

                final GraphBuildingJob graphBuildingJob = graphs.get(j);
                final IntermediateResult intermediateResult = batch.get(j);

                if (intermediateResult.heuristicScore >= threshold[0]) {
                    final ExactComputationWithThreshold exactJob = new ExactComputationWithThreshold(threshold, graphBuildingJob, intermediateResult);
                    if (IS_SINGLETHREADED) {
                        final ExactResult ex = exactJob.computeExact();
                        tick();
                        if (ex != null) {
                            ++treesComputed;
                            putIntQueue(ex, intermediateResult, queue, graphCache, threshold);
                        }
                    } else {
                        batchJobs.add(exactJob);
                    }
                }
            }

            if (!IS_SINGLETHREADED) {
                for (int JJ = batchJobs.size() - 1; JJ >= 0; --JJ)
                    submitSubJob(batchJobs.get(JJ));
                for (ExactComputationWithThreshold job : batchJobs) {
                    final ExactResult r = job.takeResult();
                    tick();
                    if (r != null) {
                        ++treesComputed;
                        putIntQueue(r, job.intermediateResult, queue, graphCache, threshold);
                    }
                }
            }
        }
        progressInfo("Computed " + treesComputed + " / " + intermediateResults.size() + " trees with maximum gap is " + threshold[1]);

        if (graphCache != null) {
            for (ExactResult r : graphCache) {
                queue.replace(r, r.score);
            }
            graphCache.clear();
        }
        configureProgress(80, 99, numberOfResultsToKeep);
        boolean CHECK_FOR_TREESIZE = earlyStopping;
        final ArrayList<ExactResult> exactResults = new ArrayList<>();
        for (ExactResult r : queue) {
            exactResults.add(new ExactResult(r.decomposition, r.graph, r.tree, r.score));
            if (CHECK_FOR_TREESIZE && exactResults.size() >= MIN_NUMBER_OF_TREES_CHECK_FOR_INTENSITY) {
                if (!checkForTreeQuality(exactResults)) return new FinalResult();
                CHECK_FOR_TREESIZE = false;
            }
        }
        if (CHECK_FOR_TREESIZE && exactResults.size() < MIN_NUMBER_OF_TREES_CHECK_FOR_INTENSITY) {
            if (!checkForTreeQuality(exactResults)) return new FinalResult();
            CHECK_FOR_TREESIZE = false;
        }
        // now recalibrate trees
        if (pinput.getAnnotation(ForbidRecalibration.class, ForbidRecalibration.ALLOWED).isAllowed()) {
            double maxRecalibrationBonus = Double.POSITIVE_INFINITY;

            final ArrayList<RecalibrationJob> recalibrationJobs = new ArrayList<>();
            checkTimeout();
            for (int i = 0, nn = Math.min(exactResults.size(), numberOfResultsToKeep); i < nn; ++i) {
                ExactResult r = exactResults.get(i);
                final RecalibrationJob rj = new RecalibrationJob(r);
                recalibrationJobs.add(rj);
                submitSubJob(rj);
            }
            for (int i = 0, nn = recalibrationJobs.size(); i < nn; ++i) {
                ExactResult recalibratedResult = recalibrationJobs.get(i).takeResult();
                final FTree recalibrated = recalibratedResult.tree;

                final TreeScoring sc = recalibrated.getAnnotationOrThrow(TreeScoring.class);

                final double recalibrationBonus = sc.getRecalibrationBonus();
                double recalibrationPenalty = 0d;
                if (i <= 10) {
                    maxRecalibrationBonus = Math.min(recalibrated.getTreeWeight(), maxRecalibrationBonus);
                } else {
                    recalibrationPenalty = Math.min(recalibrationBonus, Math.max(0, recalibrated.getTreeWeight() - maxRecalibrationBonus));
                }
                sc.setRecalibrationPenalty(recalibrationPenalty);
                sc.setOverallScore(sc.getOverallScore() - sc.getRecalibrationPenalty());
                recalibrated.setTreeWeight(recalibrated.getTreeWeight() - recalibrationPenalty);
                exactResults.set(i, new ExactResult(recalibratedResult.decomposition, null, recalibrated, recalibrated.getTreeWeight()));
                tick();
            }
            Collections.sort(exactResults, Collections.reverseOrder());
        } else {
            Collections.sort(exactResults, Collections.reverseOrder());
            for (int i=0; i < Math.min(numberOfResultsToKeep, exactResults.size()); ++i) {
                analyzer.addTreeAnnotations(exactResults.get(i).graph,exactResults.get(i).tree);
            }
        }
        Collections.sort(exactResults, Collections.reverseOrder());
        final int nl = Math.min(numberOfResultsToKeep, exactResults.size());
        final ArrayList<FTree> finalResults = new ArrayList<>(nl);
        for (int m = 0; m < nl; ++m) {
            final double score = analyzer.recalculateScores(exactResults.get(m).tree);
            if (Math.abs(score - exactResults.get(m).tree.getTreeWeight()) > 0.1) {
                LoggerFactory.getLogger(TreeComputationInstance.class).warn("Score of " + exactResults.get(m).decomposition.toString() + " differs significantly from recalculated score: " + score + " vs " + exactResults.get(m).tree.getTreeWeight() + " with tree size is " + exactResults.get(m).tree.getFragmentAnnotationOrThrow(Score.class).get(exactResults.get(m).tree.getFragmentAt(1)).get("TreeSizeScorer") + " and " + exactResults.get(m).tree.getAnnotationOrThrow(ProcessedInput.class).getAnnotationOrThrow(TreeSizeScorer.TreeSizeBonus.class).score + " sort key is score " + exactResults.get(m).score);
                analyzer.recalculateScores(exactResults.get(m).tree);
            } else if (Math.abs(score - exactResults.get(m).tree.getAnnotationOrThrow(TreeScoring.class).getOverallScore()) > 0.1) {
                LoggerFactory.getLogger(TreeComputationInstance.class).warn("Score of tree " + exactResults.get(m).decomposition.toString() + " differs significantly from recalculated score: " + score + " vs " + exactResults.get(m).tree.getAnnotationOrThrow(TreeScoring.class).getOverallScore()  + " with tree size is " + exactResults.get(m).tree.getFragmentAnnotationOrThrow(Score.class).get(exactResults.get(m).tree.getFragmentAt(1) ).get("TreeSizeScorer") + " and " + exactResults.get(m).tree.getAnnotationOrThrow(ProcessedInput.class).getAnnotationOrThrow(TreeSizeScorer.TreeSizeBonus.class).score + " sort key is score " + exactResults.get(m).score);
                analyzer.recalculateScores(exactResults.get(m).tree);
            }
            finalResults.add(exactResults.get(m).tree);
        }


        return new FinalResult(finalResults);
    }

    private void checkTimeout() {
        final long time = System.currentTimeMillis();
        final int elapsedTime = (int)((time-startTime)/1000);
        restTime = Math.min(restTime,secondsPerInstance-elapsedTime);
        if (restTime <= 0) throw new TimeoutException();
    }

    private class RecalibrationJob extends BasicJJob<ExactResult> {
        private final ExactResult r;

        public RecalibrationJob(ExactResult input) {
            this.r = input;
        }

        @Override
        protected ExactResult compute() throws Exception {
            FGraph graph;
            if (r.graph == null) {
                graph = analyzer.buildGraph(pinput, r.decomposition);
            } else graph = r.graph;
            if (r.tree.getAnnotationOrNull(ProcessedInput.class) == null)
                analyzer.addTreeAnnotations(graph, r.tree);
            final FTree tree = r.tree;
            ExactResult recalibratedResult = recalibrate(pinput, tree);
            final FTree recalibrated;
            final double recalibrationBonus = recalibratedResult.tree.getTreeWeight() - tree.getTreeWeight();
            if (recalibrationBonus <= 0) {
                recalibrated = tree;
            } else {
                recalibrated = recalibratedResult.tree;
                final TreeScoring sc = recalibrated.getAnnotationOrThrow(TreeScoring.class);
                sc.setRecalibrationBonus(recalibrationBonus);
            }
            return new ExactResult(r.decomposition, null, recalibrated, recalibrated.getTreeWeight());
        }
    }

    private void putIntQueue(ExactResult r, IntermediateResult intermediateResult, DoubleEndWeightedQueue2<ExactResult> queue, DoubleEndWeightedQueue2<ExactResult> graphCache, double[] threshold) {
        threshold[1] = Math.max(threshold[1], r.score - intermediateResult.heuristicScore);
        final FGraph graph = r.graph;
        if (graphCache != null) {
            graphCache.add(r, r.score);
            r = new ExactResult(r.decomposition, null, r.tree, r.score);
        }
        if (!queue.add(r, r.score)) {
            // we have computed enough trees. Let's calculate lowerbound threshold
            threshold[0] = queue.lowerbound - threshold[1];
        } else if (graphCache != null) {
            // we have to annotate the tree
            analyzer.addTreeAnnotations(graph, r.tree);
        }
    }

    private static final double MAX_TREESIZE_INCREASE = 0;//3d;
    private static final double TREE_SIZE_INCREASE = 1d;
    private static final int MIN_NUMBER_OF_EXPLAINED_PEAKS = 0;//15;
    private static final double MIN_EXPLAINED_INTENSITY = 0d;//0.7d;
    private static final int MIN_NUMBER_OF_TREES_CHECK_FOR_INTENSITY = 5;

    private boolean checkForTreeQuality(List<ExactResult> results) {
        for (ExactResult r : results) {
            final FTree tree = r.tree;
            if (analyzer.getIntensityRatioOfExplainedPeaksFromUnanotatedTree(pinput, tree, r.decomposition.getIon()) >= MIN_EXPLAINED_INTENSITY && tree.numberOfVertices() >= Math.min(pinput.getMergedPeaks().size() - 2, MIN_NUMBER_OF_EXPLAINED_PEAKS)) {
                return true;
            }
        }
        return false;
    }

    /*

    - jeder Kandidat besteht aus Molekülformel + Ionisierung (nicht PrecursorIonType!)
    - erstmal berechnen wir alle Bäume heuristisch und ranken die Candidaten nach Score.
    - danach berechnen wir exakte Lösungen für die ersten K Bäume und bestimmen einen Threshold
    - danach berechnen wir Schrittweise neue Bäume und passen den Threshold jedes Mal an bis wir abbrechen können
    - danach rekalibrieren wir das resultierende Set und sortieren neu


     */

    private List<GraphBuildingJob> computeGraphBatches(List<IntermediateResult> results) {
        final List<GraphBuildingJob> graphs = new ArrayList<>(results.size());
        for (IntermediateResult r : results) {
            GraphBuildingJob job = new GraphBuildingJob(r.candidate);
            graphs.add(job);
            submitSubJob(job);
        }
        return graphs;
    }


    // 1. Multithreaded: Berechne ProcessedInput für alle Ionisierungen
    // 2. Multithreaded: Berechne Graphen für alle Ionisierungen, berechne Bäume via Heuristik
    // 3. evtl. Multithreaded: Berechne exakte Lösung für jeden Baum
    // 4. Breche ab, wenn ausreichend gute exakte Lösungen gefunden wurden

    protected class HeuristicJob extends BasicJJob<IntermediateResult> {

        protected Decomposition decomposition;

        protected HeuristicJob(Decomposition formula) {
            super(JobType.CPU);
            this.decomposition = formula;
        }


        @Override
        protected IntermediateResult compute() throws Exception {
            final FGraph graph = analyzer.buildGraph(pinput, decomposition);
            // compute heuristic

            //final FTree heuristic = new CriticalPathSolver(graph).solve();
            final FTree heuristic = new ExtendedCriticalPathHeuristic(graph).solve();


            IntermediateResult result = new IntermediateResult(decomposition, heuristic.getTreeWeight());
            return result;
        }
    }

    protected class GraphBuildingJob extends BasicJJob<FGraph> {
        private final Decomposition decomposition;

        public GraphBuildingJob(Decomposition decomposition) {
            super(JobType.CPU);
            this.decomposition = decomposition;
        }

        @Override
        protected FGraph compute() throws Exception {
            return analyzer.buildGraph(pinput, decomposition);
        }
    }

    protected ExactResult recalibrate(ProcessedInput input, FTree tree) {
        final SpectralRecalibration rec = new HypothesenDrivenRecalibration2().collectPeaksFromMs2(input.getExperimentInformation(), tree);
        final ProcessedInput pin = input.getRecalibratedVersion(rec);
        // we have to completely rescore the input...
        final DecompositionList l = new DecompositionList(Arrays.asList(pin.getAnnotationOrThrow(DecompositionList.class).find(tree.getRoot().getFormula())));
        pin.setAnnotation(DecompositionList.class, l);
        analyzer.performPeakScoring(pin);
        FGraph graph = analyzer.buildGraph(pin, l.getDecompositions().get(0));
        graph.addAnnotation(SpectralRecalibration.class, rec);
        graph.setAnnotation(ProcessedInput.class, pin);
        final FTree recalibratedTree = analyzer.computeTree(graph);
        //System.out.println("Recalibrate " + tree.getRoot().getFormula() + " => " + rec.getRecalibrationFunction() + "  ( " + (recalibratedTree.getTreeWeight() - tree.getTreeWeight()) + ")");
        recalibratedTree.setAnnotation(SpectralRecalibration.class, rec);
        recalibratedTree.setAnnotation(ProcessedInput.class, pin);
        return new ExactResult(l.getDecompositions().get(0), graph, recalibratedTree, recalibratedTree.getTreeWeight());
    }

    protected final static class IntermediateResult implements Comparable<IntermediateResult> {

        protected final Decomposition candidate;
        protected double heuristicScore;

        public IntermediateResult(Decomposition formula, double heuristicScore) {
            this.candidate = formula;
            this.heuristicScore = heuristicScore;

        }

        public String toString() {
            return candidate.getCandidate() + ": " + heuristicScore;
        }

        @Override
        public int compareTo(IntermediateResult o) {
            return Double.compare(heuristicScore, o.heuristicScore);
        }
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

    protected final static class ExactResult implements Comparable<ExactResult> {

        protected final Decomposition decomposition;
        protected final double score;
        protected final FGraph graph;
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

    protected static class ExactResultComparator implements Comparator<ExactResult> {

        @Override
        public int compare(ExactResult o1, ExactResult o2) {
            return o1.decomposition.getCandidate().compareTo(o2.decomposition.getCandidate());
        }
    }
}
