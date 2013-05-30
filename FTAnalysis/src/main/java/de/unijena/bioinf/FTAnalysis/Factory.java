package de.unijena.bioinf.FTAnalysis;


import de.unijena.bioinf.ChemistryBase.chem.utils.scoring.ChemicalCompoundScorer;
import de.unijena.bioinf.ChemistryBase.math.ExponentialDistribution;
import de.unijena.bioinf.ChemistryBase.math.LogNormalDistribution;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.LimitNumberOfPeaksFilter;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.NoiseThresholdFilter;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.NormalizeToSumPreprocessor;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.merging.HighIntensityMerger;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.*;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp.GurobiSolver;

import java.util.ArrayList;
import java.util.List;

public class Factory {

    public FragmentationPatternAnalysis getBlankAnalysis() {
        final FragmentationPatternAnalysis analysis = new FragmentationPatternAnalysis();
        analysis.setToSirius();
        analysis.getPreprocessors().add(new NormalizeToSumPreprocessor());
        final List<LossScorer> lossScorers = new ArrayList<LossScorer>();
        lossScorers.add(CommonLossEdgeScorer.getDefaultCommonLossScorer(1d, 0.01).recombinate(3).merge(CommonLossEdgeScorer.getDefaultUnplausibleLossScorer(Math.log(0.1))));
        lossScorers.add(FreeRadicalEdgeScorer.getRadicalScorerWithDefaultSet(Math.log(0.9), Math.log(0.1), 0d));
        lossScorers.add(new DBELossScorer());
        analysis.setLossScorers(lossScorers);
        analysis.setPeakMerger(new HighIntensityMerger(0.05d));
        analysis.getPostProcessors().add(new NoiseThresholdFilter(0.01d));
        analysis.getPostProcessors().add(new LimitNumberOfPeaksFilter(Integer.MAX_VALUE));
        analysis.setTreeBuilder(new GurobiSolver());
        getByClassName(PeakIsNoiseScorer.class, analysis.getFragmentPeakScorers()).setDistribution(ExponentialDistribution.fromLambda(8));
        final GurobiSolver solver = new GurobiSolver();
        solver.setNumberOfCPUs(FTAnalysis.NUMBEROFCPUS);
        analysis.setTreeBuilder(solver);
        return analysis;
    }

    public FragmentationPatternAnalysis getBlankAnalysisWithFragPriors() {
        final FragmentationPatternAnalysis analysis = new FragmentationPatternAnalysis();
        analysis.setToSirius();
        analysis.getPreprocessors().add(new NormalizeToSumPreprocessor());
        final List<LossScorer> lossScorers = new ArrayList<LossScorer>();
        lossScorers.add(CommonLossEdgeScorer.getDefaultCommonLossScorer(1d, 0.01).recombinate(3).merge(CommonLossEdgeScorer.getDefaultUnplausibleLossScorer(Math.log(0.1))));
        lossScorers.add(FreeRadicalEdgeScorer.getRadicalScorerWithDefaultSet(Math.log(0.9), Math.log(0.1), 0d));
        lossScorers.add(new DBELossScorer());

        analysis.getDecompositionScorers().add(new ChemicalPriorScorer(ChemicalCompoundScorer.createDefaultCompoundScorer(true),
                ChemicalPriorScorer.LEARNED_NORMALIZATION_CONSTANT, 100d)
        );
        analysis.getDecompositionScorers().add(CommonFragmentsScore.getLearnedCommonFragmentScorerThatCompensateChemicalPrior());

        analysis.setLossScorers(lossScorers);
        analysis.setPeakMerger(new HighIntensityMerger(0.05d));
        analysis.getPostProcessors().add(new NoiseThresholdFilter(0.01d));
        analysis.getPostProcessors().add(new LimitNumberOfPeaksFilter(Integer.MAX_VALUE));
        analysis.setTreeBuilder(new GurobiSolver());
        getByClassName(PeakIsNoiseScorer.class, analysis.getFragmentPeakScorers()).setDistribution(ExponentialDistribution.fromLambda(8));
        final GurobiSolver solver = new GurobiSolver();
        solver.setNumberOfCPUs(FTAnalysis.NUMBEROFCPUS);
        analysis.setTreeBuilder(solver);
        return analysis;
    }

