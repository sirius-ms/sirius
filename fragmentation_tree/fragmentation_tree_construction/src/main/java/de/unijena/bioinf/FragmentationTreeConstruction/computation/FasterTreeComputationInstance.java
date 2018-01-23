package de.unijena.bioinf.FragmentationTreeConstruction.computation;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.recalibration.HypothesenDrivenRecalibration2;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.recalibration.SpectralRecalibration;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.TreeSizeScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.ftheuristics.treebuilder.ExtendedCriticalPathHeuristicTreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.model.*;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.jjobs.exceptions.TimeoutException;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class FasterTreeComputationInstance extends AbstractTreeComputationInstance {

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

    public FasterTreeComputationInstance(JobManager manager, FragmentationPatternAnalysis analyzer, Ms2Experiment input, int numberOfResultsToKeep) {
        super();
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
        List<Decomposition> decompositions = pinput.getAnnotationOrThrow(DecompositionList.class).getDecompositions();
        // as long as we do not find good quality results
        final boolean useHeuristic = true;//pinput.getParentPeak().getMz() > 300;
        final ExactResult[] results = estimateTreeSizeAndRecalibration(decompositions, useHeuristic);
        final List<FTree> trees = new ArrayList<>(results.length);
        for (ExactResult r : results) trees.add(r.tree);
        return new FinalResult(trees);
    }

    public ExactResult[] estimateTreeSizeAndRecalibration(List<Decomposition> decompositions, boolean useHeuristic) {
        final int NCPUS = jobManager().getCPUThreads();
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
        double treeSize = treeSizeBonus.score;
        final List<ExactResult> results = new ArrayList<>(decompositions.size());
        // TREE SIZE
        while (inc <= MAX_TREESIZE_INCREASE) {
            tss.fastReplace(pinput, new TreeSizeScorer.TreeSizeBonus(treeSize));
            results.clear();
            final List<TreeComputationJob> jobs = new ArrayList<>(decompositions.size());
            final TreeBuilder builder = useHeuristic ? new ExtendedCriticalPathHeuristicTreeBuilder() : analyzer.getTreeBuilder();
            for (Decomposition d : decompositions) {
                final TreeComputationJob job = new TreeComputationJob(builder,null,d);
                submitSubJob(job);
                jobs.add(job);
            }
            for (TreeComputationJob job : jobs) {
                results.add(job.takeResult());
            }
            Collections.sort(results, Collections.reverseOrder());
            final int treeSizeCheck = Math.min(results.size(),MIN_NUMBER_OF_TREES_CHECK_FOR_INTENSITY);
            if (checkForTreeQuality(results.subList(0,treeSizeCheck))) {
                break;
            }
            inc += TREE_SIZE_INCREASE;
            treeSize += TREE_SIZE_INCREASE;
        }
        final List<ExactResult> topResults = results.subList(0,Math.min(results.size(), n+10));
        if (pinput.getAnnotation(ForbidRecalibration.class, ForbidRecalibration.FORBIDDEN).isForbidden()) {
            final List<BasicJJob<ExactResult>> jobs = new ArrayList<>();
            if (useHeuristic) {
                topResults.forEach((t)->jobs.add(new ExactJob(t)));
            } else {
                topResults.forEach((t)->jobs.add(new AnnotationJob(t)));
            }
            jobs.forEach(this::submitSubJob);
            return jobs.stream().map(JJob::takeResult).sorted(Collections.reverseOrder()).toArray(ExactResult[]::new);
        }
        final List<RecalibrationJob> recalibrationJobs = new ArrayList<>();
        for (ExactResult r : topResults) {
            final RecalibrationJob recalibrationJob = new RecalibrationJob(r);
            submitSubJob(recalibrationJob);
            recalibrationJobs.add(recalibrationJob);
        }
        final ExactResult[] exact = recalibrationJobs.stream().map(JJob::takeResult).sorted(Collections.reverseOrder()).toArray(ExactResult[]::new);
        return exact;
    }

    protected class ExactJob extends BasicJJob<ExactResult> {
        private final ExactResult template;

        public ExactJob(ExactResult template) {
            this.template = template;
        }

        @Override
        protected ExactResult compute() throws Exception {
            FGraph graph = analyzer.buildGraph(pinput, template.decomposition);
            final FTree tree = analyzer.getTreeBuilder().computeTree().withMultithreading(1).withTimeLimit(secondsPerTree).withMinimalScore(template.score-1e-3).withTemplate(template.tree).solve(pinput, graph).tree;
            analyzer.addTreeAnnotations(graph,tree);
            return new ExactResult(template.decomposition,null,tree,tree.getTreeWeight());
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
            analyzer.addTreeAnnotations(graph,tree);
            return new ExactResult(template.decomposition,null,tree,tree.getTreeWeight());
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
            final FGraph graph = analyzer.buildGraphWithoutReduction(pinput, decomposition);
            final FTree tree = treeBuilder.computeTree().withTimeLimit(secondsPerTree).solve(pinput, graph).tree;
            final ExactResult er = new ExactResult(decomposition,null,tree,tree.getTreeWeight());
            if (graphCache!=null) {
                double score = graphCache.getWeightLowerbound();
                if (tree.getTreeWeight()>score) {
                    synchronized (graphCache) {
                        if (tree.getTreeWeight()>graphCache.getWeightLowerbound()) {
                            er.graph = graph;
                            if (!graphCache.add(er,tree.getTreeWeight()))
                                er.graph=null;
                        }
                    }
                }
            }
            return er;
        }
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
            return new ExactResult(r.decomposition, null, recalibratedResult.tree, recalibratedResult.score);
        }
    }

    private static final double MAX_TREESIZE_INCREASE = 3d;
    private static final double TREE_SIZE_INCREASE = 1d;
    private static final int MIN_NUMBER_OF_EXPLAINED_PEAKS = 15;
    private static final double MIN_EXPLAINED_INTENSITY = 0.7d;
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

    protected ExactResult recalibrate(ProcessedInput input, FTree tree) {
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
        final FTree recalibratedTree = analyzer.computeTree(graph);
        recalibratedTree.setAnnotation(SpectralRecalibration.class, rec);
        recalibratedTree.setAnnotation(ProcessedInput.class, pin);
        return new ExactResult(l.getDecompositions().get(0), graph, recalibratedTree, recalibratedTree.getTreeWeight());
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

    protected static class ExactResultComparator implements Comparator<ExactResult> {

        @Override
        public int compare(ExactResult o1, ExactResult o2) {
            return o1.decomposition.getCandidate().compareTo(o2.decomposition.getCandidate());
        }
    }
}
