package de.unijena.bioinf.FragmentationTree;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.ScoredMolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.TreeIterator;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.LimitNumberOfPeaksFilter;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.NoiseThresholdFilter;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.LossScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp.GurobiSolver;
import de.unijena.bioinf.FragmentationTreeConstruction.inspection.GraphOutput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.*;
import de.unijena.bioinf.babelms.GenericParser;
import de.unijena.bioinf.babelms.ms.JenaMsParser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 *
 */
public class GraphBuilder {

    public static void main(String[] args) {
        try {
            final Profile prof = new Profile("default");
            final File input = new File(args[0]);
            final FragmentationPatternAnalysis analyzer = prof.fragmentationPatternAnalysis;
            FragmentationPatternAnalysis.getOrCreateByClassName(NoiseThresholdFilter.class, analyzer.getPreprocessors()).setThreshold(0.01);
            FragmentationPatternAnalysis.getOrCreateByClassName(NoiseThresholdFilter.class, analyzer.getPostProcessors()).setThreshold(0.01);
            FragmentationPatternAnalysis.getOrCreateByClassName(LimitNumberOfPeaksFilter.class, analyzer.getPostProcessors()).setLimit(100);
            analyzer.setRecalibrationMethod(null);

            ((GurobiSolver)analyzer.getTreeBuilder()).setSecondsPerInstance(60*60*60*60*60);
            ((GurobiSolver)analyzer.getTreeBuilder()).setSecondsPerDecomposition(60*60*60*60*60);

            final Ms2Experiment experiment = new GenericParser<Ms2Experiment>(new JenaMsParser()).parseFile(input);
            final ProcessedInput pinput = analyzer.preprocessing(experiment);
            final File dir = new File(input.getName());
            if (dir.exists()) {
                for (File f : dir.listFiles()) if (f.isFile()) f.delete();
                dir.delete();
            }
            dir.mkdir();
            // 1. build a graph for each PMD
            double optScore = Double.NEGATIVE_INFINITY;
            for (ScoredMolecularFormula f : pinput.getParentMassDecompositions()) {
                final FragmentationGraph g = analyzer.buildGraph(pinput, f);
                final FragmentationTree t = null;//analyzer.computeTree(g);
                final double score = (t==null) ? f.getScore() : t.getScore();
                final double rootScore = (t==null) ? f.getScore() : t.getRootScore();
                optScore = Math.max(score, optScore);
                new GraphOutput().printToFile(g, score-rootScore, rootScore, new File(input.getName(), f.getFormula() + ".txt"));
            }
            // 2. build a combined graph
            final FragmentationGraph combinedGraph = buildGraph(analyzer, pinput);
            // print combined graph
            new GraphOutput().printToFile(combinedGraph, optScore, new File(input.getName(), "__COMBINED__" + ".txt"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }


    public static FragmentationGraph buildGraph(FragmentationPatternAnalysis analyzer, ProcessedInput input) {
        final ProcessedPeak parentPeak = input.getParentPeak();
        final FragmentationGraph graph = new FragmentationGraph(input);
        final GraphFragment pseudoRoot = graph.addVertex(parentPeak, new ScoredMolecularFormula(MolecularFormula.parse(""), 0d));
        for (ScoredMolecularFormula f : input.getParentMassDecompositions()) {
            final GraphFragment g = graph.addVertex(parentPeak, f);
            graph.addEdge(pseudoRoot, g);
        }
        final ArrayList<ProcessedPeak> peaks = new ArrayList<ProcessedPeak>(input.getMergedPeaks());
        Collections.sort(peaks, new ProcessedPeak.MassComparator());
        int n = peaks.indexOf(parentPeak);
        for (int i = n-1; i >= 0; --i) {
            final ProcessedPeak peak = peaks.get(i);
            final int pi = peak.getIndex();
            for (ScoredMolecularFormula decomposition : peak.getDecompositions()) {
                final MolecularFormula formula = decomposition.getFormula();
                List<GraphFragment> parents = new ArrayList<GraphFragment>();
                for (GraphFragment f : graph.getFragmentsWithoutRoot()) {
                    if (f.getPeak().getIndex() == pi) continue;
                    final MolecularFormula fragmentFormula = f.getDecomposition().getFormula();
                    assert  (f.getPeak().getMz() > peak.getMz());
                    if (fragmentFormula.isSubtractable(formula)) {
                        parents.add(f);
                    }
                }
                if (!parents.isEmpty()) {
                    final GraphFragment newFragment = graph.addVertex(peak, decomposition);
                    for (GraphFragment parent : parents) graph.addEdge(parent, newFragment);
                }
            }
        }

        // score graph
        final Iterator<Loss> edges = graph.lossIterator();
        final double[] peakScores = input.getPeakScores();
        final double[][] peakPairScores = input.getPeakPairScores();
        final LossScorer[] lossScorers = analyzer.getLossScorers().toArray(new LossScorer[analyzer.getLossScorers().size()]);
        final Object[] precomputeds = new Object[lossScorers.length];
        for (int i=0; i < precomputeds.length; ++i) precomputeds[i] = lossScorers[i].prepare(input);
        while (edges.hasNext()) {
            final Loss loss = edges.next();
            if (loss.getHead() == pseudoRoot) {
                loss.setWeight(loss.getTail().getDecomposition().getScore() + peakScores[peakScores.length - 1]);
            } else {
                final Fragment u = loss.getHead();
                final Fragment v = loss.getTail();
                // take score of molecular formula
                double score = v.getDecomposition().getScore();
                // add it to score of the peak
                score += peakScores[v.getPeak().getIndex()];
                // add it to the score of the peak pairs
                score += peakPairScores[u.getPeak().getIndex()][v.getPeak().getIndex()]; // TODO: Umdrehen!
                // add the score of the loss
                for (int i=0; i < lossScorers.length; ++i) score += lossScorers[i].score(loss, input, precomputeds[i]);
                loss.setWeight(score);
            }
        }
        // set root scores
        graph.setRootScore(0d);
        graph.prepareForTreeComputation();
        return graph;
    }


}