    public FragmentationPatternAnalysis getAnalysisWithCommonFragmentsAndBetterRadicalScorer() {
        final FragmentationPatternAnalysis analysis = new FragmentationPatternAnalysis();
        analysis.setToSirius();
        analysis.getPreprocessors().add(new NormalizeToSumPreprocessor());
        final List<LossScorer> lossScorers = new ArrayList<LossScorer>();
        lossScorers.add(CommonLossEdgeScorer.getDefaultCommonLossScorer(1d, 0.01).recombinate(3).merge(CommonLossEdgeScorer.getDefaultUnplausibleLossScorer(Math.log(0.1))));
        lossScorers.add(FreeRadicalEdgeScorer.getRadicalScorerWithDefaultSet());
        lossScorers.add(new DBELossScorer());
        lossScorers.add(new PureCarbonNitrogenLossScorer());

        analysis.getDecompositionScorers().add(new ChemicalPriorScorer(ChemicalCompoundScorer.createDefaultCompoundScorer(true),
                ChemicalPriorScorer.LEARNED_NORMALIZATION_CONSTANT, 100d)
        );
        analysis.getDecompositionScorers().add(CommonFragmentsScore.getLearnedCommonFragmentScorer());

        analysis.setLossScorers(lossScorers);
        analysis.setPeakMerger(new HighIntensityMerger(0.05d));
        analysis.getPostProcessors().add(new NoiseThresholdFilter(0.01d));
        analysis.getPostProcessors().add(new LimitNumberOfPeaksFilter(Integer.MAX_VALUE));
        analysis.setTreeBuilder(new GurobiSolver());
        getByClassName(PeakIsNoiseScorer.class, analysis.getFragmentPeakScorers()).setDistribution(ExponentialDistribution.fromLambda(12));
        final GurobiSolver solver = new GurobiSolver();
        solver.setNumberOfCPUs(FTAnalysis.NUMBEROFCPUS);
        analysis.setTreeBuilder(solver);
        return analysis;
    }

    public FragmentationPatternAnalysis getAnalysisWithoutCommonLosses() {
        final FragmentationPatternAnalysis analysis = new FragmentationPatternAnalysis();
        analysis.setToSirius();
        analysis.getPreprocessors().add(new NormalizeToSumPreprocessor());
        final List<LossScorer> lossScorers = new ArrayList<LossScorer>();
        lossScorers.add(CommonLossEdgeScorer.getDefaultUnplausibleLossScorer(Math.log(0.1)));
        lossScorers.add(FreeRadicalEdgeScorer.getRadicalScorerWithDefaultSet());
        lossScorers.add(new DBELossScorer());
        lossScorers.add(new PureCarbonNitrogenLossScorer());

        final List<PeakPairScorer> peakPairScorers = new ArrayList<PeakPairScorer>();
        peakPairScorers.add(new CollisionEnergyEdgeScorer(0.1, 0.8));
        peakPairScorers.add(new LossSizeScorer(new LogNormalDistribution(3.4484318558075935d, 1.070374352318858d), -7.076877716754365d));
        analysis.setPeakPairScorers(peakPairScorers);

        analysis.getDecompositionScorers().add(new ChemicalPriorScorer(ChemicalCompoundScorer.createDefaultCompoundScorer(true),
                ChemicalPriorScorer.LEARNED_NORMALIZATION_CONSTANT, 100d)
        );
        analysis.getDecompositionScorers().add(CommonFragmentsScore.getLearnedCommonFragmentScorer());

        analysis.setLossScorers(lossScorers);
        analysis.setPeakMerger(new HighIntensityMerger(0.05d));
        analysis.getPostProcessors().add(new NoiseThresholdFilter(0.01d));
        analysis.getPostProcessors().add(new LimitNumberOfPeaksFilter(Integer.MAX_VALUE));
        analysis.setTreeBuilder(new GurobiSolver());
        getByClassName(PeakIsNoiseScorer.class, analysis.getFragmentPeakScorers()).setDistribution(ExponentialDistribution.fromLambda(12));
        final GurobiSolver solver = new GurobiSolver();
        solver.setNumberOfCPUs(FTAnalysis.NUMBEROFCPUS);
        analysis.setTreeBuilder(solver);
        return analysis;
    }

    private static <S, T extends S> T getByClassName(Class<T> klass, List<S> list) {
        for (S elem : list) if (elem.getClass().equals(klass)) return (T)elem;
        return null;
    }

}
