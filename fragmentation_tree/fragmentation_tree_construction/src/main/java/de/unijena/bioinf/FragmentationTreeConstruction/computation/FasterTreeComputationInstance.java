package de.unijena.bioinf.FragmentationTreeConstruction.computation;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.*;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.recalibration.HypothesenDrivenRecalibration2;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.recalibration.SpectralRecalibration;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.TreeSizeScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.ftheuristics.treebuilder.ExtendedCriticalPathHeuristicTreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.model.*;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.exceptions.TimeoutException;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

public class FasterTreeComputationInstance extends AbstractTreeComputationInstance {

    protected final Ms2Experiment experiment;
    protected final int numberOfResultsToKeep;
    // yet another workaround =/
    // 0 = unprocessed, 1 = validated, 2 =  preprocessed, 3 = scored
    protected int state = 0;
    protected AtomicInteger ticks;
    protected volatile int nextProgress;
    protected int ticksPerProgress, progressPerTick;

    protected long startTime;
    protected volatile int restTime;
    protected int secondsPerInstance, secondsPerTree;

    public FasterTreeComputationInstance(FragmentationPatternAnalysis analyzer, Ms2Experiment input, int numberOfResultsToKeep) {
        super(analyzer);
        this.experiment = input;
        this.numberOfResultsToKeep = numberOfResultsToKeep;
        this.ticks = new AtomicInteger(0);
    }


    public static FasterTreeComputationInstance beautify(FragmentationPatternAnalysis analyzer, FTree tree) {
        return new FasterTreeComputationInstance(analyzer, tree.getAnnotationOrThrow(ProcessedInput.class), tree);
    }

