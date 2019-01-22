package de.unijena.bioinf.FragmentationTreeConstruction.inspection;

import de.unijena.bioinf.ChemistryBase.ms.ft.*;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.IsotopeMs1Scorer;

public class ScoringHelper {


    protected final FTree tree;
    protected final FragmentAnnotation<Score> fragmentScoring;
    protected final LossAnnotation<Score> lossScoring;

    public ScoringHelper(FTree tree) {
        this.tree = tree;
        this.fragmentScoring = tree.getOrCreateFragmentAnnotation(Score.class);
        this.lossScoring = tree.getOrCreateLossAnnotation(Score.class);
    }

    public double getRootScore() {
        return tree.getRootScore();
    }

    public double getTreeScore() {
        return tree.getTreeWeight();
    }

    public double getIsotopeMs1Score() {
        return fragmentScoring.get(tree.getRoot()).get(FragmentationPatternAnalysis.getScoringMethodName(IsotopeMs1Scorer.class));
    }

    public double getBeautificationPenalty() {
        return fragmentScoring.get(tree.getRoot()).get(Beautified.PENALTY_KEY);
    }

    public double getRecalibrationPenalty() {
        return fragmentScoring.get(tree.getRoot()).get(Recalibrated.PENALTY_KEY);
    }

    // ...
}
