package de.unijena.bioinf.FragmentationTreeConstruction.computation;

public class FasterTreeComputationInstance {//extends MasterJJob<FasterTreeComputationInstance.FinalResult> {
//
//    protected final JobManager jobManager;
//    protected final FragmentationPatternAnalysis analyzer;
//    protected final Ms2Experiment experiment;
//    protected final int numberOfResultsToKeep;
//    protected ProcessedInput pinput;
//    // yet another workaround =/
//    // 0 = unprocessed, 1 = validated, 2 =  preprocessed, 3 = scored
//    protected int state = 0;
//    protected AtomicInteger ticks;
//    protected volatile int nextProgress;
//    protected int ticksPerProgress, progressPerTick;
//
//    protected long startTime;
//    protected int restTime, secondsPerInstance, secondsPerTree;
//
//    public FasterTreeComputationInstance(JobManager manager, FragmentationPatternAnalysis analyzer, Ms2Experiment input, int numberOfResultsToKeep) {
//        super(JobType.CPU);
//        this.jobManager = manager;
//        this.analyzer = analyzer;
//        this.experiment = input;
//        this.numberOfResultsToKeep = numberOfResultsToKeep;
//        this.ticks = new AtomicInteger(0);
//    }
//
//    @Override
//    protected JobManager jobManager() {
//        return jobManager;
//    }
//
//
//    /////////////////
//
//
//    public ProcessedInput validateInput() {
//        if (state <= 0) {
//            pinput = analyzer.performValidation(experiment);
//            state = 1;
//        }
//        return pinput;
//    }
//
//    public ProcessedInput precompute() {
//        if (state <= 1) {
//            this.pinput = analyzer.preprocessInputBeforeScoring(validateInput());
//            state = 2;
//        }
//        return pinput;
//    }
//
//    private ProcessedInput score() {
//        if (state <= 2) {
//            this.pinput = analyzer.performPeakScoring(precompute());
//            state = 3;
//        }
//        return pinput;
//    }
//
//    protected void tick() {
//        tick(100);
//    }
//
//    protected void tick(int max) {
//        final int t = ticks.incrementAndGet();
//        if (t == nextProgress) {
//            final int incrementation = (t * progressPerTick) / ticksPerProgress;
//            updateProgress(Math.min(incrementation, max));
//            while (true) {
//                int x = ticks.get();
//                nextProgress = (x * progressPerTick) + ticksPerProgress;
//                if (ticks.get() == x) break;
//            }
//        }
//    }
//
//    protected void configureProgress(int from, int to, int numberOfTicks) {
//        int span = to - from;
//        if (numberOfTicks > span) {
//            ticksPerProgress = numberOfTicks / span;
//            progressPerTick = 1;
//        } else {
//            ticksPerProgress = 1;
//            progressPerTick = span / numberOfTicks;
//        }
//        ticks.set(from * ticksPerProgress);
//        nextProgress = (from + 1) * ticksPerProgress;
//        updateProgress(from);
//    }
//
//    @Override
//    protected FinalResult compute() throws Exception {
//        score();
//        startTime = System.currentTimeMillis();
//        final Timeout timeout = pinput.getAnnotation(Timeout.class, Timeout.NO_TIMEOUT);
//        secondsPerInstance = timeout.getNumberOfSecondsPerInstance();
//        secondsPerTree = timeout.getNumberOfSecondsPerDecomposition();
//        restTime = Math.min(secondsPerInstance, secondsPerTree);
//        // preprocess input
//
//        final int n = pinput.getAnnotationOrThrow(DecompositionList.class).getDecompositions().size();
//        // as long as we do not find good quality results
//        final boolean useHeuristic = pinput.getParentPeak().getMz() > 400;
//
//
//
//    }
//
//    public void estimateTreeSizeAndRecalibration(List<Decomposition> decompositions, boolean useHeuristic) {
//        final int NCPUS = jobManager().getCPUThreads();
//        final int BATCH_SIZE = Math.min(4 * NCPUS, Math.max(30, NCPUS));
//        final int MAX_GRAPH_CACHE_SIZE = Math.max(30, BATCH_SIZE);
//        final int n = Math.min(decompositions.size(), numberOfResultsToKeep);
//
//        TreeSizeScorer.TreeSizeBonus treeSizeBonus;
//        final TreeSizeScorer tss = FragmentationPatternAnalysis.getByClassName(TreeSizeScorer.class, analyzer.getFragmentPeakScorers());
//        if (tss != null) {
//            treeSizeBonus = new TreeSizeScorer.TreeSizeBonus(tss.getTreeSizeScore());
//            pinput.setAnnotation(TreeSizeScorer.TreeSizeBonus.class, treeSizeBonus);
//        } else {
//            treeSizeBonus = null;
//        }
//        double inc = 0d;
//        final List<ExactResult> results = new ArrayList<>(decompositions.size());
//        // TREE SIZE
//        while (inc < MAX_TREESIZE_INCREASE) {
//            results.clear();
//            final List<TreeComputationJob> jobs = new ArrayList<>(decompositions.size());
//            final TreeBuilder builder = useHeuristic ? new ExtendedCriticalPathHeuristicTreeBuilder() : analyzer.getTreeBuilder();
//            for (Decomposition d : decompositions) {
//                final TreeComputationJob job = new TreeComputationJob(builder,null,d);
//                submitSubJob(job);
//                jobs.add(job);
//            }
//            for (TreeComputationJob job : jobs) {
//                results.add(job.takeResult());
//            }
//            Collections.sort(results, Collections.reverseOrder());
//            final int treeSizeCheck = Math.min(results.size(),MIN_NUMBER_OF_TREES_CHECK_FOR_INTENSITY);
//            if (checkForTreeQuality(results.subList(0,treeSizeCheck))) {
//                break;
//            }
//        }
//        final List<ExactResult> recalibrated = new ArrayList<>();
//        // the first 10 results might change due to recalibration
//        for (int i=0; i < Math.min(n, 10); ++i) {
//            final FTree tree = results.get(i).tree;
//            final RecalibrationJob
//
//        }
//
//
//
//
//
//
//
//        final DoubleEndWeightedQueue2<ExactResult> graphCache =  new DoubleEndWeightedQueue2<>(Math.min(n, MAX_GRAPH_CACHE_SIZE), new ExactResultComparator());
//        graphCache.setCallback(new TObjectProcedure<ExactResult>() {
//            @Override
//            public boolean execute(ExactResult object) {
//                object.graph = null;
//                return true;
//            }
//        });
//
//    }
//
//    protected class TreeComputationJob extends BasicJJob<ExactResult> {
//
//        protected final TreeBuilder treeBuilder;
//        protected final DoubleEndWeightedQueue2<ExactResult> graphCache;
//        protected final Decomposition decomposition;
//
//        public TreeComputationJob(TreeBuilder treeBuilder, DoubleEndWeightedQueue2<ExactResult> graphCache, Decomposition decomposition) {
//            this.treeBuilder = treeBuilder;
//            this.graphCache = graphCache;
//            this.decomposition = decomposition;
//        }
//
//        @Override
//        protected ExactResult compute() throws Exception {
//            final FGraph graph = analyzer.buildGraphWithoutReduction(pinput, decomposition);
//            final FTree tree = treeBuilder.computeTree().withTimeLimit(secondsPerTree).solve(pinput, graph).tree;
//            final ExactResult er = new ExactResult(decomposition,null,tree,tree.getTreeWeight());
//            if (graphCache!=null) {
//                double score = graphCache.getWeightLowerbound();
//                if (tree.getTreeWeight()>score) {
//                    synchronized (graphCache) {
//                        if (tree.getTreeWeight()>graphCache.getWeightLowerbound()) {
//                            er.graph = graph;
//                            if (!graphCache.add(er,tree.getTreeWeight()))
//                                er.graph=null;
//                        }
//                    }
//                }
//            }
//            return er;
//        }
//    }
//
//
//
//    private FinalResult computeExactTreesInParallel(List<IntermediateResult> intermediateResults, boolean earlyStopping) {
//        return computeExactTreesSinglethreaded(intermediateResults, earlyStopping);
//    }
//
//    private final class ExactComputationWithThreshold extends BasicJJob<ExactResult> {
//
//        private final double[] sharedVariable;
//        private final JJob<FGraph> graphJJob;
//        private final IntermediateResult intermediateResult;
//
//        public ExactComputationWithThreshold(double[] sharedVariable, JJob<FGraph> graphJJob, IntermediateResult intermediateResult) {
//            this.sharedVariable = sharedVariable;
//            this.graphJJob = graphJJob;
//            this.intermediateResult = intermediateResult;
//        }
//
//        protected ExactResult computeExact() {
//            final double threshold = sharedVariable[0];
//            if (intermediateResult.heuristicScore < threshold) {
//                graphJJob.cancel(true);
//                return null; // early stopping
//            }
//            final FGraph graph = graphJJob.takeResult();
//            graph.setAnnotation(Timeout.class, Timeout.newTimeout(secondsPerInstance, restTime));
//            final FTree tree = analyzer.computeTreeWithoutAnnotating(graph, intermediateResult.heuristicScore - 1e-3);
//            if (tree == null) return null;
//            return new ExactResult(intermediateResult.candidate, graph, tree, tree.getTreeWeight());
//        }
//
//        @Override
//        protected ExactResult compute() throws Exception {
//            return computeExact();
//        }
//    }
//
//    private FinalResult computeExactTreesSinglethreaded(List<IntermediateResult> intermediateResults, boolean earlyStopping) throws TimeoutException {
//        // compute in batches
//        configureProgress(20, 80, (int) Math.ceil(intermediateResults.size() * 0.2));
//        final int NCPUS = jobManager().getCPUThreads();
//        final int BATCH_SIZE = Math.min(4 * NCPUS, Math.max(30, NCPUS));
//        final int MAX_GRAPH_CACHE_SIZE = Math.max(30, BATCH_SIZE);
//
//        final int n = Math.min(intermediateResults.size(), numberOfResultsToKeep);
//        final DoubleEndWeightedQueue2<ExactResult> queue = new DoubleEndWeightedQueue2<>(Math.max(20, n + 10), new ExactResultComparator());
//        final DoubleEndWeightedQueue2<ExactResult> graphCache;
//        // store at maximum 30 graphs
//        if (queue.capacity > MAX_GRAPH_CACHE_SIZE) {
//            graphCache = new DoubleEndWeightedQueue2<>(MAX_GRAPH_CACHE_SIZE, new ExactResultComparator());
//        } else {
//            graphCache = null;
//        }
//
//        final double[] threshold = new double[]{Double.NEGATIVE_INFINITY, 0d};
//
//        final boolean IS_SINGLETHREADED = intermediateResults.size() < 200 && !analyzer.getTreeBuilder().isThreadSafe();
//
//        final List<ExactComputationWithThreshold> batchJobs = new ArrayList<>(BATCH_SIZE);
//        int treesComputed = 0;
//        outerLoop:
//        for (int i = 0; i < intermediateResults.size(); i += BATCH_SIZE) {
//            checkTimeout();
//            final List<IntermediateResult> batch = intermediateResults.subList(i, Math.min(intermediateResults.size(), i + BATCH_SIZE));
//            if (batch.isEmpty()) break outerLoop;
//            if (batch.get(0).heuristicScore < threshold[0]) {
//                break outerLoop;
//            }
//            final List<GraphBuildingJob> graphs = computeGraphBatches(batch);
//            batchJobs.clear();
//            for (int j = 0; j < graphs.size(); ++j) {
//
//                final GraphBuildingJob graphBuildingJob = graphs.get(j);
//                final IntermediateResult intermediateResult = batch.get(j);
//
//                if (intermediateResult.heuristicScore >= threshold[0]) {
//                    final ExactComputationWithThreshold exactJob = new ExactComputationWithThreshold(threshold, graphBuildingJob, intermediateResult);
//                    if (IS_SINGLETHREADED) {
//                        final ExactResult ex = exactJob.computeExact();
//                        tick();
//                        if (ex != null) {
//                            ++treesComputed;
//                            putIntQueue(ex, intermediateResult, queue, graphCache, threshold);
//                        }
//                    } else {
//                        batchJobs.add(exactJob);
//                    }
//                }
//            }
//
//            if (!IS_SINGLETHREADED) {
//                for (int JJ = batchJobs.size() - 1; JJ >= 0; --JJ)
//                    submitSubJob(batchJobs.get(JJ));
//                for (ExactComputationWithThreshold job : batchJobs) {
//                    final ExactResult r = job.takeResult();
//                    tick();
//                    if (r != null) {
//                        ++treesComputed;
//                        putIntQueue(r, job.intermediateResult, queue, graphCache, threshold);
//                    }
//                }
//            }
//        }
//        progressInfo("Computed " + treesComputed + " / " + intermediateResults.size() + " trees with maximum gap is " + threshold[1]);
//
//        if (graphCache != null) {
//            for (ExactResult r : graphCache) {
//                queue.replace(r, r.score);
//            }
//            graphCache.clear();
//        }
//        configureProgress(80, 99, numberOfResultsToKeep);
//        boolean CHECK_FOR_TREESIZE = earlyStopping;
//        final ArrayList<ExactResult> exactResults = new ArrayList<>();
//        for (ExactResult r : queue) {
//            exactResults.add(new ExactResult(r.decomposition, r.graph, r.tree, r.score));
//            if (CHECK_FOR_TREESIZE && exactResults.size() >= MIN_NUMBER_OF_TREES_CHECK_FOR_INTENSITY) {
//                if (!checkForTreeQuality(exactResults)) return new FinalResult();
//                CHECK_FOR_TREESIZE = false;
//            }
//        }
//        if (CHECK_FOR_TREESIZE && exactResults.size() < MIN_NUMBER_OF_TREES_CHECK_FOR_INTENSITY) {
//            if (!checkForTreeQuality(exactResults)) return new FinalResult();
//            CHECK_FOR_TREESIZE = false;
//        }
//        // now recalibrate trees
//        if (pinput.getAnnotation(ForbidRecalibration.class, ForbidRecalibration.ALLOWED).isAllowed()) {
//            double maxRecalibrationBonus = Double.POSITIVE_INFINITY;
//
//            final ArrayList<RecalibrationJob> recalibrationJobs = new ArrayList<>();
//            checkTimeout();
//            for (int i = 0, nn = Math.min(exactResults.size(), numberOfResultsToKeep); i < nn; ++i) {
//                ExactResult r = exactResults.get(i);
//                final RecalibrationJob rj = new RecalibrationJob(r);
//                recalibrationJobs.add(rj);
//                submitSubJob(rj);
//            }
//            for (int i = 0, nn = recalibrationJobs.size(); i < nn; ++i) {
//                ExactResult recalibratedResult = recalibrationJobs.get(i).takeResult();
//                final FTree recalibrated = recalibratedResult.tree;
//
//                final TreeScoring sc = recalibrated.getAnnotationOrThrow(TreeScoring.class);
//
//                final double recalibrationBonus = sc.getRecalibrationBonus();
//                double recalibrationPenalty = 0d;
//                if (i <= 10) {
//                    maxRecalibrationBonus = Math.min(recalibrated.getTreeWeight(), maxRecalibrationBonus);
//                } else {
//                    recalibrationPenalty = Math.min(recalibrationBonus, Math.max(0, recalibrated.getTreeWeight() - maxRecalibrationBonus));
//                }
//                sc.setRecalibrationPenalty(recalibrationPenalty);
//                sc.setOverallScore(sc.getOverallScore() - sc.getRecalibrationPenalty());
//                recalibrated.setTreeWeight(recalibrated.getTreeWeight() - recalibrationPenalty);
//                exactResults.set(i, new ExactResult(recalibratedResult.decomposition, null, recalibrated, recalibrated.getTreeWeight()));
//                tick();
//            }
//            Collections.sort(exactResults, Collections.reverseOrder());
//        } else {
//            Collections.sort(exactResults, Collections.reverseOrder());
//            for (int i=0; i < Math.min(numberOfResultsToKeep, exactResults.size()); ++i) {
//                analyzer.addTreeAnnotations(exactResults.get(i).graph,exactResults.get(i).tree);
//            }
//        }
//        Collections.sort(exactResults, Collections.reverseOrder());
//        final int nl = Math.min(numberOfResultsToKeep, exactResults.size());
//        final ArrayList<FTree> finalResults = new ArrayList<>(nl);
//        for (int m = 0; m < nl; ++m) {
//            final double score = analyzer.recalculateScores(exactResults.get(m).tree);
//            if (Math.abs(score - exactResults.get(m).tree.getTreeWeight()) > 0.1) {
//                LoggerFactory.getLogger(FasterTreeComputationInstance.class).warn("Score of " + exactResults.get(m).decomposition.toString() + " differs significantly from recalculated score: " + score + " vs " + exactResults.get(m).tree.getTreeWeight() + " with tree size is " + exactResults.get(m).tree.getFragmentAnnotationOrThrow(Score.class).get(exactResults.get(m).tree.getFragmentAt(1)).get("TreeSizeScorer") + " and " + exactResults.get(m).tree.getAnnotationOrThrow(ProcessedInput.class).getAnnotationOrThrow(TreeSizeScorer.TreeSizeBonus.class).score + " sort key is score " + exactResults.get(m).score);
//                analyzer.recalculateScores(exactResults.get(m).tree);
//            } else if (Math.abs(score - exactResults.get(m).tree.getAnnotationOrThrow(TreeScoring.class).getOverallScore()) > 0.1) {
//                LoggerFactory.getLogger(FasterTreeComputationInstance.class).warn("Score of tree " + exactResults.get(m).decomposition.toString() + " differs significantly from recalculated score: " + score + " vs " + exactResults.get(m).tree.getAnnotationOrThrow(TreeScoring.class).getOverallScore()  + " with tree size is " + exactResults.get(m).tree.getFragmentAnnotationOrThrow(Score.class).get(exactResults.get(m).tree.getFragmentAt(1) ).get("TreeSizeScorer") + " and " + exactResults.get(m).tree.getAnnotationOrThrow(ProcessedInput.class).getAnnotationOrThrow(TreeSizeScorer.TreeSizeBonus.class).score + " sort key is score " + exactResults.get(m).score);
//                analyzer.recalculateScores(exactResults.get(m).tree);
//            }
//            finalResults.add(exactResults.get(m).tree);
//        }
//
//
//        return new FinalResult(finalResults);
//    }
//
//    private void checkTimeout() {
//        final long time = System.currentTimeMillis();
//        final int elapsedTime = (int)((time-startTime)/1000);
//        restTime = Math.min(restTime,secondsPerInstance-elapsedTime);
//        if (restTime <= 0) throw new TimeoutException();
//    }
//
//    private class RecalibrationJob extends BasicJJob<ExactResult> {
//        private final ExactResult r;
//
//        public RecalibrationJob(ExactResult input) {
//            this.r = input;
//        }
//
//        @Override
//        protected ExactResult compute() throws Exception {
//            FGraph graph;
//            if (r.graph == null) {
//                graph = analyzer.buildGraph(pinput, r.decomposition);
//            } else graph = r.graph;
//            if (r.tree.getAnnotationOrNull(ProcessedInput.class) == null)
//                analyzer.addTreeAnnotations(graph, r.tree);
//            final FTree tree = r.tree;
//            ExactResult recalibratedResult = recalibrate(pinput, tree);
//            final FTree recalibrated;
//            final double recalibrationBonus = recalibratedResult.tree.getTreeWeight() - tree.getTreeWeight();
//            if (recalibrationBonus <= 0) {
//                recalibrated = tree;
//            } else {
//                recalibrated = recalibratedResult.tree;
//                final TreeScoring sc = recalibrated.getAnnotationOrThrow(TreeScoring.class);
//                sc.setRecalibrationBonus(recalibrationBonus);
//            }
//            return new ExactResult(r.decomposition, null, recalibrated, recalibrated.getTreeWeight());
//        }
//    }
//
//    private void putIntQueue(ExactResult r, IntermediateResult intermediateResult, DoubleEndWeightedQueue2<ExactResult> queue, DoubleEndWeightedQueue2<ExactResult> graphCache, double[] threshold) {
//        threshold[1] = Math.max(threshold[1], r.score - intermediateResult.heuristicScore);
//        final FGraph graph = r.graph;
//        if (graphCache != null) {
//            graphCache.add(r, r.score);
//            r = new ExactResult(r.decomposition, null, r.tree, r.score);
//        }
//        if (!queue.add(r, r.score)) {
//            // we have computed enough trees. Let's calculate lowerbound threshold
//            threshold[0] = queue.lowerbound - threshold[1];
//        } else if (graphCache != null) {
//            // we have to annotate the tree
//            analyzer.addTreeAnnotations(graph, r.tree);
//        }
//    }
//
//    private static final double MAX_TREESIZE_INCREASE = 0;//3d;
//    private static final double TREE_SIZE_INCREASE = 1d;
//    private static final int MIN_NUMBER_OF_EXPLAINED_PEAKS = 0;//15;
//    private static final double MIN_EXPLAINED_INTENSITY = 0d;//0.7d;
//    private static final int MIN_NUMBER_OF_TREES_CHECK_FOR_INTENSITY = 5;
//
//    private boolean checkForTreeQuality(List<ExactResult> results) {
//        for (ExactResult r : results) {
//            final FTree tree = r.tree;
//            if (analyzer.getIntensityRatioOfExplainedPeaksFromUnanotatedTree(pinput, tree, r.decomposition.getIon()) >= MIN_EXPLAINED_INTENSITY && tree.numberOfVertices() >= Math.min(pinput.getMergedPeaks().size() - 2, MIN_NUMBER_OF_EXPLAINED_PEAKS)) {
//                return true;
//            }
//        }
//        return false;
//    }
//
//    /*
//
//    - jeder Kandidat besteht aus Molekülformel + Ionisierung (nicht PrecursorIonType!)
//    - erstmal berechnen wir alle Bäume heuristisch und ranken die Candidaten nach Score.
//    - danach berechnen wir exakte Lösungen für die ersten K Bäume und bestimmen einen Threshold
//    - danach berechnen wir Schrittweise neue Bäume und passen den Threshold jedes Mal an bis wir abbrechen können
//    - danach rekalibrieren wir das resultierende Set und sortieren neu
//
//
//     */
//
//    private List<GraphBuildingJob> computeGraphBatches(List<IntermediateResult> results) {
//        final List<GraphBuildingJob> graphs = new ArrayList<>(results.size());
//        for (IntermediateResult r : results) {
//            GraphBuildingJob job = new GraphBuildingJob(r.candidate);
//            graphs.add(job);
//            submitSubJob(job);
//        }
//        return graphs;
//    }
//
//
//    // 1. Multithreaded: Berechne ProcessedInput für alle Ionisierungen
//    // 2. Multithreaded: Berechne Graphen für alle Ionisierungen, berechne Bäume via Heuristik
//    // 3. evtl. Multithreaded: Berechne exakte Lösung für jeden Baum
//    // 4. Breche ab, wenn ausreichend gute exakte Lösungen gefunden wurden
//
//    protected class HeuristicJob extends BasicJJob<IntermediateResult> {
//
//        protected Decomposition decomposition;
//
//        protected HeuristicJob(Decomposition formula) {
//            super(JobType.CPU);
//            this.decomposition = formula;
//        }
//
//
//        @Override
//        protected IntermediateResult compute() throws Exception {
//            final FGraph graph = analyzer.buildGraph(pinput, decomposition);
//            // compute heuristic
//
//            //final FTree heuristic = new CriticalPathSolver(graph).solve();
//            final FTree heuristic = new ExtendedCriticalPathHeuristic(graph).solve();
//
//
//            IntermediateResult result = new IntermediateResult(decomposition, heuristic.getTreeWeight());
//            return result;
//        }
//    }
//
//    protected class GraphBuildingJob extends BasicJJob<FGraph> {
//        private final Decomposition decomposition;
//
//        public GraphBuildingJob(Decomposition decomposition) {
//            super(JobType.CPU);
//            this.decomposition = decomposition;
//        }
//
//        @Override
//        protected FGraph compute() throws Exception {
//            return analyzer.buildGraph(pinput, decomposition);
//        }
//    }
//
//    protected ExactResult recalibrate(ProcessedInput input, FTree tree) {
//        final SpectralRecalibration rec = new HypothesenDrivenRecalibration2().collectPeaksFromMs2(input.getExperimentInformation(), tree);
//        final ProcessedInput pin = input.getRecalibratedVersion(rec);
//        // we have to completely rescore the input...
//        final DecompositionList l = new DecompositionList(Arrays.asList(pin.getAnnotationOrThrow(DecompositionList.class).find(tree.getRoot().getFormula())));
//        pin.setAnnotation(DecompositionList.class, l);
//        analyzer.performPeakScoring(pin);
//        FGraph graph = analyzer.buildGraph(pin, l.getDecompositions().get(0));
//        graph.addAnnotation(SpectralRecalibration.class, rec);
//        graph.setAnnotation(ProcessedInput.class, pin);
//        final FTree recalibratedTree = analyzer.computeTree(graph);
//        //System.out.println("Recalibrate " + tree.getRoot().getFormula() + " => " + rec.getRecalibrationFunction() + "  ( " + (recalibratedTree.getTreeWeight() - tree.getTreeWeight()) + ")");
//        recalibratedTree.setAnnotation(SpectralRecalibration.class, rec);
//        recalibratedTree.setAnnotation(ProcessedInput.class, pin);
//        return new ExactResult(l.getDecompositions().get(0), graph, recalibratedTree, recalibratedTree.getTreeWeight());
//    }
//
//    protected final static class IntermediateResult implements Comparable<IntermediateResult> {
//
//        protected final Decomposition candidate;
//        protected double heuristicScore;
//
//        public IntermediateResult(Decomposition formula, double heuristicScore) {
//            this.candidate = formula;
//            this.heuristicScore = heuristicScore;
//
//        }
//
//        public String toString() {
//            return candidate.getCandidate() + ": " + heuristicScore;
//        }
//
//        @Override
//        public int compareTo(IntermediateResult o) {
//            return Double.compare(heuristicScore, o.heuristicScore);
//        }
//    }
//
//    public final static class FinalResult {
//        protected final boolean canceledDueToLowScore;
//        protected final List<FTree> results;
//
//        public FinalResult(List<FTree> results) {
//            this.canceledDueToLowScore = false;
//            this.results = results;
//        }
//
//        public FinalResult() {
//            this.canceledDueToLowScore = true;
//            this.results = null;
//        }
//
//        public List<FTree> getResults() {
//            return results;
//        }
//    }
//
//    protected final static class ExactResult implements Comparable<ExactResult> {
//
//        protected final Decomposition decomposition;
//        protected final double score;
//        protected FGraph graph;
//        protected FTree tree;
//
//        public ExactResult(Decomposition decomposition, FGraph graph, FTree tree, double score) {
//            this.decomposition = decomposition;
//            this.score = score;
//            this.tree = tree;
//            this.graph = graph;
//        }
//
//        @Override
//        public boolean equals(Object o) {
//            if (o instanceof ExactResult) return equals((ExactResult) o);
//            else return false;
//        }
//
//        public boolean equals(ExactResult o) {
//            return score == o.score && decomposition.getCandidate().equals(o.decomposition.getCandidate());
//        }
//
//        @Override
//        public int compareTo(ExactResult o) {
//            final int a = Double.compare(score, o.score);
//            if (a != 0) return a;
//            return decomposition.getCandidate().compareTo(o.decomposition.getCandidate());
//        }
//    }
//
//    protected static class ExactResultComparator implements Comparator<ExactResult> {
//
//        @Override
//        public int compare(ExactResult o1, ExactResult o2) {
//            return o1.decomposition.getCandidate().compareTo(o2.decomposition.getCandidate());
//        }
//    }
}