    private FasterTreeComputationInstance(FragmentationPatternAnalysis analyzer, ProcessedInput input, FTree tree) {
        this(analyzer, input.getOriginalInput(), 1);
        this.pinput = input;
        this.pinput.setAnnotation(DecompositionList.class, new DecompositionList(Arrays.asList(new Decomposition(tree.getRoot().getFormula(), tree.getAnnotationOrThrow(PrecursorIonType.class).getIonization(), tree.getAnnotationOrThrow(TreeScoring.class).getRootScore()))));
        this.state = 3;
    }

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
        configureProgress(0, 2, 1);
        score();
        startTime = System.currentTimeMillis();
        final Timeout timeout = pinput.getAnnotation(Timeout.class, Timeout.NO_TIMEOUT);
        secondsPerInstance = timeout.getNumberOfSecondsPerInstance();
        secondsPerTree = timeout.getNumberOfSecondsPerDecomposition();
        restTime = Math.min(secondsPerInstance, secondsPerTree);
        // preprocess input
        List<Decomposition> decompositions = pinput.getAnnotationOrThrow(DecompositionList.class).getDecompositions();
        // as long as we do not find good quality results
        final boolean useHeuristic = pinput.getParentPeak().getMz() > 300;
        final ExactResult[] results = estimateTreeSizeAndRecalibration(decompositions, useHeuristic);
        final List<FTree> trees = new ArrayList<>(results.length);
        for (ExactResult r : results) trees.add(r.tree);
        trees.forEach(this::recalculateScore);
        return new FinalResult(trees);
    }

    protected void recalculateScore(FTree tree) {
        double oldScore = tree.getTreeWeight();
        double newScore = analyzer.recalculateScores(tree);
        if (Math.abs(newScore - oldScore) > 0.1) {
            final double treeSize = tree.numberOfVertices()==1 ? 0 : tree.getFragmentAnnotationOrNull(Score.class).get(tree.getFragmentAt(tree.numberOfVertices() - 1)).get("TreeSizeScorer");
            this.LOG().warn("Score of " + tree.getRoot().getFormula() + " differs significantly from recalculated score: " + oldScore + " vs " + newScore + " with tree size is " + pinput.getAnnotation(TreeSizeScorer.TreeSizeBonus.class, new TreeSizeScorer.TreeSizeBonus(-0.5d)).score + " and " + treeSize + " sort key is score " + tree.getTreeWeight() + " and filename is " + String.valueOf(pinput.getExperimentInformation().getSource()));
        }
    }

    public ExactResult[] estimateTreeSizeAndRecalibration(List<Decomposition> decompositions, boolean useHeuristic) throws ExecutionException {
        final int NCPUS = jobManager.getCPUThreads();
        final int BATCH_SIZE = Math.min(4 * NCPUS, Math.max(30, NCPUS));
        final int MAX_GRAPH_CACHE_SIZE = Math.max(30, BATCH_SIZE);
        final int n = Math.min(decompositions.size(), numberOfResultsToKeep);

        TreeSizeScorer.TreeSizeBonus treeSizeBonus;
        final TreeSizeScorer tss = FragmentationPatternAnalysis.getByClassName(TreeSizeScorer.class, analyzer.getFragmentPeakScorers());
        if (tss != null) {
            treeSizeBonus = new TreeSizeScorer.TreeSizeBonus(tss.getTreeSizeScore());
            pinput.setAnnotation(TreeSizeScorer.TreeSizeBonus.class, treeSizeBonus);
        } else {
            treeSizeBonus = null;
        }
        double inc = 0d;
        double treeSize = treeSizeBonus == null ? 0d : treeSizeBonus.score;
        final List<ExactResult> results = new ArrayList<>(decompositions.size());
        // TREE SIZE
        while (inc <= MAX_TREESIZE_INCREASE) {
            configureProgress(2, useHeuristic ? 50 : 90,decompositions.size());
            if (tss != null) tss.fastReplace(pinput, new TreeSizeScorer.TreeSizeBonus(treeSize));
            results.clear();
            final List<TreeComputationJob> jobs = new ArrayList<>(decompositions.size());
            final TreeBuilder builder = useHeuristic ? new ExtendedCriticalPathHeuristicTreeBuilder() : analyzer.getTreeBuilder();
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
            if (tss == null || checkForTreeQuality(results.subList(0, treeSizeCheck), false)) {
                break;
            }
            inc += TREE_SIZE_INCREASE;
            treeSize += TREE_SIZE_INCREASE;
        }
        final List<ExactResult> topResults = results.subList(0, Math.min(results.size(), n + 10));
        configureProgress(100, topResults.size());
        if (pinput.getAnnotation(ForbidRecalibration.class, ForbidRecalibration.ALLOWED).isForbidden()) {
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
            final RecalibrationJob recalibrationJob = new RecalibrationJob(r, useHeuristic ? new ExtendedCriticalPathHeuristicTreeBuilder() : analyzer.getTreeBuilder());
            submitSubJob(recalibrationJob);
            recalibrationJobs.add(recalibrationJob);
        }
        final ExactResult[] exact = recalibrationJobs.stream().map(this::takeResultAndCheckTime).sorted(Collections.reverseOrder()).limit(numberOfResultsToKeep).toArray(ExactResult[]::new);

        if (inc >= MAX_TREESIZE_INCREASE) {
            for (ExactResult t : exact) t.tree.setAnnotation(Beautified.class, Beautified.IS_BEAUTIFUL);
        } else {
            checkForTreeQuality(Arrays.asList(exact), true);
        }
        return exact;
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
            FGraph graph = analyzer.buildGraph(pinput, template.decomposition);
            final FTree tree = analyzer.getTreeBuilder().computeTree().withMultithreading(1).withTimeLimit(Math.min(restTime, secondsPerTree)).withMinimalScore(template.score - 1e-3)/*.withTemplate(template.tree)*/.solve(pinput, graph).tree;
            analyzer.addTreeAnnotations(graph, tree);
            tick();
            return new ExactResult(template.decomposition, null, tree, tree.getTreeWeight());
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
            final FTree tree = template.tree;
            analyzer.addTreeAnnotations(graph, tree);
            tick();
            return new ExactResult(template.decomposition, null, tree, tree.getTreeWeight());
        }
    }

    protected class TreeComputationJob extends BasicJJob<ExactResult> {

        protected final TreeBuilder treeBuilder;
        protected final DoubleEndWeightedQueue2<ExactResult> graphCache;
        protected final Decomposition decomposition;

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
    }

    private void checkTimeout() {
        final long time = System.currentTimeMillis();
        final int elapsedTime = (int) ((time - startTime) / 1000);
        restTime = Math.min(restTime, secondsPerInstance - elapsedTime);
        if (restTime <= 0) throw new TimeoutException();
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
            FGraph graph;
            if (r.graph == null) {
                graph = analyzer.buildGraph(pinput, r.decomposition);
            } else graph = r.graph;
            final FTree tree = r.tree;
            return recalibrate(pinput, tb, tree, graph);
        }
    }

    protected ExactResult recalibrate(ProcessedInput input, TreeBuilder tb, FTree tree, FGraph origGraph) {
        if (tree.getAnnotationOrNull(ProcessedInput.class) == null)
            analyzer.addTreeAnnotations(origGraph, tree);
        final SpectralRecalibration rec = new HypothesenDrivenRecalibration2().collectPeaksFromMs2(input.getExperimentInformation(), tree);
        final ProcessedInput pin = input.getRecalibratedVersion(rec);
        // we have to completely rescore the input...
        final DecompositionList l = new DecompositionList(Arrays.asList(pin.getAnnotationOrThrow(DecompositionList.class).find(tree.getRoot().getFormula())));
        pin.setAnnotation(DecompositionList.class, l);
        analyzer.performDecomposition(pin);
        analyzer.performPeakScoring(pin);
        FGraph graph = analyzer.buildGraph(pin, l.getDecompositions().get(0));
        graph.addAnnotation(SpectralRecalibration.class, rec);
        graph.setAnnotation(ProcessedInput.class, pin);
        final FTree recal = tb.computeTree().withTimeLimit(Math.min(restTime, secondsPerTree)).solve(pin, graph).tree;
        final FTree finalTree;
        if (recal.getTreeWeight() > tree.getTreeWeight()) {
            finalTree = analyzer.getTreeBuilder().computeTree().withTimeLimit(Math.min(restTime, secondsPerTree)).withTemplate(recal).withMinimalScore(recal.getTreeWeight() - 1e-3).solve(pin, graph).tree;
            finalTree.setAnnotation(SpectralRecalibration.class, rec);
            finalTree.setAnnotation(ProcessedInput.class, pin);
            finalTree.setAnnotation(RecalibrationFunction.class, rec.toPolynomial());
            analyzer.addTreeAnnotations(graph, finalTree);
        } else {
            finalTree = analyzer.getTreeBuilder().computeTree().withTimeLimit(Math.min(restTime, secondsPerTree)).withTemplate(tree).withMinimalScore(tree.getTreeWeight() - 1e-3).solve(input, origGraph).tree;
            finalTree.setAnnotation(ProcessedInput.class, input);
            finalTree.setAnnotation(RecalibrationFunction.class, RecalibrationFunction.identity());
            finalTree.setAnnotation(SpectralRecalibration.class, SpectralRecalibration.none());
            analyzer.addTreeAnnotations(origGraph, finalTree);
        }
        assert finalTree!=null;
        tick();
        return new ExactResult(l.getDecompositions().get(0), null, finalTree, finalTree.getTreeWeight());
    }
}
