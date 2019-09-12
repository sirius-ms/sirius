package de.unijena.bioinf.FragmentationTreeConstruction.computation;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.NumberOfCandidates;
import de.unijena.bioinf.ChemistryBase.ms.NumberOfCandidatesPerIon;
import de.unijena.bioinf.ChemistryBase.ms.ft.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.Decomposition;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.ForbidRecalibration;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.Timeout;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.Whiteset;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.recalibration.HypothesenDrivenRecalibration;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.TreeSizeScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.ftheuristics.treebuilder.ExtendedCriticalPathHeuristicTreeBuilder;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.exceptions.TimeoutException;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.annotations.DecompositionList;
import de.unijena.bioinf.sirius.annotations.SpectralRecalibration;
import gnu.trove.list.array.TIntArrayList;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class FasterTreeComputationInstance extends BasicMasterJJob<FasterTreeComputationInstance.FinalResult> {

    protected final Ms2Experiment experiment;
    protected final int numberOfResultsToKeep;
    protected final int numberOfResultsToKeepPerIonization;

    protected ProcessedInput inputCopyForRecalibration;

    // yet another workaround =/
    // 0 = unprocessed, 1 = validated, 2 =  preprocessed, 3 = scored
    protected AtomicInteger ticks;
    protected volatile int nextProgress;
    protected int ticksPerProgress, progressPerTick;

    private int state = 0;

    protected long startTime;
    protected volatile int restTime;
    protected int secondsPerInstance, secondsPerTree;

    /**
     *
     * @param analyzer
     * @param input
     */
    public FasterTreeComputationInstance(FragmentationPatternAnalysis analyzer, ProcessedInput input) {
        super(JJob.JobType.CPU);
        this.analyzer = analyzer;
        this.experiment = input.getExperimentInformation();
        this.pinput = input;
        this.inputCopyForRecalibration = input.clone();
        this.numberOfResultsToKeep = input.getAnnotationOrDefault(NumberOfCandidates.class).value;
        this.numberOfResultsToKeepPerIonization = input.getAnnotationOrDefault(NumberOfCandidatesPerIon.class).value;
        this.ticks = new AtomicInteger(0);
    }

    private FasterTreeComputationInstance(FragmentationPatternAnalysis analyzer, ProcessedInput input, FTree tree) {
        this(analyzer, input);
        this.pinput = input;
        this.pinput.setAnnotation(Whiteset.class, Whiteset.of(tree.getRoot().getFormula()));
        this.inputCopyForRecalibration = pinput;
        score();
    }

    private ProcessedInput score() {
        if (state <= 2) {
            this.pinput = analyzer.performDecomposition(pinput);
            this.pinput = analyzer.performPeakScoring(pinput);
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
            updateProgress(0, 100, Math.min(incrementation, max));
            while (true) {
                int x = ticks.get();
                nextProgress = x + ticksPerProgress;
                if (ticks.get() < nextProgress) break;
            }
        }
    }

    protected void configureProgress(int to, int numberOfTicks) {
        configureProgress(Math.min(to-1,this.currentProgress().getNewValue()), to, numberOfTicks);
    }

    protected void configureProgress(int from, int to, int numberOfTicks) {
        int span = to - from;
        if (numberOfTicks<1) numberOfTicks=1;
        if (numberOfTicks > span) {
            ticksPerProgress = numberOfTicks / span;
            progressPerTick = 1;
        } else {
            ticksPerProgress = 1;
            progressPerTick = span / numberOfTicks;
        }
        ticks.set((from * ticksPerProgress)/progressPerTick);
        nextProgress = ((from + progressPerTick) * ticksPerProgress)/progressPerTick;
        updateProgress(from);
    }

    @Override
    protected FinalResult compute() throws Exception {
        final long t = System.nanoTime();
        System.out.println(new Date() + "\t-> I am Sirius, computing trees for Experiment " + pinput.getExperimentInformation().getName());
        configureProgress(0, 2, 1);
        score();
        startTime = System.currentTimeMillis();
        final Timeout timeout = pinput.getAnnotationOrDefault(Timeout.class);
        secondsPerInstance = timeout.getNumberOfSecondsPerInstance();
        secondsPerTree = timeout.getNumberOfSecondsPerDecomposition();
        restTime = Math.min(secondsPerInstance, secondsPerTree);
        // preprocess input
        List<Decomposition> decompositions = pinput.getAnnotationOrThrow(DecompositionList.class).getDecompositions();
        // as long as we do not find good quality results
        final boolean useHeuristic = pinput.getParentPeak().getMass() > 300;
        final ExactResult[] results = estimateTreeSizeAndRecalibration(decompositions, useHeuristic);
        //we do not resolve here anymore -> because we need unresolved trees to expand adducts for fingerid
        final List<FTree> trees = Arrays.stream(results).map(r -> r.tree).collect(Collectors.toList());
        final long t2 = System.nanoTime();
        final int timeInSeconds = (int)Math.round((t2-t)*1e-9);
        System.out.println(new Date() + "\t-> I am Sirius, finished with computing trees for Experiment " + pinput.getExperimentInformation().getName() +" which took " + (timeInSeconds) + " seconds.");
        return new FinalResult(trees);
    }

    protected void recalculateScore(ProcessedInput input, FTree tree, String prefix) {
        double oldScore = tree.getTreeWeight();
        double newScore = analyzer.recalculateScores(input, tree);

        if (Math.abs(newScore - oldScore) > 0.1) {

            final double treeSize = tree.numberOfVertices()==1 ? 0 : tree.getFragmentAnnotationOrNull(Score.class).get(tree.getFragmentAt(tree.numberOfVertices() - 1)).get("TreeSizeScorer");
            this.LOG().warn(prefix + ": Score of " + tree.getRoot().getFormula() + " differs significantly from recalculated score: " + oldScore + " vs " + newScore + " with tree size is " + pinput.getAnnotation(TreeSizeScorer.TreeSizeBonus.class, () -> new TreeSizeScorer.TreeSizeBonus(-0.5d)).score + " and root score is " + tree.getFragmentAnnotationOrNull(Score.class).get(tree.getFragmentAt(0)).sum() + " and " + treeSize + " sort key is score " + tree.getTreeWeight() + " and filename is " + String.valueOf(pinput.getExperimentInformation().getSource()));
        }

    }

    public ExactResult[] estimateTreeSizeAndRecalibration(List<Decomposition> decompositions, boolean useHeuristic) throws ExecutionException {
        final int NCPUS = jobManager.getCPUThreads();
        final int BATCH_SIZE = Math.min(4 * NCPUS, Math.max(30, NCPUS));
        final int MAX_GRAPH_CACHE_SIZE = Math.max(30, BATCH_SIZE);
//        final int n = Math.min(decompositions.size(), numberOfResultsToKeep);

        TreeSizeScorer.TreeSizeBonus treeSizeBonus;
        final TreeSizeScorer tss = FragmentationPatternAnalysis.getByClassName(TreeSizeScorer.class, analyzer.getFragmentPeakScorers());
        if (tss != null) {
            treeSizeBonus = pinput.getAnnotation(TreeSizeScorer.TreeSizeBonus.class, () -> new TreeSizeScorer.TreeSizeBonus(tss.getTreeSizeScore()));
            pinput.setAnnotation(TreeSizeScorer.TreeSizeBonus.class, treeSizeBonus);
        } else {
            treeSizeBonus = null;
        }
        double inc = tss == null ? 0d : treeSizeBonus.score - tss.getTreeSizeScore();
        double treeSize = treeSizeBonus == null ? 0d : treeSizeBonus.score;
        final double originalTreeSize = treeSize;
        final List<ExactResult> results = new ArrayList<>(decompositions.size());
        // TREE SIZE
        while (inc <= MAX_TREESIZE_INCREASE) {
            configureProgress(2, useHeuristic ? 50 : 90,decompositions.size());
            if (tss != null) tss.fastReplace(pinput, new TreeSizeScorer.TreeSizeBonus(treeSize));
            results.clear();
            final List<TreeComputationJob> jobs = new ArrayList<>(decompositions.size());
            final TreeBuilder builder = useHeuristic ? getHeuristicTreeBuilder() : analyzer.getTreeBuilder();
            for (Decomposition d : decompositions) {
                if (Double.isInfinite(d.getScore())) continue;
                final TreeComputationJob job = new TreeComputationJob(builder, null, d);
                submitSubJob(job);
                jobs.add(job);
            }
            int counter = 0;
            for (TreeComputationJob job : jobs) {
                results.add(job.awaitResult());
                if (++counter % 100 == 0) {
                    checkTimeout();
                }
            }
            Collections.sort(results, Collections.reverseOrder());
            final int treeSizeCheck = Math.min(results.size(), MIN_NUMBER_OF_TREES_CHECK_FOR_INTENSITY);
            if (tss == null || checkForTreeQuality(results.subList(0, treeSizeCheck))) {
                break;
            }
            inc += TREE_SIZE_INCREASE;
            treeSize += TREE_SIZE_INCREASE;
        }
        if (inc > MAX_TREESIZE_INCREASE) {
            inc -= TREE_SIZE_INCREASE;
            treeSize -= TREE_SIZE_INCREASE;
        }
        final List<ExactResult> topResults = extractExactResults(results, numberOfResultsToKeep+10, numberOfResultsToKeepPerIonization+5);
        configureProgress(100, topResults.size());
        if (pinput.getAnnotationOrDefault(ForbidRecalibration.class).isForbidden()) {
            final List<BasicJJob<ExactResult>> jobs = new ArrayList<>();
            if (useHeuristic) {
                topResults.forEach((t) -> jobs.add(new ExactJob(t)));
            } else {
                topResults.forEach((t) -> jobs.add(new AnnotationJob(t)));
            }
            jobs.forEach(this::submitSubJob);
            LoggerFactory.getLogger(FasterTreeComputationInstance.class).warn("Recalibration is disabled!");
            return jobs.stream().map(this::takeResultAndCheckTime).sorted(Collections.reverseOrder()).toArray(ExactResult[]::new);
        }
        final List<RecalibrationJob> recalibrationJobs = new ArrayList<>();
        for (ExactResult r : topResults) {
            final RecalibrationJob recalibrationJob = new RecalibrationJob(r, useHeuristic ? getHeuristicTreeBuilder() : analyzer.getTreeBuilder());
            submitSubJob(recalibrationJob);
            recalibrationJobs.add(recalibrationJob);
        }
        final ExactResult[] exact = extractExactResults(recalibrationJobs.stream().map(this::takeResultAndCheckTime).sorted(Collections.reverseOrder()).collect(Collectors.toList()), numberOfResultsToKeep, numberOfResultsToKeepPerIonization).toArray(new ExactResult[0]);
        final double[] originalScores = new double[exact.length];
        for (int k=0; k < exact.length; ++k) originalScores[k] = exact[k].score;
        final ExactJob[] beautify = new ExactJob[exact.length];
        final TIntArrayList beautifyTodo = new TIntArrayList();

        while (tss!=null) {
            beautifyTodo.resetQuick();
            for (int k=0; k < exact.length; ++k) {
                if (!exact[k].tree.getAnnotation(Beautified.class, Beautified::ugly).isBeautiful()) {
                    if ((treeSize >= MAX_TREESIZE || inc >= MAX_TREESIZE_INCREASE )|| isHighQuality(exact[k])) {
                        setBeautiful(exact[k], treeSize, originalTreeSize, originalScores[k]);
                    } else {
                        beautifyTodo.add(k);
                    }
                }
            }
            if (beautifyTodo.isEmpty()) break;
            inc += TREE_SIZE_INCREASE;
            treeSize += TREE_SIZE_INCREASE;
            tss.fastReplace(pinput, new TreeSizeScorer.TreeSizeBonus(treeSize));
            for (int j=0; j < beautifyTodo.size(); ++j) {
                final int K = beautifyTodo.getQuick(j);
                if (exact[K].input!=null) {
                    tss.fastReplace(exact[K].input, new TreeSizeScorer.TreeSizeBonus(treeSize));
                    exact[K].input.setAnnotation(Beautified.class, Beautified.inProcess(inc));
                } else {
                    pinput.setAnnotation(Beautified.class, Beautified.inProcess(inc));
                }
                beautify[K] = submitSubJob(new ExactJob(exact[K]));
            }
            for (int j=0; j < beautifyTodo.size(); ++j) {
                final int K = beautifyTodo.getQuick(j);
                exact[K] = beautify[K].takeResult();
            }
        }
        revertTreeSizeIncrease(exact, originalTreeSize);

        return exact;
    }

    private void revertTreeSizeIncrease(ExactResult[] exact, double orig) {
        //pinput.setAnnotation(TreeSizeScorer.TreeSizeBonus.class, new TreeSizeScorer.TreeSizeBonus(orig));
        for (ExactResult r : exact) {
            //if (r.input!=null) r.input.setAnnotation(TreeSizeScorer.TreeSizeBonus.class, new TreeSizeScorer.TreeSizeBonus(orig));
            final double penalty = (r.tree.getAnnotationOrThrow(Beautified.class).getBeautificationPenalty());
            r.tree.setTreeWeight(r.tree.getTreeWeight()-penalty);
            recalculateScore(r.input==null ? pinput : r.input, r.tree, "final");
        }
    }

    private void setBeautiful(ExactResult t, double maxTreeSize, double originalTreeSize, double originalScore) {
        t.tree.setAnnotation(Beautified.class, Beautified.beautified(maxTreeSize-originalTreeSize, t.score-originalScore));
    }

    private List<ExactResult> extractExactResults(List<ExactResult> results, int numberOfResultsToKeep, int numberOfResultsToKeepPerIonization) {
        if (numberOfResultsToKeepPerIonization<=0 || results.size()<=numberOfResultsToKeep){
            return results.subList(0, Math.min(results.size(), numberOfResultsToKeep));
        } else {
            Map<Ionization, List<ExactResult>> ionToResults = new HashMap<>();
            int i = 0;
            for (ExactResult result : results) {
                final Ionization ion = result.decomposition.getIon();
                List<ExactResult> ionResults = ionToResults.get(ion);
                if (ionResults==null){
                    ionResults = new ArrayList<>();
                    ionResults.add(result);
                    ionToResults.put(ion, ionResults);
                } else if (ionResults.size()<numberOfResultsToKeepPerIonization){
                    ionResults.add(result);
                }
                ++i;
            }
            Set<ExactResult> exractedResults = new HashSet<>();
            exractedResults.addAll(results.subList(0, numberOfResultsToKeep));
            for (List<ExactResult> ionResults : ionToResults.values()) {
                exractedResults.addAll(ionResults);
            }
            List<ExactResult> list = new ArrayList<>();
            list.addAll(exractedResults);
            Collections.sort(list, Collections.reverseOrder());
            if (!list.isEmpty()) {
                final UnconsideredCandidatesUpperBound unconsideredCandidatesUpperBound = new UnconsideredCandidatesUpperBound(pinput.getAnnotationOrThrow(DecompositionList.class).getDecompositions().size(), list.get(list.size()-1).score);
                list.forEach(t->t.tree.setAnnotation(UnconsideredCandidatesUpperBound.class, unconsideredCandidatesUpperBound));
            }

            return list;
        }
    }

    @NotNull
    private ExtendedCriticalPathHeuristicTreeBuilder getHeuristicTreeBuilder() {
        return new ExtendedCriticalPathHeuristicTreeBuilder();
    }

    private ExactResult takeResultAndCheckTime(BasicJJob<ExactResult> r) {
        final ExactResult result = r.takeResult();
        checkTimeout();
        return result;
    }

    protected class ExactJob extends BasicJJob<ExactResult> {
        private final ExactResult template;

        public ExactJob(ExactResult template) {
            this.template = template;
        }

        @Override
        protected ExactResult compute() throws Exception {
            final ProcessedInput input = template.input==null ? pinput : template.input;
            FGraph graph = analyzer.buildGraph(input, template.decomposition);
            final TreeBuilder.Result tree = analyzer.getTreeBuilder().computeTree().withMultithreading(1).withTimeLimit(Math.min(restTime, secondsPerTree))/*.withMinimalScore(template.score - 1e-3).withTemplate(template.tree)*/.solve(pinput, graph);
            analyzer.makeTreeReleaseReady(input, graph, tree.tree, tree.mapping);
            recalculateScore(input, tree.tree, "ExactJob");
            tick();
            return new ExactResult(template.input==null ? null : template.input, template.decomposition, null, tree.tree, tree.tree.getTreeWeight());
        }
    }

    protected class AnnotationJob extends BasicJJob<ExactResult> {
        private final ExactResult template;

        public AnnotationJob(ExactResult template) {
            this.template = template;
        }

        @Override
        protected ExactResult compute() throws Exception {
            FGraph graph = analyzer.buildGraph(pinput, template.decomposition);
            // TODO: we recompute the tree. Is that really a good idea?
            // Find a better solution
            final TreeBuilder.Result r = analyzer.getTreeBuilder().computeTree().withTimeLimit(Math.min(restTime, secondsPerTree))/*.withTemplate(template.tree)*/.solve(pinput, graph);
            analyzer.makeTreeReleaseReady(pinput, graph, r.tree,r.mapping);
            tick();
            recalculateScore(pinput, r.tree, "annotation");
            return new ExactResult(template.decomposition, null, r.tree, r.tree.getTreeWeight());
        }
    }

    protected class TreeComputationJob extends BasicJJob<ExactResult> {

        private TreeBuilder treeBuilder;
        private DoubleEndWeightedQueue2<ExactResult> graphCache;
        private Decomposition decomposition;

        public TreeComputationJob(TreeBuilder treeBuilder, DoubleEndWeightedQueue2<ExactResult> graphCache, Decomposition decomposition) {
            this.treeBuilder = treeBuilder;
            this.graphCache = graphCache;
            this.decomposition = decomposition;
        }


        @Override
        protected ExactResult compute() throws Exception {
            final FGraph graph = treeBuilder instanceof ExtendedCriticalPathHeuristicTreeBuilder ? analyzer.buildGraphWithoutReduction(pinput, decomposition) : analyzer.buildGraph(pinput, decomposition);
            final FTree tree = treeBuilder.computeTree().withTimeLimit(Math.min(restTime, secondsPerTree)).solve(pinput, graph).tree;
            final ExactResult er = new ExactResult(decomposition, null, tree, tree.getTreeWeight());
            if (graphCache != null) {
                double score = graphCache.getWeightLowerbound();
                if (tree.getTreeWeight() > score) {
                    synchronized (graphCache) {
                        if (tree.getTreeWeight() > graphCache.getWeightLowerbound()) {
                            er.graph = graph;
                            if (!graphCache.add(er, tree.getTreeWeight()))
                                er.graph = null;
                        }
                    }
                }
            }
            tick();
            return er;
        }

        @Override
        protected void cleanup() {
            super.cleanup();
            this.treeBuilder = null;
            this.graphCache = null;
            this.decomposition = null;
        }
    }



    private void checkTimeout() {
        final long time = System.currentTimeMillis();
        final int elapsedTime = (int) ((time - startTime) / 1000);
        restTime = Math.min(restTime, secondsPerInstance - elapsedTime);
        if (restTime <= 0) throw new TimeoutException("FasterTreeComputationInstance canceled by timeout!");
    }

    private class RecalibrationJob extends BasicJJob<ExactResult> {
        private final ExactResult r;
        private final TreeBuilder tb;

        public RecalibrationJob(ExactResult input, TreeBuilder tb) {
            this.r = input;
            this.tb = tb;
        }

        @Override
        protected ExactResult compute() throws Exception {
            return recalibrate(pinput, tb, r.decomposition, r.tree, r.graph);
        }
    }

    protected ExactResult recalibrate(ProcessedInput input, TreeBuilder tb, Decomposition decomp, FTree tree, FGraph origGraphOrNull) {
        final SpectralRecalibration rec = new HypothesenDrivenRecalibration().collectPeaksFromMs2(input, tree);
        final ProcessedInput pin = this.inputCopyForRecalibration.clone();
        pin.setAnnotation(SpectralRecalibration.class, rec);
        pin.setAnnotation(Whiteset.class, Whiteset.of(input.getExperimentInformation().getPrecursorIonType().measuredNeutralMoleculeToNeutralMolecule(tree.getRoot().getFormula()))); // TODO: check if this works for adducts
        pin.setAnnotation(TreeSizeScorer.TreeSizeBonus.class, pinput.getAnnotationOrNull(TreeSizeScorer.TreeSizeBonus.class));
        // we have to completely rescore the input...
        //final DecompositionList l = new DecompositionList(Arrays.asList(pin.getAnnotationOrThrow(DecompositionList.class).find(tree.getRoot().getFormula())));
        //pin.setAnnotation(DecompositionList.class, l);
        analyzer.performDecomposition(pin);
        analyzer.performPeakScoring(pin);
        FGraph graph = analyzer.buildGraph(pin, decomp);
        graph.addAnnotation(SpectralRecalibration.class, rec);
        final TreeBuilder.Result recal = tb.computeTree().withTimeLimit(Math.min(restTime, secondsPerTree)).solve(pin, graph);
        final TreeBuilder.Result finalTree;
        if (recal.tree.getTreeWeight() >= tree.getTreeWeight()) {
            finalTree = analyzer.getTreeBuilder().computeTree().withTimeLimit(Math.min(restTime, secondsPerTree))/*.withTemplate(recal.tree)/*.withMinimalScore(recal.tree.getTreeWeight() - 1e-3)*/.solve(pin, graph);
            if (finalTree.tree==null){
                // TODO: why is tree score != ILP score? Or is this an error in ILP?
                // check that
                TreeBuilder.Result solve = analyzer.getTreeBuilder().computeTree().withTimeLimit(Math.min(restTime, secondsPerTree))/*.withTemplate(recal.tree)*/.solve(pin, graph);
                throw new RuntimeException("Recalibrated tree is null for "+input.getExperimentInformation().getName()+". Error in ILP? Without score constraint the result is = optimal = " + solve.isOptimal + ", score = " + solve.tree.getTreeWeight() + " with score of uncalibrated tree is " + recal.tree.getTreeWeight());
            }
            finalTree.tree.setAnnotation(SpectralRecalibration.class, rec);
            analyzer.makeTreeReleaseReady(pin, graph, finalTree.tree, finalTree.mapping);
        } else {
            pin.setAnnotation(SpectralRecalibration.class, SpectralRecalibration.none());
            final FGraph origGraph = origGraphOrNull==null ? analyzer.buildGraph(pinput, decomp) : origGraphOrNull;
            finalTree = analyzer.getTreeBuilder().computeTree().withTimeLimit(Math.min(restTime, secondsPerTree))/*.withTemplate(tree).withMinimalScore(tree.getTreeWeight() - 1e-3)*/.solve(pin, origGraph);
            finalTree.tree.setAnnotation(SpectralRecalibration.class, SpectralRecalibration.none());
            analyzer.makeTreeReleaseReady(pin, origGraph, finalTree.tree, finalTree.mapping);
        }
        recalculateScore(pin, finalTree.tree, "recalibrate");
        assert finalTree!=null;
        tick();
        if (pin.getAnnotationOrThrow(DecompositionList.class).getDecompositions().size() <= 0) {
            System.err.println("WTF?");
        }
        return new ExactResult(pin, pin.getAnnotationOrThrow(DecompositionList.class).getDecompositions().get(0), null, finalTree.tree, finalTree.tree.getTreeWeight());
    }


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


    protected boolean checkForTreeQuality(List<ExactResult> results) {
        for (ExactResult r : results) {
            final FTree tree = r.tree;
            if (isHighQuality(r)) {
                return true;
            }
        }
        return false;
    }

    protected boolean isHighQuality(ExactResult r) {
        return analyzer.getIntensityRatioOfExplainedPeaksFromUnanotatedTree(pinput, r.tree) >= MIN_EXPLAINED_INTENSITY && r.tree.numberOfVertices() >= Math.min(pinput.getMergedPeaks().size() - 2, MIN_NUMBER_OF_EXPLAINED_PEAKS);
    }


    protected final static class ExactResult implements Comparable<ExactResult> {

        protected ProcessedInput input;
        protected final Decomposition decomposition;
        protected final double score;
        protected FGraph graph;
        protected FTree tree;

        public ExactResult(ProcessedInput input, Decomposition decomposition, FGraph graph, FTree tree, double score) {
            this.input = input;
            this.decomposition = decomposition;
            this.score = score;
            this.tree = tree;
            this.graph = graph;
        }

        public ExactResult(Decomposition decomposition, FGraph graph, FTree tree, double score) {
            this(null,decomposition,graph,tree,score);
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
