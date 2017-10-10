package de.unijena.bioinf.FragmentationTreeConstruction.computation;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.FragmentationTreeConstruction.ftheuristics.solver.CriticalPathSolver;
import de.unijena.bioinf.FragmentationTreeConstruction.model.DecompositionList;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.JobManager;

import java.util.ArrayList;
import java.util.List;

public class TreeComputationInstance extends BasicJJob<TreeComputationInstance.TreeComputationResult> {

    protected static class TreeComputationResult {

    }

    protected final JobManager jobManager;
    protected final FragmentationPatternAnalysis analyzer;
    protected final Ms2Experiment experiment;
    protected final int numberOfResultsToKeep;
    protected ProcessedInput pinput;

    public TreeComputationInstance(JobManager manager, FragmentationPatternAnalysis analyzer, Ms2Experiment input, int numberOfResultsToKeep) {
        super(JJob.JobType.CPU);
        this.jobManager = manager;
        this.analyzer = analyzer;
        this.experiment = input;
        this.numberOfResultsToKeep = numberOfResultsToKeep;
    }

    @Override
    protected TreeComputationResult compute() throws Exception {
        // preprocess input
        this.pinput = analyzer.preprocessing(experiment);
        // compute heuristics
        final List<HeuristicJob> heuristics = new ArrayList<>();
        for (final Scored<MolecularFormula> formula : pinput.getAnnotationOrThrow(DecompositionList.class).getDecompositions()) {
            final HeuristicJob heuristicJob = new HeuristicJob(formula);
            jobManager.submitSubJob(heuristicJob);
            heuristics.add(heuristicJob);
        }
        // collect results
        final List<IntermediateResult> intermediateResults = new ArrayList<>();
        for (HeuristicJob job : heuristics) {
            intermediateResults.add(job.compute());
        }


        return null;
    }


    // 1. Multithreaded: Berechne ProcessedInput für alle Ionisierungen
    // 2. Multithreaded: Berechne Graphen für alle Ionisierungen, berechne Bäume via Heuristik
    // 3. evtl. Multithreaded: Berechne exakte Lösung für jeden Baum
    // 4. Breche ab, wenn ausreichend gute exakte Lösungen gefunden wurden

    protected class HeuristicJob extends BasicJJob<IntermediateResult> {

        protected Scored<MolecularFormula> decomposition;

        protected HeuristicJob(Scored<MolecularFormula> formula) {
            super(JobType.CPU);
            this.decomposition = formula;
        }


        @Override
        protected IntermediateResult compute() throws Exception {
            final FGraph graph = analyzer.buildGraph(pinput, decomposition);
            // compute heuristic
            final FTree heuristic = new CriticalPathSolver(graph).solve();
            IntermediateResult result = new IntermediateResult(decomposition, 0);
            result.heuristicScore = heuristic.getTreeWeight();
            return result;
        }
    }

    protected static class IntermediateResult implements Comparable<IntermediateResult> {

        protected final Scored<MolecularFormula> candidate;
        protected final int ionType;
        protected double heuristicScore, exactScore, score;
        protected FTree tree;

        public IntermediateResult(Scored<MolecularFormula> formula, int ionType) {
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
        public int compareTo(IntermediateResult o) {
            return Double.compare(score, o.score);
        }
    }
}
