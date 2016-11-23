package de.unijena.bioinf.FragmentationTreeConstruction;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.TreeScoring;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.FragmentationTreeConstruction.ftheuristics.ftreeheuristics.solver.CriticalPathSolver;
import de.unijena.bioinf.FragmentationTreeConstruction.model.DecompositionList;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;

import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * - compute fragmentation graphs in parallel
 * - compute heuristics in parallel
 * - use heuristics to estimate subset of molecular formulas to compute exactly
 * - (optional) compute trees in parallel if Gurobi is used
 */
public class FasterMultithreadedTreeComputation {

    /*
    I recommend to use queues whenever possible to schedule the jobs.
    Blocking Queues allow to limit the number of instances that can be enqueued at the same
    time (and therefore, avoid memory overflows)
     */
    private final BlockingQueue<MolecularFormula> graphsToCompute;
    private final BlockingQueue<FGraph> treesToCompute;

    /*
    You can use TreeMaps to store your heuristical or exact computed trees
     */
    private final TreeMap<MolecularFormula, FTree> map;

    /*
    The underlying framework
     */
    private final FragmentationPatternAnalysis analyzer;

    /*
    The actual input data
     */
    private ProcessedInput input;
    private HashMap<MolecularFormula, Scored<MolecularFormula>> formulasToConsider;

    public FasterMultithreadedTreeComputation(FragmentationPatternAnalysis analyzer) {
        this.graphsToCompute = new ArrayBlockingQueue<>(1000);
        this.treesToCompute = new ArrayBlockingQueue<>(10);
        this.map = new TreeMap<>();
        this.analyzer = analyzer;
    }

    public void setInput(ProcessedInput input) {
        setInput(input,null);
    }

    public void setInput(ProcessedInput input, HashSet<MolecularFormula> formulas) {
        this.input = input;
        this.formulasToConsider = new HashMap<>();
        for (Scored<MolecularFormula> formula : input.getPeakAnnotationOrThrow(DecompositionList.class).get(input.getParentPeak()).getDecompositions()) {
            if (formulas==null || formulas.contains(formula.getCandidate()))
                formulasToConsider.put(formula.getCandidate(), formula);
        }
    }

    /**
     * start multi-threaded computation
     */
    public void startComputation() {

        for (MolecularFormula formula : formulasToConsider.keySet()) {

        }

        System.out.println(getScore(computeTreeExactly(computeGraph(MolecularFormula.parse("C20H17NO6")))));
    }


    /*
    some helper functions you might need
     */

    private double getScore(FTree tree) {
        return tree.getAnnotationOrThrow(TreeScoring.class).getOverallScore();
    }

    private synchronized FTree computeTreeExactly(FGraph graph) {
        return analyzer.computeTree(graph);
    }

    private FGraph computeGraph(MolecularFormula formula) {
        return analyzer.buildGraph(input, formulasToConsider.get(formula));
    }

    private FTree computeTreeHeuristically(FGraph graph) {
        return new CriticalPathSolver(graph).solve();
    }







}
