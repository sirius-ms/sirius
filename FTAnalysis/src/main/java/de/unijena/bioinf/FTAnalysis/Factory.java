package de.unijena.bioinf.FTAnalysis;


import de.unijena.bioinf.ChemistryBase.math.ExponentialDistribution;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.NoiseThresholdFilter;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.merging.HighIntensityMerger;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.*;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp.GurobiSolver;

import java.util.ArrayList;
import java.util.List;

public class Factory {

    public FragmentationPatternAnalysis getBlankAnalysis() {
        final FragmentationPatternAnalysis analysis = new FragmentationPatternAnalysis();
        analysis.setToSirius();
        final List<LossScorer> lossScorers = new ArrayList<LossScorer>();
        lossScorers.add(CommonLossEdgeScorer.getDefaultUnplausibleLossScorer(Math.log(0.1)));
        lossScorers.add(FreeRadicalEdgeScorer.getRadicalScorerWithDefaultSet(Math.log(0.9), Math.log(0.1)));
        lossScorers.add(new DBELossScorer());
        analysis.setLossScorers(lossScorers);
        analysis.setPeakMerger(new HighIntensityMerger(0.05d));
        analysis.getPostProcessors().add(new NoiseThresholdFilter(0.01d));
        analysis.setTreeBuilder(new GurobiSolver());
        getByClassName(PeakIsNoiseScorer.class, analysis.getFragmentPeakScorers()).setDistribution(ExponentialDistribution.fromLambda(8));
        return analysis;
    }

    private static <S, T extends S> T getByClassName(Class<T> klass, List<S> list) {
        for (S elem : list) if (elem.getClass().equals(klass)) return (T)elem;
        return null;
    }

}
