package de.unijena.bioinf.FragmentationTreeConstruction.graph.format;


import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.DecompositionScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.LossScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.MassDeviationVertexScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.PeakPairScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.legacy.*;
import de.unijena.bioinf.FragmentationTreeConstruction.model.FragmentationTree;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;
import de.unijena.bioinf.FragmentationTreeConstruction.model.TreeFragment;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScoringFormatter implements VertexFormatter<TreeFragment> {

    public transient static DecimalFormat scoreFormatter = new DecimalFormat("###0.0##");

    protected final List<DecompositionScorer> rootScorers;
    protected final List<DecompositionScorer> fragmentScorers;
    protected final List<PeakPairScorer> ms2Scores;
    protected final List<LossScorer> edgeScorers;
    protected List<double[][]> ms2Scorings;
    protected FragmentationTree tree;
    protected Object[] rootPrecomputeds, fragmentPrecomputeds, edgePrecomputeds;
    protected final List<String> rootScoreNames, fragmenScoreNames, edgeScoreNames, ms2ScoreNames;

    public ScoringFormatter(DecompositionScorer rootScorer, DecompositionScorer fragmentScorer, LossScorer edgeScorer) {
        this(rootScorer, fragmentScorer, edgeScorer, null, null);
    }

    public ScoringFormatter(DecompositionScorer rootScorer, DecompositionScorer fragmentScorer, LossScorer edgeScorer, PeakPairScorer ms2Scorer, FragmentationTree tree) {
        this.rootScorers = (rootScorer instanceof VertexScoreList)   ? ((VertexScoreList)rootScorer).getScorers()
                                                                    : Collections.singletonList(rootScorer);
        this.fragmentScorers = (fragmentScorer instanceof VertexScoreList)   ? ((VertexScoreList)fragmentScorer).getScorers()
                                                                            : Collections.singletonList(fragmentScorer);
        this.edgeScorers = (edgeScorer instanceof EdgeScoreList)   ? ((EdgeScoreList)edgeScorer).getScorers()
                                                                      : Collections.singletonList(edgeScorer);
        ms2Scores = (ms2Scorer instanceof PeakPairScoreList) ? ((PeakPairScoreList)ms2Scorer).getScorers()
                : Collections.singletonList(ms2Scorer);
        this.rootScoreNames = getScoreNames(rootScorers);
        this.fragmenScoreNames = getScoreNames(fragmentScorers);
        this.edgeScoreNames = getScoreNames(edgeScorers);
        this.ms2ScoreNames = getScoreNames(ms2Scores);
        if (tree != null) setTree(tree);
    }

    private static List<String> getScoreNames(List<? extends Object> objects) {
        final ArrayList<String> names = new ArrayList<String>(objects.size());
        for (Object o : objects) {
            final Class<?> klass = o.getClass();
            final ScoreName name = (klass.getAnnotation(ScoreName.class));
            if (name != null) {
                names.add(name.value());
            } else {
                names.add(klass.getSimpleName());
            }
        }
        return names;
    }

    public void setTree(FragmentationTree tree) {
        this.tree = tree;
        rootPrecomputeds = new Object[rootScoreNames.size()];
        for (int i=0; i < rootScoreNames.size(); ++i) {
            rootPrecomputeds[i] = (rootScorers.get(i) instanceof MassDeviationVertexScorer) ? null : rootScorers.get(i).prepare(tree.getInput());
        }
        fragmentPrecomputeds = new Object[fragmenScoreNames.size()];
        for (int i=0; i < fragmentScorers.size(); ++i) {
            fragmentPrecomputeds[i] = (fragmentScorers.get(i) instanceof MassDeviationVertexScorer) ? null : fragmentScorers.get(i).prepare(tree.getInput());
        }
        edgePrecomputeds = new Object[edgeScoreNames.size()];
        for (int i=0; i < edgeScorers.size(); ++i) {
            edgePrecomputeds[i] = edgeScorers.get(i).prepare(tree.getInput(), tree);
        }
        ms2Scorings = new ArrayList<double[][]>(ms2Scores.size());
        final ArrayList<ProcessedPeak> peaks = new ArrayList<ProcessedPeak>(tree.getInput().getMergedPeaks());
        for (int i=0; i< ms2ScoreNames.size(); ++i) {
            final double[][] scores = new double[peaks.size()][peaks.size()];
            ms2Scores.get(i).score(peaks, tree.getInput(), scores);
            ms2Scorings.add(scores);
        }
    }

    @Override
    public String format(TreeFragment vertex) {
        final StringBuilder buffer = new StringBuilder();
        if (vertex.isRoot()) {
            for (int i=0; i < rootScorers.size(); ++i) {
                if (rootScorers.get(i) instanceof MassDeviationVertexScorer) {
                    buffer.append(massdev(vertex, rootScorers.get(i)));
                } else {
                    final double score = rootScorers.get(i).score(vertex.getDecomposition().getFormula(), vertex.getPeak(), tree.getInput(), rootPrecomputeds[i]);
                    buffer.append(rootScoreNames.get(i) + ": " + scoreFormatter.format(score));
                    if (i+1 < rootScorers.size()) buffer.append("\\n");
                }
            }
            return buffer.toString();
        } else {
            for (int i=0; i < fragmentScorers.size(); ++i) {
                if (fragmentScorers.get(i) instanceof MassDeviationVertexScorer) {
                    buffer.append(massdev(vertex, fragmentScorers.get(i)));
                } else {
                    final double score = fragmentScorers.get(i).score(vertex.getDecomposition().getFormula(), vertex.getPeak(), tree.getInput(), fragmentPrecomputeds[i]);
                    buffer.append(fragmenScoreNames.get(i) + ": " + scoreFormatter.format(score) + "\\n");
                }
            }
            for (int i=0; i < edgeScorers.size(); ++i) {
                final double score = edgeScorers.get(i).score(vertex.getParentEdge(), tree.getInput(), edgePrecomputeds[i]);
                buffer.append(edgeScoreNames.get(i) + ": " + scoreFormatter.format(score));
                buffer.append("\\n");
            }
            for (int i=0; i < ms2Scores.size(); ++i) {
                final double score = ms2Scorings.get(i)[vertex.getParentEdge().getHead().getPeak().getIndex()][vertex.getPeak().getIndex()];
                buffer.append(ms2ScoreNames.get(i) + ": " + scoreFormatter.format(score));
                if (i+1 < ms2ScoreNames.size()) buffer.append("\\n");
            }
            return buffer.toString();
        }
    }

    private String massdev(TreeFragment vertex, DecompositionScorer scorer) {
        final MassDeviationVertexScorer massdev = (MassDeviationVertexScorer)scorer;
        final double decscore = massdev.computeScore(vertex.getDecomposition().getFormula(), vertex.getPeak(), 0, tree.getInput());
        return scoreFormatter.format(decscore) + " + " +  scoreFormatter.format(massdev.getLambda()*vertex.getPeak().getRelativeIntensity()) + "\\n";
    }
}
