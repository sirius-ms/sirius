package de.unijena.bioinf.sirius;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.ms.ft.*;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.sirius.plugins.IsotopePatternInMs1Plugin;
import de.unijena.bioinf.sirius.scores.IsotopeScore;
import de.unijena.bioinf.sirius.scores.SiriusScore;
import de.unijena.bioinf.sirius.scores.TreeScore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class FTreeMetricsHelper {
    protected final FTree tree;
    protected final FragmentAnnotation<Score> fragmentScoring;
    protected final LossAnnotation<Score> lossScoring;
    protected final Fragment measuredIonRoot;

    public FTreeMetricsHelper(FTree tree) {
        this.tree = tree;
        this.fragmentScoring = tree.getOrCreateFragmentAnnotation(Score.class);
        this.lossScoring = tree.getOrCreateLossAnnotation(Score.class);
        final LossAnnotation<LossType> l = tree.getLossAnnotationOrNull(LossType.class);
        this.measuredIonRoot = l==null ? tree.getRoot() : getMeasuredIonRoot(l, tree.getRoot());
    }

    public Optional<TreeStatistics> asTreeStats() {
        return tree.getAnnotation(TreeStatistics.class);
    }

    public double getSiriusScore() {
        return getSiriusScore(tree);
    }

    public double getTreeScore() {
        return getSiriusScore() - getIsotopeMs1Score();
    }

    public double getRootScore() {
        return getRootScore(tree);
    }

    public double getExplainedPeaksRatio() {
        return getExplainedPeaksRatio(tree);
    }

    public int getNumOfExplainedPeaks() {
        return getNumOfExplainedPeaks(tree);
    }

    public double getExplainedIntensityRatio() {
        return getExplainedIntensityRatio(tree);
    }

    public int getNumberOfExplainablePeaks() {
        return getNumberOfExplainablePeaks(tree);
    }

    public double getIsotopeMs1Score() {
            Score score = fragmentScoring.get(measuredIonRoot);
            return score.get(FragmentationPatternAnalysis.getScoringMethodName(IsotopePatternInMs1Plugin.Ms1IsotopePatternScorer.class));
    }

    // TODO: we should solve that smarter... A fragment should know if it is the measured ion. Need another annotation for that
    private Fragment getMeasuredIonRoot(LossAnnotation<LossType> lossAno, Fragment root) {
        if (root.isLeaf()) return root;
        final @Nullable LossType ano = lossAno.get(root.getOutgoingEdge(0));
        //@todo kai: how to handle null here?
        if (ano != null && ano.isRegular()) return root;
        return getMeasuredIonRoot(lossAno, root.getChildren(0));
    }

    public double getBeautificationPenalty() {
        return fragmentScoring.get(measuredIonRoot).get(Beautified.PENALTY_KEY);
    }

    public double getRecalibrationPenalty() {
        return fragmentScoring.get(measuredIonRoot).get(Recalibrated.PENALTY_KEY);
    }

    public double getMedianMassDeviation() {
        return Double.NaN;
//        FragmentAnnotation<Peak> peakAno = tree.getOrCreateFragmentAnnotation(Peak.class);
//        FragmentAnnotation<AnnotatedPeak> annoPeakAnno = ft.getFragmentAnnotationOrNull(AnnotatedPeak.class);

    }

    /*private static void calculateDeviatonMassInPpm(Fragment treeNode, Ionization ionization, PeakAnnotation<Peak> peakAno, PeakAnnotation<AnnotatedPeak> annoPeakAnno, ArrayList<Double> massDeviations) {
        final double relativToPpm = 1000 * 1000;

        final double peakMass;
        if (peakAno.get(treeNode) == null) {
            peakMass = ionization.addToMass(treeNode.getFormula().getMass());
        } else {
            if (annoPeakAnno != null)
                peakMass = annoPeakAnno.get(treeNode).getMass();
            else
                peakMass = peakAno.get(treeNode).getMass();
        }


        if (ionization != null && treeNode != null) {
            Double massDeviation = ((treeNode.getFormula().getMass() - peakMass + ionization.getMass()) / treeNode.getFormula().getMass()) * relativToPpm;
            massDeviations.add(massDeviation);
        }
    }*/


    // static helper methods
    public static double getSiriusScore(FTree tree) {
        return tree.getTreeWeight();
    }

    public static double getRootScore(@NotNull FTree tree) {
        return tree.getRootScore();
    }

    public static double getExplainedPeaksRatio(@NotNull FTree tree) {
        return tree.getAnnotationOrThrow(TreeStatistics.class).getRatioOfExplainedPeaks();
    }

    public static double getExplainedIntensityRatio(@NotNull FTree tree) {
        return tree.getAnnotationOrThrow(TreeStatistics.class).getExplainedIntensity();
    }

    public static int getNumberOfExplainablePeaks(@NotNull FTree tree) {
        final int numberOfExplainedPeaks = getNumOfExplainedPeaks(tree);
        if (numberOfExplainedPeaks==0) return 0;
        return (int)Math.ceil(getNumOfExplainedPeaks(tree) / getExplainedPeaksRatio(tree));
    }

    /**
     * number of peaks explained by FTree, ignoring the root
     * @param tree
     * @return
     */
    public static int getNumOfExplainedPeaks(@NotNull FTree tree) {
        return tree.numberOfVertices()-1;
    }





    public static Set<FormulaScore> getScoresFromTree(@NotNull final FTree tree) {
        final FTreeMetricsHelper helper = new FTreeMetricsHelper(tree);
        final Set<FormulaScore> scores = new HashSet<>(3);
        scores.add(new SiriusScore(helper.getSiriusScore()));
        try {
            scores.add(new IsotopeScore(helper.getIsotopeMs1Score()));
            scores.add(new TreeScore(helper.getTreeScore()));
        } catch (Throwable e) {
            //todo remove debug stuff
            System.out.println("DEBUG: Something with this tree is wrong? Cannot calculate isotope score" + tree.getRoot().getFormula().toString());
            e.printStackTrace();
        }


        return scores;
    }
}
