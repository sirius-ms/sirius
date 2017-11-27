package de.unijena.bioinf.FragmentationTreeConstruction.computation;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.TreeScoring;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.recalibration.HypothesenDrivenRecalibration2;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.recalibration.SpectralRecalibration;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.TreeSizeScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.SinglethreadedTreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.ftheuristics.ExtendedCriticalPathHeuristic;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Decomposition;
import de.unijena.bioinf.FragmentationTreeConstruction.model.DecompositionList;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Whiteset;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.JobManager;

import java.util.*;
import java.util.concurrent.ExecutionException;

public class TreeComputationInstance extends BasicJJob<TreeComputationInstance.FinalResult> {

    protected final JobManager jobManager;
    protected final FragmentationPatternAnalysis analyzer;
    protected final Ms2Experiment experiment;
    protected final int numberOfResultsToKeep;
    protected ProcessedInput pinput;
    // yet another workaround =/
    // 0 = unprocessed, 1 = validated, 2 =  preprocessed, 3 = scored
    protected int state=0;

    public TreeComputationInstance(JobManager manager, FragmentationPatternAnalysis analyzer, Ms2Experiment input, int numberOfResultsToKeep) {
        super(JJob.JobType.CPU);
        this.jobManager = manager;
        this.analyzer = analyzer;
        this.experiment = input;
        this.numberOfResultsToKeep = numberOfResultsToKeep;
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
                stats1[0],stats1[1],(int)stats1[2],
                heuristic2.getTreeWeight(),
                exact2.getTreeWeight(),
                p3,
                p4,
                n3,
                n4,
                stats2[0], stats2[1], (int)stats2[2]

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

        return new double[]{intersection/union, intersection/fs.size(), intersection };

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
        if (state <= 2)  {
            this.pinput = analyzer.performPeakScoring(precompute());
            state = 3;
        }
        return pinput;
    }

    @Override
    protected FinalResult compute() throws Exception {
        score();
        // preprocess input
        TreeSizeScorer.TreeSizeBonus treeSizeBonus;
        final TreeSizeScorer tss = FragmentationPatternAnalysis.getByClassName(TreeSizeScorer.class, analyzer.getFragmentPeakScorers());
        if (tss!=null) {
            treeSizeBonus = new TreeSizeScorer.TreeSizeBonus(tss.getTreeSizeScore());
            pinput.setAnnotation(TreeSizeScorer.TreeSizeBonus.class, treeSizeBonus);
        } else {
            treeSizeBonus = null;
        }
        double inc=0d;

        // as long as we do not find good quality results
        while (true) {
            final boolean retryWithHigherScore = inc < MAX_TREESIZE_INCREASE;
            // compute heuristics
            final List<HeuristicJob> heuristics = new ArrayList<>();
            for (final Decomposition formula : pinput.getAnnotationOrThrow(DecompositionList.class).getDecompositions()) {
                final HeuristicJob heuristicJob = new HeuristicJob(formula);
                jobManager.submitSubJob(heuristicJob);
                heuristics.add(heuristicJob);
            }
            // collect results
            final List<IntermediateResult> intermediateResults = new ArrayList<>();
            int k=0;
            for (HeuristicJob job : heuristics) {
                try {
                    intermediateResults.add(job.awaitResult());
                } catch (ExecutionException e) {
                    e.printStackTrace();
                    throw e;
                }
            }
            // sort by score
            Collections.sort(intermediateResults, Collections.reverseOrder());
            // now compute from best scoring compound to lowest scoring compound
            final FinalResult fr;
            if (analyzer.getTreeBuilder() instanceof SinglethreadedTreeBuilder) {
                fr = computeExactTreesSinglethreaded(intermediateResults, retryWithHigherScore);
            } else {
                fr = computeExactTreesInParallel(intermediateResults, retryWithHigherScore);
            }
            if (tss!=null && retryWithHigherScore && fr.canceledDueToLowScore) {
                inc += TREE_SIZE_INCREASE;
                treeSizeBonus = new TreeSizeScorer.TreeSizeBonus(treeSizeBonus.score + TREE_SIZE_INCREASE);
                tss.fastReplace(pinput, treeSizeBonus);
                /*
                //pinput.setAnnotation(Scoring.class,null);
                pinput.setAnnotation(TreeSizeScorer.TreeSizeBonus.class,treeSizeBonus);
                analyzer.performPeakScoring(pinput);
                */
            } else return fr;
            //
        }

        //return null;
    }

