package de.unijena.bioinf.FTAnalysis;


import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.MolecularFormulaScorer;
import de.unijena.bioinf.ChemistryBase.chem.utils.scoring.ChemicalCompoundScorer;
import de.unijena.bioinf.ChemistryBase.math.ExponentialDistribution;
import de.unijena.bioinf.ChemistryBase.math.LogNormalDistribution;
import de.unijena.bioinf.ChemistryBase.math.MathUtils;
import de.unijena.bioinf.ChemistryBase.math.ParetoDistribution;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.LimitNumberOfPeaksFilter;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.NoiseThresholdFilter;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.NormalizeToSumPreprocessor;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.inputValidator.MissingValueValidator;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.merging.HighIntensityMerger;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.*;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.legacy.CommonLossEdgeScorer;
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
        analysis.setPeakMerger(new HighIntensityMerger(0.01d));
        analysis.getPostProcessors().add(new NoiseThresholdFilter(0.005d));
        analysis.getPostProcessors().add(new LimitNumberOfPeaksFilter(Integer.MAX_VALUE));
        analysis.setTreeBuilder(new GurobiSolver());
        getByClassName(PeakIsNoiseScorer.class, analysis.getFragmentPeakScorers()).setDistribution(ExponentialDistribution.fromLambda(12));
        final GurobiSolver solver = new GurobiSolver();
        solver.setNumberOfCPUs(FTAnalysis.NUMBEROFCPUS);
        analysis.setTreeBuilder(solver);
        return analysis;
    }

    public FragmentationPatternAnalysis getAnalysisStart(boolean isAgilent) {
        final FragmentationPatternAnalysis analysis = new FragmentationPatternAnalysis();
        analysis.setToSirius();
        analysis.getPreprocessors().add(new NormalizeToSumPreprocessor());
        final List<LossScorer> lossScorers = new ArrayList<LossScorer>();
        lossScorers.add(FreeRadicalEdgeScorer.getRadicalScorerWithDefaultSet());
        lossScorers.add(new DBELossScorer());
        lossScorers.add(new PureCarbonNitrogenLossScorer());

        //lossScorers.add(new StrangeElementScorer());
        final LossSizeScorer ls = new LossSizeScorer(LogNormalDistribution.withMeanAndSd(3.4484318558075935d, 1.070374352318858d), -4.909082669257325d);
        //lossScorers.add(CommonLossEdgeScorer.getDefaultUnplausibleLossScorer(Math.log(0.1)));
        lossScorers.add(CommonLossEdgeScorer.getAlesListScorer(ls).recombinate(1, ls).merge(CommonLossEdgeScorer.getDefaultUnplausibleLossScorer(Math.log(0.1))));

        final List<PeakPairScorer> peakPairScorers = new ArrayList<PeakPairScorer>();
        peakPairScorers.add(new CollisionEnergyEdgeScorer(0.1, 0.8));


        //peakPairScorers.add(new LossSizeScorer(LogNormalDistribution.withMeanAndSd(3.8656329978554234d, 0.5512475076353699d), -4.355600412857635d));
        peakPairScorers.add(ls);

        //final double lambda = (isAgilent ? 4.492752d : 0.7507144d);
        final double massDev = (isAgilent ? 3 : 3);

        final double lambda = (isAgilent ? 1.016169293d : 0.824447576d);

        getByClassName(MassDeviationVertexScorer.class, analysis.getDecompositionScorers()).setMassPenalty(massDev);

        analysis.setPeakPairScorers(peakPairScorers);

        analysis.getDecompositionScorers().add(new ChemicalPriorScorer(ChemicalCompoundScorer.createDefaultCompoundScorer(true),
                ChemicalPriorScorer.LEARNED_NORMALIZATION_CONSTANT, 100d)
        );
        analysis.getDecompositionScorers().add(CommonFragmentsScore.getLearnedCommonFragmentScorer());
        analysis.getFragmentPeakScorers().add(new TreeSizeScorer(0d));

        analysis.setLossScorers(lossScorers);
        analysis.setPeakMerger(new HighIntensityMerger(0.01d));
        analysis.getPostProcessors().add(new NoiseThresholdFilter(0.005d));
        analysis.getPostProcessors().add(new LimitNumberOfPeaksFilter(50));
        analysis.setTreeBuilder(new GurobiSolver());
        getByClassName(PeakIsNoiseScorer.class, analysis.getFragmentPeakScorers()).setDistribution(new ParetoDistribution(lambda, 0.005d));
        final GurobiSolver solver = new GurobiSolver();
        solver.setNumberOfCPUs(FTAnalysis.NUMBEROFCPUS);
        analysis.setTreeBuilder(solver);
        return analysis;
    }

    public FragmentationPatternAnalysis oldSiriusVersion(boolean isAgilent) {
        final MolecularFormulaScorer h2cScorer = new MolecularFormulaScorer() {
            @Override
            public double score(MolecularFormula formula) {
                return Math.log(MathUtils.pdf(formula.hetero2CarbonRatio(), 0.5886335, 0.5550574));
            }
        };
        final FragmentationPatternAnalysis analysis = new FragmentationPatternAnalysis();
        analysis.setInitial();
        analysis.getPreprocessors().add(new NormalizeToSumPreprocessor());
        analysis.getInputValidators().add(new MissingValueValidator());
        analysis.getDecompositionScorers().add(new MassDeviationVertexScorer(false));
        analysis.getRootScorers().add(new MassDeviationVertexScorer(true));
        analysis.getRootScorers().add(new ChemicalPriorScorer(h2cScorer,0d));
        analysis.getFragmentPeakScorers().add(new PeakIsNoiseScorer(4));
        analysis.getLossScorers().add(CommonLossEdgeScorer.getDefaultCommonLossScorer(1).recombinate(3).merge(CommonLossEdgeScorer.getDefaultUnplausibleLossScorer(Math.log(0.1))));
        analysis.getLossScorers().add(FreeRadicalEdgeScorer.getRadicalScorerWithDefaultSet());
        analysis.getLossScorers().add(new DBELossScorer());
        analysis.getLossScorers().add(new PureCarbonNitrogenLossScorer());
        analysis.getLossScorers().add(new ChemicalPriorEdgeScorer(h2cScorer, 0d));
        analysis.getPeakPairScorers().add(new CollisionEnergyEdgeScorer(0.1, 0.8));
        analysis.getPeakPairScorers().add(new RelativeLossSizeScorer());
        analysis.getPostProcessors().add(new NoiseThresholdFilter(0.01d));

        analysis.getPostProcessors().add(new LimitNumberOfPeaksFilter(50));

        return analysis;
    }

    public FragmentationPatternAnalysis oldSiriusVersionWithPareto(boolean isAgilent) {
        final MolecularFormulaScorer h2cScorer = new MolecularFormulaScorer() {
            @Override
            public double score(MolecularFormula formula) {
                return Math.log(MathUtils.pdf(formula.hetero2CarbonRatio(), 0.5886335, 0.5550574));
            }
        };
        final FragmentationPatternAnalysis analysis = new FragmentationPatternAnalysis();
        analysis.setInitial();
        final double lambda = (isAgilent ? 1.016169293d : 0.824447576d);
        analysis.getPreprocessors().add(new NormalizeToSumPreprocessor());
        analysis.getInputValidators().add(new MissingValueValidator());
        analysis.getDecompositionScorers().add(new MassDeviationVertexScorer(false));
        analysis.getRootScorers().add(new MassDeviationVertexScorer(true));
        analysis.getRootScorers().add(new ChemicalPriorScorer(h2cScorer,0d));
        analysis.getFragmentPeakScorers().add(new PeakIsNoiseScorer(4));
        analysis.getLossScorers().add(CommonLossEdgeScorer.getDefaultCommonLossScorer(1).recombinate(3).merge(CommonLossEdgeScorer.getDefaultUnplausibleLossScorer(Math.log(0.1))));
        analysis.getLossScorers().add(FreeRadicalEdgeScorer.getRadicalScorerWithDefaultSet());
        analysis.getLossScorers().add(new DBELossScorer());
        analysis.getLossScorers().add(new PureCarbonNitrogenLossScorer());
        analysis.getLossScorers().add(new ChemicalPriorEdgeScorer(h2cScorer, 0d));
        analysis.getPeakPairScorers().add(new CollisionEnergyEdgeScorer(0.1, 0.8));
        analysis.getPeakPairScorers().add(new RelativeLossSizeScorer());
        analysis.getPostProcessors().add(new NoiseThresholdFilter(0.01d));

        analysis.getPostProcessors().add(new LimitNumberOfPeaksFilter(50));
        getByClassName(PeakIsNoiseScorer.class, analysis.getFragmentPeakScorers()).setDistribution(new ParetoDistribution(lambda, 0.005d));
        return analysis;
    }

    public FragmentationPatternAnalysis getAnalysisForTraining(boolean isAgilent) {
        final FragmentationPatternAnalysis analysis = new FragmentationPatternAnalysis();
        analysis.setToSirius();
        analysis.getPreprocessors().add(new NormalizeToSumPreprocessor());
        final List<LossScorer> lossScorers = new ArrayList<LossScorer>();
        lossScorers.add(FreeRadicalEdgeScorer.getRadicalScorerWithDefaultSet());
        lossScorers.add(new DBELossScorer());
        lossScorers.add(new PureCarbonNitrogenLossScorer());

        lossScorers.add(new StrangeElementScorer());

        //lossScorers.add(CommonLossEdgeScorer.getDefaultUnplausibleLossScorer(Math.log(0.1)));
        lossScorers.add(CommonLossEdgeScorer.getOptimizedCommonLossScorer().slightlyFavourAlesList().recombinateSpec(2, new StrangeElementScorer()).merge(CommonLossEdgeScorer.getDefaultUnplausibleLossScorer(Math.log(0.1))));

        final List<PeakPairScorer> peakPairScorers = new ArrayList<PeakPairScorer>();
        peakPairScorers.add(new CollisionEnergyEdgeScorer(0.1, 0.8));


        //peakPairScorers.add(new LossSizeScorer(LogNormalDistribution.withMeanAndSd(3.8656329978554234d, 0.5512475076353699d), -4.355600412857635d));
        peakPairScorers.add(new LossSizeScorer(LossSizeScorer.LEARNED_DISTRIBUTION, LossSizeScorer.LEARNED_NORMALIZATION));

        final double lambda = (isAgilent ? 1.016169293d : 0.824447576d);
        final double massDev = (isAgilent ? 3 : 3);

        getByClassName(MassDeviationVertexScorer.class, analysis.getDecompositionScorers()).setMassPenalty(massDev);

        analysis.setPeakPairScorers(peakPairScorers);

        analysis.getDecompositionScorers().add(new ChemicalPriorScorer(ChemicalCompoundScorer.createDefaultCompoundScorer(true),
                ChemicalPriorScorer.LEARNED_NORMALIZATION_CONSTANT, 100d)
        );
        analysis.getDecompositionScorers().add(CommonFragmentsScore.getLearnedCommonFragmentScorer());
        analysis.getFragmentPeakScorers().add(new TreeSizeScorer(1));

        analysis.setLossScorers(lossScorers);
        analysis.setPeakMerger(new HighIntensityMerger(0.01d));
        analysis.getPostProcessors().add(new NoiseThresholdFilter(0.01d));
        analysis.getPostProcessors().add(new LimitNumberOfPeaksFilter(50));
        analysis.setTreeBuilder(new GurobiSolver());
        getByClassName(PeakIsNoiseScorer.class, analysis.getFragmentPeakScorers()).setDistribution(new ParetoDistribution(lambda, 0.005d));
        final GurobiSolver solver = new GurobiSolver();
        solver.setNumberOfCPUs(FTAnalysis.NUMBEROFCPUS);
        analysis.setTreeBuilder(solver);
        return analysis;
    }

    public FragmentationPatternAnalysis getAnalysisWithCommonLossesLowTs(boolean isAgilent) {
        final FragmentationPatternAnalysis analysis = new FragmentationPatternAnalysis();
        analysis.setToSirius();
        analysis.getPreprocessors().add(new NormalizeToSumPreprocessor());
        final List<LossScorer> lossScorers = new ArrayList<LossScorer>();
        lossScorers.add(FreeRadicalEdgeScorer.getRadicalScorerWithDefaultSet());
        lossScorers.add(new DBELossScorer());
        lossScorers.add(new PureCarbonNitrogenLossScorer());

        lossScorers.add(new StrangeElementScorer());

        //lossScorers.add(CommonLossEdgeScorer.getDefaultUnplausibleLossScorer(Math.log(0.1)));
        lossScorers.add(CommonLossEdgeScorer.getOptimizedCommonLossScorer().recombinateSpec(2, new StrangeElementScorer()).merge(CommonLossEdgeScorer.getDefaultUnplausibleLossScorer(Math.log(0.1))));

        final List<PeakPairScorer> peakPairScorers = new ArrayList<PeakPairScorer>();
        peakPairScorers.add(new CollisionEnergyEdgeScorer(0.1, 0.8));


        //peakPairScorers.add(new LossSizeScorer(LogNormalDistribution.withMeanAndSd(3.8656329978554234d, 0.5512475076353699d), -4.355600412857635d));
        peakPairScorers.add(new LossSizeScorer(LossSizeScorer.LEARNED_DISTRIBUTION, LossSizeScorer.LEARNED_NORMALIZATION));

        final double lambda = (isAgilent ? 1.016169293d : 0.824447576d);
        final double massDev = (isAgilent ? 5 : 3.5);

        getByClassName(MassDeviationVertexScorer.class, analysis.getDecompositionScorers()).setMassPenalty(massDev);

        analysis.setPeakPairScorers(peakPairScorers);

        analysis.getDecompositionScorers().add(new ChemicalPriorScorer(ChemicalCompoundScorer.createDefaultCompoundScorer(true),
                ChemicalPriorScorer.LEARNED_NORMALIZATION_CONSTANT, 100d)
        );
        analysis.getDecompositionScorers().add(CommonFragmentsScore.getLearnedCommonFragmentScorer());
        analysis.getFragmentPeakScorers().add(new TreeSizeScorer(CommonLossEdgeScorer.OPTIMIZED_NORMALIZATION));

        analysis.setLossScorers(lossScorers);
        analysis.setPeakMerger(new HighIntensityMerger(0.01d));
        analysis.getPostProcessors().add(new NoiseThresholdFilter(0.005d));
        analysis.getPostProcessors().add(new LimitNumberOfPeaksFilter(50));
        analysis.setTreeBuilder(new GurobiSolver());
        getByClassName(PeakIsNoiseScorer.class, analysis.getFragmentPeakScorers()).setDistribution(new ParetoDistribution(lambda, 0.005d));
        final GurobiSolver solver = new GurobiSolver();
        solver.setNumberOfCPUs(FTAnalysis.NUMBEROFCPUS);
        analysis.setTreeBuilder(solver);
        return analysis;
    }

    public static <S, T extends S> T getByClassName(Class<T> klass, List<S> list) {
        for (S elem : list) if (elem.getClass().equals(klass)) return (T)elem;
        return null;
    }

}
