package de.unijena.bioinf.sirius;

import de.unijena.bioinf.ChemistryBase.ms.ft.*;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.sirius.plugins.IsotopePatternInMs1Plugin;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.sirius.scores.IsotopeScore;
import de.unijena.bioinf.sirius.scores.SiriusScore;
import de.unijena.bioinf.sirius.scores.TreeScore;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class FTreeMetricsHelper {
    protected final FTree tree;
    protected final FragmentAnnotation<Score> fragmentScoring;
    protected final LossAnnotation<Score> lossScoring;

    public FTreeMetricsHelper(FTree tree) {
        this.tree = tree;
        this.fragmentScoring = tree.getOrCreateFragmentAnnotation(Score.class);
        this.lossScoring = tree.getOrCreateLossAnnotation(Score.class);
    }


    public double getSiriusScore() {
        return getSiriusScore(tree);
    }

    public double getTreeScore() {
        return getSiriusScore() - getIsotopeMs1Score();
    }

    public double getIsotopeMs1Score() {
            Score score = fragmentScoring.get(tree.getRoot());
            if (score == null) {
                LossAnnotation<LossType> l = tree.getLossAnnotationOrNull(LossType.class);
                if (l!=null) {
                    score = fragmentScoring.get(getMeasuredIonRoot(l,tree.getRoot()));
                }
            }
            return score.get(FragmentationPatternAnalysis.getScoringMethodName(IsotopePatternInMs1Plugin.Ms1IsotopePatternScorer.class));
    }

    // TODO: we should solve that smarter... A fragment should know if it is the measured ion. Need another annotation for that
    private Fragment getMeasuredIonRoot(LossAnnotation<LossType> lossAno, Fragment root) {
        if (root.isLeaf() || lossAno.get(root.getOutgoingEdge(0)).isRegular()) return root;
        else return getMeasuredIonRoot(lossAno, root.getChildren(0));
    }

    public double getBeautificationPenalty() {
        return fragmentScoring.get(tree.getRoot()).get(Beautified.PENALTY_KEY);
    }

    public double getRecalibrationPenalty() {
        return fragmentScoring.get(tree.getRoot()).get(Recalibrated.PENALTY_KEY);
    }


    public static double getSiriusScore(FTree tree) {
        return tree.getTreeWeight();
    }

    public static double getRootScore(@NotNull FTree tree) {
        return tree.getRootScore();
    }

    public static double getExplainedPeaksRatio(@NotNull FTree tree) {
        return tree.getAnnotationOrThrow(TreeStatistics.class).getRatioOfExplainedPeaks();
    }

    public static double getNumOfExplainedPeaks(@NotNull FTree tree) {
        return tree.numberOfVertices();
    }

    public static double getExplainedIntensityRatio(@NotNull FTree tree) {
        return tree.getAnnotationOrThrow(TreeStatistics.class).getExplainedIntensity();
    }

    public static double getNumberOfExplainablePeaks(@NotNull FTree tree) {
        return getNumOfExplainedPeaks(tree) / getExplainedPeaksRatio(tree);
    }

    public static Set<FormulaScore> getScoresFromTree(@NotNull final FTree tree) {
        final FTreeMetricsHelper helper = new FTreeMetricsHelper(tree);
        final Set<FormulaScore> scores = new HashSet<>(3);
        scores.add(new SiriusScore(helper.getSiriusScore()));
        try {
            scores.add(new IsotopeScore(helper.getIsotopeMs1Score()));
            scores.add(new TreeScore(helper.getTreeScore()));
        } catch (Throwable e) {
            System.out.println("DEBUG: Something with this tree is wrong? Cannot calculate isotope score" + tree.getRoot().getFormula().toString());
            e.printStackTrace();
        }


        return scores;
    }
}