    private FinalResult computeExactTreesInParallel(List<IntermediateResult> intermediateResults, boolean earlyStopping) {
        return computeExactTreesSinglethreaded(intermediateResults,earlyStopping);
    }

    private FinalResult computeExactTreesSinglethreaded(List<IntermediateResult> intermediateResults, boolean earlyStopping) {
        // compute in batches
        double threshold = Double.NEGATIVE_INFINITY;
        final int BATCH_SIZE = 10;
        final List<FTree> results = new ArrayList<>();
        final int n = numberOfResultsToKeep;
        double maximumGap = 0d;
        final DoubleEndWeightedQueue2<ExactResult> queue = new DoubleEndWeightedQueue2<>(Math.max(20,n+10),new ExactResultComparator());
        final DoubleEndWeightedQueue2<ExactResult> graphCache;
        // store at maximum 30 graphs
        if (queue.capacity > 30){
            graphCache = new DoubleEndWeightedQueue2<>(30, new ExactResultComparator());
        } else {
            graphCache = null;
        }

        outerLoop:
        for (int i=0; i < intermediateResults.size(); i += BATCH_SIZE) {
            final List<IntermediateResult> batch = intermediateResults.subList(i, Math.min(intermediateResults.size(),i+BATCH_SIZE));
            final List<GraphBuildingJob> graphs = computeGraphBatches(batch);
            for (int j=0; j < graphs.size(); ++j) {
                final FGraph graph = graphs.get(j).takeResult();
                final IntermediateResult intermediateResult = batch.get(j);
                if (intermediateResult.heuristicScore < threshold) {
                    System.err.println("BREAK AFTER " + (j + i)  + " / " + intermediateResults.size() + " steps! Max GAP IS " + maximumGap);
                    break outerLoop;
                }
                final FTree tree = analyzer.computeTreeWithoutAnnotating(graph, intermediateResult.heuristicScore-1e-3);
                if (tree==null) continue ;
                maximumGap = Math.max(maximumGap, tree.getTreeWeight() - intermediateResult.heuristicScore);
                ExactResult r = new ExactResult(graphs.get(j).decomposition, graph, tree, tree.getTreeWeight());
                if (graphCache!=null) {
                    graphCache.add(r,r.score);
                    r = new ExactResult(r.decomposition,null,r.tree,r.score);
                }
                if (! queue.add(r, r.score) ) {
                    // we have computed enough trees. Let's calculate lowerbound threshold
                    threshold = queue.lowerbound - maximumGap;
                } else if (graphCache!=null) {
                    // we have to annotate the tree
                    analyzer.addTreeAnnotations(graph,r.tree);
                }
            }
        }
        if (graphCache!=null) {
            for (ExactResult r : graphCache) {
                queue.replace(r,r.score);
            }
            graphCache.clear();
        }

        boolean CHECK_FOR_TREESIZE = earlyStopping;
        final ArrayList<ExactResult> exactResults = new ArrayList<>();
        for (ExactResult r : queue) {
            exactResults.add(new ExactResult(r.decomposition,r.graph,r.tree,r.score));
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
        int k=0;
        double maxRecalibrationBonus = Double.POSITIVE_INFINITY;
        final ListIterator<ExactResult> riter = exactResults.listIterator();
        while (riter.hasNext()) {
            ExactResult r = riter.next();
            if (k > numberOfResultsToKeep) {
                riter.remove();
                continue;
            }
            FGraph graph;
            if (r.graph==null) {
                graph = analyzer.buildGraph(pinput,r.decomposition);
            } else graph = r.graph;
            if (r.tree.getAnnotationOrNull(ProcessedInput.class)==null)
                analyzer.addTreeAnnotations(graph,r.tree);
            final FTree tree = r.tree;
            ExactResult recalibratedResult = recalibrate(pinput, tree);
            final FTree recalibrated;
            final double recalibrationBonus = recalibratedResult.tree.getTreeWeight() - tree.getTreeWeight();
            double recalibrationPenalty = 0d;
            if (recalibrationBonus<=0) {
                recalibrated = tree;
            } else {
                graph = recalibratedResult.graph;
                recalibrated = recalibratedResult.tree;
            }
            if (k <= 10) {
                maxRecalibrationBonus = Math.min(recalibrated.getTreeWeight(), maxRecalibrationBonus);
            } else {
                recalibrationPenalty = Math.min(recalibrationBonus, Math.max(0, recalibrated.getTreeWeight() - maxRecalibrationBonus));
            }
            final TreeScoring sc = recalibrated.getAnnotationOrThrow(TreeScoring.class);
            sc.setRecalibrationBonus(recalibrationBonus);
            sc.setRecalibrationPenalty(recalibrationPenalty);
            sc.setOverallScore(sc.getOverallScore() - sc.getRecalibrationPenalty());
            recalibrated.setTreeWeight(recalibrated.getTreeWeight()-recalibrationPenalty);
            riter.set(new ExactResult(r.decomposition, null, recalibrated, recalibrated.getTreeWeight()));
            ++k;
        }
        Collections.sort(exactResults, Collections.reverseOrder());
        final int nl = Math.min(numberOfResultsToKeep,exactResults.size());
        final ArrayList<FTree> finalResults = new ArrayList<>(nl);
        for (int m=0; m < nl; ++m) {
            final boolean correct = analyzer.recalculateScores(exactResults.get(m).tree);
            finalResults.add(exactResults.get(m).tree);
        }


        return new FinalResult(finalResults);
    }


    private static final double MAX_TREESIZE_INCREASE = 3d;
    private static final double TREE_SIZE_INCREASE = 1d;
    private static final int MIN_NUMBER_OF_EXPLAINED_PEAKS = 15;
    private static final double MIN_EXPLAINED_INTENSITY = 0.7d;
    private static final int MIN_NUMBER_OF_TREES_CHECK_FOR_INTENSITY = 5;

    private boolean checkForTreeQuality(List<ExactResult> results) {
        for (ExactResult r : results) {
            final FTree tree = r.tree;
            if (analyzer.getIntensityRatioOfExplainedPeaksFromUnanotatedTree(pinput,tree, r.decomposition.getIon()) >= MIN_EXPLAINED_INTENSITY && tree.numberOfVertices() >= Math.min(pinput.getMergedPeaks().size()-2, MIN_NUMBER_OF_EXPLAINED_PEAKS)) {
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
            jobManager.submitSubJob(job);
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
        final FTree recalibratedTree = analyzer.computeTree(graph);
        //System.out.println("Recalibrate " + tree.getRoot().getFormula() + " => " + rec.getRecalibrationFunction() + "  ( " + (recalibratedTree.getTreeWeight() - tree.getTreeWeight()) + ")");
        recalibratedTree.setAnnotation(SpectralRecalibration.class, rec);
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
            if (o instanceof ExactResult) return equals((ExactResult)o);
            else return false;
        }

        public boolean equals(ExactResult o) {
            return score == o.score && decomposition.getCandidate().equals(o.decomposition.getCandidate());
        }

        @Override
        public int compareTo(ExactResult o) {
            final int a = Double.compare(score, o.score);
            if (a!=0) return a;
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
