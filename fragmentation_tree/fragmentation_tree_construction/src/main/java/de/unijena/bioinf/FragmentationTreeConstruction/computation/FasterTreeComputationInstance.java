/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.FragmentationTreeConstruction.computation;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.NumberOfCandidates;
import de.unijena.bioinf.ChemistryBase.ms.NumberOfCandidatesPerIonization;
import de.unijena.bioinf.ChemistryBase.ms.PossibleAdducts;
import de.unijena.bioinf.ChemistryBase.ms.ft.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.Decomposition;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.ForbidRecalibration;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.Timeout;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.Whiteset;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.recalibration.HypothesenDrivenRecalibration;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.TreeSizeScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.ftheuristics.treebuilder.ExtendedCriticalPathHeuristicTreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.model.UseHeuristic;
import de.unijena.bioinf.elgordo.LipidSpecies;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.jjobs.JJob;
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

    protected long startTimeMillis;
    protected long millisPerTree;
    protected long secsPerTree;
    //    protected volatile int restTime;
//    protected int secondsPerInstance, secondsPerTree;
//    protected final Timeout timeout;
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
        this.numberOfResultsToKeepPerIonization = input.getAnnotationOrDefault(NumberOfCandidatesPerIonization.class).value;
        this.ticks = new AtomicInteger(0);
        Timeout timeout = pinput.getAnnotation(Timeout.class).orElse(Timeout.NO_TIMEOUT);
        secsPerTree = timeout.getNumberOfSecondsPerDecomposition();
        millisPerTree = secsPerTree * 1000L;
        withTimeLimit(timeout.getNumberOfSecondsPerInstance() * 1000L);
    }

    private FasterTreeComputationInstance(FragmentationPatternAnalysis analyzer, ProcessedInput input, FTree tree) {
        this(analyzer, input);
        this.pinput = input;
        this.pinput.setAnnotation(Whiteset.class, Whiteset.ofMeasuredFormulas(Collections.singleton(tree.getRoot().getFormula()), FasterTreeComputationInstance.class));
        this.inputCopyForRecalibration = pinput;
        score();
    }

    private long restTimeSec() {
        return Math.max(1, ((startTimeMillis + getTimeLimit()) - System.currentTimeMillis())/1000L);
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
        configureProgress((int) Math.min(to-1, this.currentProgress().getProgress()), to, numberOfTicks);
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

        startTimeMillis = System.currentTimeMillis();
//        secondsPerInstance = timeout.getNumberOfSecondsPerInstance();
//        secondsPerTree = timeout.getNumberOfSecondsPerDecomposition();
//        restTime = secondsPerInstance;//Math.min(secondsPerInstance, secondsPerTree);
        // preprocess input
        List<Decomposition> decompositions = pinput.getAnnotationOrThrow(DecompositionList.class).getDecompositions();
        // as long as we do not find good quality results

        final UseHeuristic uh = pinput.getAnnotation(UseHeuristic.class).orElse(UseHeuristic.newInstance(300, 650));
        final boolean useHeuristic = pinput.getParentPeak().getMass() >= uh.useHeuristicAboveMz;
        final boolean useHeuristicOny = pinput.getParentPeak().getMass() >= uh.useOnlyHeuristicAboveMz;

        checkForInterruption();

        final ExactResult[] results = estimateTreeSizeAndRecalibration(decompositions, useHeuristic, useHeuristicOny);

        checkForInterruption();

        if (results.length > 0) {
            final UnconsideredCandidatesUpperBound it = new UnconsideredCandidatesUpperBound(
                    pinput.getAnnotationOrThrow(DecompositionList.class).getDecompositions().size() - results.length,
                    results[results.length - 1].score);
            for (ExactResult result : results) result.tree.setAnnotation(UnconsideredCandidatesUpperBound.class, it);
        }

        final List<FTree> trees = Arrays.stream(results).map(r -> r.tree).collect(Collectors.toList());
        return new FinalResult(trees);
    }

    protected void recalculateScore(ProcessedInput input, FTree tree, String prefix) {
        double oldScore = tree.getTreeWeight();
        double newScore = analyzer.recalculateScores(input, tree);

        if (Math.abs(newScore - oldScore) > 0.1) {

            final double treeSize = tree.numberOfVertices()==1 ? 0 : tree.getFragmentAnnotationOrNull(Score.class).get(tree.getFragmentAt(tree.numberOfVertices() - 1)).get("TreeSizeScorer");
            this.logTrace(prefix + ": Score of " + tree.getRoot().getFormula() + " differs significantly from recalculated score: " + oldScore + " vs " + newScore + " with tree size is " + pinput.getAnnotation(TreeSizeScorer.TreeSizeBonus.class, () -> new TreeSizeScorer.TreeSizeBonus(-0.5d)).score + " and root score is " + tree.getFragmentAnnotationOrNull(Score.class).get(tree.getFragmentAt(0)).sum() + " and " + treeSize + " sort key is score " + tree.getTreeWeight() + " and filename is " + pinput.getExperimentInformation().getSourceString() + " " + pinput.getExperimentInformation().getName());
        }

    }

    public ExactResult[] estimateTreeSizeAndRecalibration(List<Decomposition> decompositions, boolean useHeuristic, boolean useHeuristicOnly) throws ExecutionException, InterruptedException {
        if (useHeuristicOnly)
            useHeuristic = true;

//        final int NCPUS = jobManager.getCPUThreads();
//        final int BATCH_SIZE = Math.min(4 * NCPUS, Math.max(30, NCPUS));
//        final int MAX_GRAPH_CACHE_SIZE = Math.max(30, BATCH_SIZE);
//        final int n = Math.min(decompositions.size(), numberOfResultsToKeep);


        //enforced molecular formulas that must be kept independent of score
        Set<PrecursorIonType> ionTypes = pinput.getAnnotationOrThrow(PossibleAdducts.class).getAdducts();
        Set<MolecularFormula> enforcedMeasuredFormulas = pinput.getAnnotationOr(Whiteset.class, c -> Whiteset.empty()).getNeutralEnforcedAsMeasuredFormulasSet(ionTypes);


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
        checkForInterruption();

        // TREE SIZE
        while (inc <= MAX_TREESIZE_INCREASE) {
            configureProgress(2, useHeuristic ? 50 : 90, decompositions.size());
            if (tss != null) tss.fastReplace(pinput, new TreeSizeScorer.TreeSizeBonus(treeSize));
            results.clear();
            final TreeBuilder builder = useHeuristic ? getHeuristicTreeBuilder() : analyzer.getTreeBuilder();
            final List<TreeComputationJob> jobs = decompositions.stream().filter(d -> !Double.isInfinite(d.getScore())).map(d -> (TreeComputationJob) new TreeComputationJob(builder, null, d).withEndTime(getEndTime()).withTimeLimit(millisPerTree)).collect(Collectors.toList());
            checkForInterruption();
            submitSubJobsInBatches(jobs, SiriusJobs.getCPUThreads() * 4).forEach(JJob::takeResult);

            /*for (Decomposition d : decompositions) {
                if (Double.isInfinite(d.getScore())) continue;
                final TreeComputationJob job = new TreeComputationJob(builder, null, d);
                submitSubJob(job);
                jobs.add(job);
                    checkForInterruption();
                    checkTimeout();
            }*/

//            int counter = 0;
            for (TreeComputationJob job : jobs) {
                results.add(job.awaitResult());
                checkForInterruption();
                /*if (++counter % 100 == 0) {
                    checkForInterruption();
                    checkTimeout();
                }*/
            }
            results.sort(Collections.reverseOrder());
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

        final int numberOfResultsToKeep = Math.min(results.size(), this.numberOfResultsToKeep);
        final int numberOfResultsToKeepPerIonization = Math.min(this.numberOfResultsToKeepPerIonization, results.size());

        final List<ExactResult> topResults = extractTopResults(results, numberOfResultsToKeep + 10, numberOfResultsToKeepPerIonization + 5, enforcedMeasuredFormulas);
        configureProgress(100, topResults.size());
        checkForInterruption();

        if (pinput.getAnnotationOrDefault(ForbidRecalibration.class).isForbidden()) {
            final List<BasicJJob<ExactResult>> jobs = new ArrayList<>();
            if (useHeuristic && !useHeuristicOnly) {
                topResults.forEach((t) -> jobs.add((ExactJob) new ExactJob(t, analyzer.getTreeBuilder()).withEndTime(getEndTime()).withTimeLimit(millisPerTree)));
            } else {
                topResults.forEach((t) -> jobs.add(new AnnotationJob(t)));
            }
            checkForInterruption();
            jobs.forEach(this::submitSubJob);
            LoggerFactory.getLogger(FasterTreeComputationInstance.class).debug("Recalibration is disabled!");
            checkForInterruption();
            return extractTopResults(jobs.stream()
                    .map(JJob::takeResult).sorted(Collections.reverseOrder())
                    .collect(Collectors.toList()), numberOfResultsToKeep, numberOfResultsToKeepPerIonization, enforcedMeasuredFormulas)
                    .toArray(ExactResult[]::new);
        }
        final List<RecalibrationJob> recalibrationJobs = new ArrayList<>();
        for (ExactResult r : topResults) {
            checkForInterruption();
            TreeBuilder builder = useHeuristic ? getHeuristicTreeBuilder() : analyzer.getTreeBuilder();
            final RecalibrationJob recalibrationJob = (RecalibrationJob) new RecalibrationJob(r, builder,
                    useHeuristicOnly ? builder : analyzer.getTreeBuilder()).withEndTime(getEndTime()).withTimeLimit(millisPerTree);
            submitSubJob(recalibrationJob);
            recalibrationJobs.add(recalibrationJob);
        }
        final ExactResult[] recalibrated = extractTopResults(recalibrationJobs.stream()
                .map(JJob::takeResult)
                .sorted(Collections.reverseOrder())
                .collect(Collectors.toList()), numberOfResultsToKeep, numberOfResultsToKeepPerIonization, enforcedMeasuredFormulas)
                .toArray(ExactResult[]::new);
        final double[] originalScores = new double[recalibrated.length];
        for (int k = 0; k < recalibrated.length; ++k) originalScores[k] = recalibrated[k].score;
        final ExactJob[] beautify = new ExactJob[recalibrated.length];
        final TIntArrayList beautifyTodo = new TIntArrayList();



        if (tss!=null) {
            //beautify trees
            while (true) {
                checkForInterruption();
                beautifyTodo.clearQuick();
                for (int k = 0; k < recalibrated.length; ++k) {
                    if (!recalibrated[k].tree.getAnnotation(Beautified.class, Beautified::ugly).isBeautiful()) {
                        if ((treeSize >= MAX_TREESIZE || inc >= MAX_TREESIZE_INCREASE) || isHighQuality(recalibrated[k])) {
                            setBeautiful(recalibrated[k], treeSize, originalTreeSize, originalScores[k]);
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
                    checkForInterruption();
                    final int K = beautifyTodo.getQuick(j);
                    if (recalibrated[K].input != null) {
                        tss.fastReplace(recalibrated[K].input, new TreeSizeScorer.TreeSizeBonus(treeSize));
                        recalibrated[K].input.setAnnotation(Beautified.class, Beautified.inProcess(inc));
                    } else {
                        pinput.setAnnotation(Beautified.class, Beautified.inProcess(inc));
                    }
                    beautify[K] = submitSubJob(new ExactJob(recalibrated[K], useHeuristicOnly ? getHeuristicTreeBuilder() : analyzer.getTreeBuilder()));
                }

                checkForInterruption();
                for (int j=0; j < beautifyTodo.size(); ++j) {
                    final int K = beautifyTodo.getQuick(j);
                    recalibrated[K] = beautify[K].takeResult();
                }
            }
            checkForInterruption();
            revertTreeSizeIncrease(recalibrated, originalTreeSize);
        } else {
            for (ExactResult exactResult : recalibrated) {
                setUgly(exactResult);
            }
        }

        return recalibrated;
    }

    private void revertTreeSizeIncrease(ExactResult[] exact, double orig) {
        //pinput.setAnnotation(TreeSizeScorer.TreeSizeBonus.class, new TreeSizeScorer.TreeSizeBonus(orig));
        for (ExactResult r : exact) {
            //if (r.input!=null) r.input.setAnnotation(TreeSizeScorer.TreeSizeBonus.class, new TreeSizeScorer.TreeSizeBonus(orig));
            final double penalty = (r.tree.getAnnotationOrThrow(Beautified.class).getBeautificationPenalty());
            r.tree.setTreeWeight(r.tree.getTreeWeight()-penalty);
            r.tree.setRootScore(r.tree.getRootScore()-penalty);
            recalculateScore(r.input==null ? pinput : r.input, r.tree, "final");
        }
    }

    private void setBeautiful(ExactResult t, double maxTreeSize, double originalTreeSize, double originalScore) {
        t.tree.setAnnotation(Beautified.class, Beautified.beautified(maxTreeSize-originalTreeSize, t.score-originalScore));
    }

    private void setUgly(ExactResult exactResult) {
        exactResult.tree.setAnnotation(Beautified.class, Beautified.ugly());
    }

    private List<ExactResult> extractTopResults(List<ExactResult> results, int numberOfResultsToKeep, int numberOfResultsToKeepPerIonization, Set<MolecularFormula> enforcedPrecursorFormulas) {
        List<ExactResult> returnList;
        if (numberOfResultsToKeepPerIonization<=0 || results.size()<=numberOfResultsToKeep){
            returnList = results.subList(0, Math.min(results.size(), numberOfResultsToKeep));
        } else {
            Map<Ionization, List<ExactResult>> ionToResults = new HashMap<>();
            for (ExactResult result : results) {
                final Ionization ion = result.decomposition.getIon();
                List<ExactResult> ionResults = ionToResults.get(ion);
                if (ionResults == null) {
                    ionResults = new ArrayList<>();
                    ionResults.add(result);
                    ionToResults.put(ion, ionResults);
                } else if (ionResults.size() < numberOfResultsToKeepPerIonization) {
                    ionResults.add(result);
                }
            }
            Set<ExactResult> exractedResults = new HashSet<>();
            exractedResults.addAll(results.subList(0, numberOfResultsToKeep));
            for (List<ExactResult> ionResults : ionToResults.values()) {
                exractedResults.addAll(ionResults);
            }

            returnList = new ArrayList<>(exractedResults);
            returnList.sort(Collections.reverseOrder());
        }

        //enforce some results into the top list
        Set<ExactResult> resultsSet = new HashSet<>();
        //make sure that annotated lipid MFs from ElGordo are always part of the list
        if (results.stream().anyMatch(r -> r.tree.hasAnnotation(LipidSpecies.class))) { //annotation should be on each tree. But for future fail-safety, we check all
            //in principle, this should only be a single tree possible
            List<ExactResult> lipids = results.stream().filter(r->r.tree.getAnnotation(LipidSpecies.class).map(ls -> ls.getHypotheticalMolecularFormula().map(mf->pinput.getAnnotation(PossibleAdducts.class, PossibleAdducts::empty).getAdducts(r.decomposition.getIon()).stream().anyMatch(it -> it.measuredNeutralMoleculeToNeutralMolecule(r.decomposition.getCandidate()).equals(mf))).orElse(false)).orElse(false)).collect(Collectors.toList());

            resultsSet.addAll(lipids);
        }
        //enforced formulas always part of results
        if (enforcedPrecursorFormulas!=null && enforcedPrecursorFormulas.size()>0) {
            List<ExactResult> enforced = results.stream().filter(r -> enforcedPrecursorFormulas.contains(r.decomposition.getCandidate())).collect(Collectors.toList());
            resultsSet.addAll(enforced);
        }
        if (resultsSet.size()>0) {
            resultsSet.addAll(returnList);
            returnList = new ArrayList<>(resultsSet);
            returnList.sort(Collections.reverseOrder());
        }

        return returnList;
    }

    @NotNull
    private ExtendedCriticalPathHeuristicTreeBuilder getHeuristicTreeBuilder() {
        return new ExtendedCriticalPathHeuristicTreeBuilder(this::checkHeuristicInterruption);
    }

    private boolean checkHeuristicInterruption() throws InterruptedException {
        this.checkForInterruption();
        return false;
    }

    /*private ExactResult takeResultAndCheckTime(BasicJJob<ExactResult> r) {
        final ExactResult result = r.takeResult();
        checkTimeout();
        return result;
    }*/

    protected class ExactJob extends BasicJJob<ExactResult> {
        private final ExactResult template;
        private final TreeBuilder treeBuilder;

        public ExactJob(ExactResult template, TreeBuilder treeBuilder) {
            this.template = template;
            this.treeBuilder = treeBuilder;
        }

        @Override
        protected ExactResult compute() throws Exception {
            final ProcessedInput input = template.input == null ? pinput : template.input;
            final FGraph graph = treeBuilder instanceof ExtendedCriticalPathHeuristicTreeBuilder ? analyzer.buildGraphWithoutReduction(pinput, template.decomposition) : analyzer.buildGraph(pinput, template.decomposition);
            checkForInterruption();
            final TreeBuilder.Result tree = treeBuilder.computeTree().withMultithreading(1).withTimeLimit(Math.min(restTimeSec(), secsPerTree))/*.withMinimalScore(template.score - 1e-3).withTemplate(template.tree)*/.solve(pinput, graph);
            checkForInterruption();
            analyzer.makeTreeReleaseReady(input, graph, tree.tree, tree.mapping);
            checkForInterruption();
            recalculateScore(input, tree.tree, "ExactJob");
            tick();
            return new ExactResult(template.input == null ? null : template.input, template.decomposition, null, tree.tree, tree.tree.getTreeWeight());
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
            checkForInterruption();
            final TreeBuilder.Result r = analyzer.getTreeBuilder().computeTree().withTimeLimit(Math.min(restTimeSec(), secsPerTree)).solve(pinput, graph);
            checkForInterruption();
            analyzer.makeTreeReleaseReady(pinput, graph, r.tree, r.mapping);
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
            checkForInterruption();
//            System.err.println(Objects.toString(treeBuilder));
            final FTree tree = treeBuilder.computeTree().withTimeLimit(Math.min(restTimeSec(), secsPerTree)).solve(pinput, graph).tree;
            checkForInterruption();
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

        @Override
        public String identifier() {
            return super.identifier() + " | " + experiment.getName() + "@" + experiment.getIonMass() + "m/z | " +decomposition;
        }
    }



    /*private void checkTimeout() {
        final long time = System.currentTimeMillis();
        final int elapsedTime = (int) ((time - startTime) / 1000);
        final int min = Math.min(restTime, secondsPerInstance - elapsedTime);
        restTime = min;
        if (restTime <= 0) throw new TimeoutException("FasterTreeComputationInstance canceled by timeout!");
    }*/

    private class RecalibrationJob extends BasicJJob<ExactResult> {
        private final ExactResult r;
        private final TreeBuilder builder;
        private final TreeBuilder finalBuilder;


        public RecalibrationJob(ExactResult input, TreeBuilder builder, TreeBuilder finalBuilder) {
            this.r = input;
            this.builder = builder;
            this.finalBuilder = finalBuilder;
        }

        @Override
        protected ExactResult compute() throws Exception {
            return recalibrate(pinput, builder, finalBuilder, r.decomposition, r.tree, r.graph);
        }
    }

    protected ExactResult recalibrate(ProcessedInput input, TreeBuilder builder, TreeBuilder finalBuilder, Decomposition decomp, FTree tree, FGraph origGraphOrNull) throws InterruptedException {
        final SpectralRecalibration rec = new HypothesenDrivenRecalibration().collectPeaksFromMs2(input, tree);
        final ProcessedInput pin = this.inputCopyForRecalibration.clone();
        pin.setAnnotation(PossibleAdducts.class, new PossibleAdducts(PrecursorIonType.getPrecursorIonType(decomp.getIon())));
        pin.setAnnotation(SpectralRecalibration.class, rec);
        pin.setAnnotation(Whiteset.class, Whiteset.ofMeasuredFormulas(Collections.singleton(tree.getRoot().getFormula()), RecalibrationJob.class).setIgnoreMassDeviationToResolveIonType(true).setFinalized(true)); // TODO: check if this works for adducts //TODO ElementFilter: check old todo
        pin.getExperimentInformation().setPrecursorIonType(tree.getAnnotation(PrecursorIonType.class).orElse(pin.getExperimentInformation().getPrecursorIonType()));
        pin.setAnnotation(TreeSizeScorer.TreeSizeBonus.class, pinput.getAnnotationOrNull(TreeSizeScorer.TreeSizeBonus.class));
        // we have to completely rescore the input...
        checkForInterruption();
        analyzer.performDecomposition(pin);
        checkForInterruption();
        analyzer.performPeakScoring(pin);
        if (!pin.hasAnnotation(DecompositionList.class) || pin.getAnnotationOrThrow(DecompositionList.class).getDecompositions().isEmpty() || !pin.getAnnotationOrThrow(DecompositionList.class).getDecompositions().get(0).equals(decomp)){
            logWarn("Could not recreate decomposiion during recalibration for " + input.getExperimentInformation().getName() + ". Falling back to non recalibrated tree.");
            return new ExactResult(input, decomp, null, tree, tree.getTreeWeight());
        }
        final FGraph graph = analyzer.buildGraph(pin, decomp);
        graph.addAnnotation(SpectralRecalibration.class, rec);
        checkForInterruption();
        final TreeBuilder.Result recal = builder.computeTree().withTimeLimit(Math.min(restTimeSec(), secsPerTree)).solve(pin, graph);
        checkForInterruption();
        TreeBuilder.Result finalTree;
        if (recal.tree.getTreeWeight() >= tree.getTreeWeight()) {
            finalTree = builder == finalBuilder ? recal : finalBuilder.computeTree().withTimeLimit(Math.min(restTimeSec(), secsPerTree)).solve(pin, graph);
            checkForInterruption();
            //this is to prevent null trees in case the ILP solver fails.
            if (finalTree == null || finalTree.tree == null) {
                // TODO: why is tree score != ILP score? Or is this an error in ILP?
                // check that
                TreeBuilder.Result solve = analyzer.getTreeBuilder().computeTree().withTimeLimit(Math.min(restTimeSec(), secsPerTree)).solve(pin, graph);
                logWarn("Recalibrated tree is null for " + input.getExperimentInformation().getName() + ". Error in ILP? Without score constraint the result is = optimal = " + solve.isOptimal + ", score = " + (solve.tree == null ? "NULL" : solve.tree.getTreeWeight()) + " with score of uncalibrated tree is " + recal.tree.getTreeWeight()
                        + ". Falling back to the heuristic tree. Please submit a bug report with the input data of this instance and this error message.");
                finalTree = recal;
            }
            finalTree.tree.setAnnotation(SpectralRecalibration.class, rec);
            analyzer.makeTreeReleaseReady(pin, graph, finalTree.tree, finalTree.mapping);
        } else {
            //todo we could skip recomputing heuristic tree but mapping from source tree is missing here
            pin.setAnnotation(SpectralRecalibration.class, SpectralRecalibration.none());
            final FGraph origGraph = origGraphOrNull == null ? analyzer.buildGraph(pinput, decomp) : origGraphOrNull;
            checkForInterruption();
            finalTree = finalBuilder.computeTree().withTimeLimit(Math.min(restTimeSec(), secsPerTree)).solve(pin, origGraph);
            checkForInterruption();
            //this is to prevent null trees in case the ILP solver fails.
            if (finalTree == null || finalTree.tree == null) {
                logWarn("Recalibrated ILP tree is null for '" + input.getExperimentInformation().getName() + "'. Falling back to the heuristic tree. Please submit a bug report with the input data of this instance and this error message.");
                finalTree = builder.computeTree().withTimeLimit(Math.min(restTimeSec(), secsPerTree)).solve(pin, origGraph);
            }
            finalTree.tree.setAnnotation(SpectralRecalibration.class, SpectralRecalibration.none());
            analyzer.makeTreeReleaseReady(pin, origGraph, finalTree.tree, finalTree.mapping);
        }
        checkForInterruption();
        recalculateScore(pin, finalTree.tree, "recalibrate");
        tick();
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


    @Override
    public String identifier() {
        return super.identifier() + " | " + experiment.getName() + "@" + experiment.getIonMass() + "m/z";
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
