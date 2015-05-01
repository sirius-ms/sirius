package de.unijena.bioinf.FragmentationTreeConstruction.computation.recalibration;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.math.NormalDistribution;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.MutableMeasurementProfile;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FragmentAnnotation;
import de.unijena.bioinf.ChemistryBase.ms.ft.TreeScoring;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.MultipleTreeComputation;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.MassDeviationVertexScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Ms2ExperimentImpl;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.function.Identity;

import java.util.*;

/**
 * The default recalibration method:
 * - take the computed tree, calculate a reference spectrum
 * - calculate a recalibration function by some kind of regression of the reference and the measured spectrum
 * - use this recalibration function to recalibrate the tree's vertex masses and scoring function
 * - if a recalibration function exist, recalibrate the whole spectrum and compute a new tree
 */
public class HypothesenDrivenRecalibration implements RecalibrationMethod {

    private RecalibrationStrategy method;
    private double distanceThreshold;
    private double deviationScale = 1d;

    public HypothesenDrivenRecalibration() {
        this(new LeastSquare(), 0.00002d);
    }

    public HypothesenDrivenRecalibration(RecalibrationStrategy recalibrationMethod, double distanceThreshold) {
        this.method = recalibrationMethod;
        this.distanceThreshold = distanceThreshold;
    }

    public RecalibrationStrategy getMethod() {
        return method;
    }

    public void setMethod(RecalibrationStrategy method) {
        this.method = method;
    }

    public double getDeviationScale() {
        return deviationScale;
    }

    public void setDeviationScale(double deviationScale) {
        this.deviationScale = deviationScale;
    }

    public double getDistanceThreshold() {
        return distanceThreshold;
    }

    public void setDistanceThreshold(double distanceThreshold) {
        this.distanceThreshold = distanceThreshold;
    }

    @Override
    public Recalibration recalibrate(final FTree tree, final MassDeviationVertexScorer scorer, final boolean force) {
        // get peaks from tree
        final List<Fragment> fragments = new ArrayList<Fragment>(tree.getFragments());
        final FragmentAnnotation<ProcessedPeak> peakAno = tree.getFragmentAnnotationOrThrow(ProcessedPeak.class);
        Collections.sort(fragments, new Comparator<Fragment>() {
            @Override
            public int compare(Fragment o1, Fragment o2) {
                return new Double(peakAno.get(o1).getMz()).compareTo(peakAno.get(o2).getMz());
            }
        });
        final SimpleMutableSpectrum spec = new SimpleMutableSpectrum();
        final SimpleMutableSpectrum ref = new SimpleMutableSpectrum();
        final Ionization ion = tree.getAnnotationOrThrow(Ionization.class);
        for (Fragment f : fragments) {
            spec.addPeak(new Peak(peakAno.get(f).getOriginalMz(), peakAno.get(f).getRelativeIntensity()));
            ref.addPeak(new Peak(ion.addToMass(f.getFormula().getMass()), peakAno.get(f).getRelativeIntensity()));
        }
        final UnivariateFunction recalibrationFunction = method.recalibrate(spec, ref);
        return new Recalibration() {
            private double scoreBonus = Double.NaN;
            private FTree correctedTree = null;
            private boolean recomputeTree = false;

            @Override
            public double getScoreBonus() {
                if (Double.isNaN(scoreBonus)) {
                    calculateScoreBonus();
                }
                return scoreBonus;
            }

            @Override
            public FTree getCorrectedTree(FragmentationPatternAnalysis analyzer, FTree oldTree) {
                if (correctedTree != null) return correctedTree;
                else return recomputeTree(analyzer, oldTree);
            }

            @Override
            public FTree getCorrectedTree(FragmentationPatternAnalysis analyzer) {
                return getCorrectedTree(analyzer, null);
            }

            private FTree recomputeTree(FragmentationPatternAnalysis analyzer, FTree oldTree) {
                getScoreBonus();
                final UnivariateFunction f = recalibrationFunction;
                if (f instanceof Identity && !force) {
                    correctedTree = tree;
                    return tree;
                }
                /*
                final Ms2ExperimentImpl exp = new Ms2ExperimentImpl(tree.getInput().getExperimentInformation());
                final ArrayList<Ms2Spectrum> specs = new ArrayList<Ms2Spectrum>();
                for (Ms2Spectrum spec : exp.getMs2Spectra()) {
                    specs.add(new Ms2SpectrumImpl(Spectrums.map(spec, new Spectrums.Transformation<Peak, Peak>() {
                        @Override
                        public Peak transform(Peak input) {
                            return new Peak(f.value(input.getMass()), input.getIntensity());
                        }
                    }), spec.getCollisionEnergy(), spec.getPrecursorMz(), spec.getTotalIonCount()));
                }
                exp.setMs2Spectra(specs);
                final MutableMeasurementProfile prof = new MutableMeasurementProfile(exp.getMeasurementProfile());
                exp.setMeasurementProfile(prof);
                */
                final Ms2ExperimentImpl impl = new Ms2ExperimentImpl(analyzer.validate(tree.getAnnotationOrThrow(ProcessedInput.class).getOriginalInput()));
                final MutableMeasurementProfile prof = new MutableMeasurementProfile(impl.getMeasurementProfile());
                prof.setStandardMs2MassDeviation(prof.getStandardMs2MassDeviation().multiply(deviationScale));
                impl.setMeasurementProfile(prof);
                final TreeScoring treeScoring = tree.getAnnotationOrThrow(TreeScoring.class);
                ProcessedInput pinp = analyzer.preprocessingWithRecalibration(impl, this);
                MultipleTreeComputation mtc = analyzer.computeTrees(pinp).onlyWith(Arrays.asList(tree.getRoot().getFormula())).withLowerbound(force ? 0 : treeScoring.getOverallScore()).withoutRecalibration();
                if (oldTree != null) mtc = mtc.withBackbones(oldTree);
                correctedTree = mtc.optimalTree();
                if (correctedTree == null) {
                    assert !force;
                    correctedTree = tree;
                }
                if (deviationScale == 1) {
                    if (correctedTree.getAnnotationOrThrow(TreeScoring.class).getOverallScore() >= oldTree.getAnnotationOrThrow(TreeScoring.class).getOverallScore()) return correctedTree;
                    else return oldTree;
                }
                final FTree ft2 = analyzer.computeTrees(analyzer.preprocessing(impl)).onlyWith(Arrays.asList(tree.getRoot().getFormula())).withLowerbound(0/*correctedTree.getScore()*/).withoutRecalibration().withBackbones(correctedTree).optimalTree();
                if (ft2 == null) return correctedTree;
                else if (ft2.getAnnotationOrThrow(TreeScoring.class).getOverallScore() > correctedTree.getAnnotationOrThrow(TreeScoring.class).getOverallScore())
                    return ft2;
                return correctedTree;
            }

            private void calculateScoreBonus() {
                if (recalibrationFunction instanceof Identity) {
                    scoreBonus = 0d;
                    return;
                }
                final ProcessedInput input = tree.getAnnotationOrThrow(ProcessedInput.class);
                final Deviation dev = input.getExperimentInformation().getMeasurementProfile().getStandardMs2MassDeviation();
                final Ionization ion = tree.getAnnotationOrThrow(Ionization.class);
                double sc = 0d;
                double distance = 0d;
                final FragmentAnnotation<ProcessedPeak> peakAno = tree.getFragmentAnnotationOrThrow(ProcessedPeak.class);
                for (Fragment f : fragments) {
                    final double oldMz = peakAno.get(f).getOriginalMz();
                    final double newMz = recalibrationFunction.value(oldMz);
                    distance += Math.abs(newMz - oldMz);
                    final double theoreticalMz = ion.addToMass(f.getFormula().getMass());
                    final NormalDistribution dist = scorer.getDistribution(newMz, peakAno.get(f).getRelativeIntensity(), input);
                    final double newScore = Math.log(dist.getErrorProbability(newMz - theoreticalMz));
                    final double oldScore = Math.log(dist.getErrorProbability(oldMz - theoreticalMz));
                    sc += (newScore - oldScore);
                }
                this.scoreBonus = sc;
                final double avgDist = distance / fragments.size();
                recomputeTree = (avgDist >= distanceThreshold);
            }

            @Override
            public boolean shouldRecomputeTree() {
                getScoreBonus();
                return recomputeTree;
            }

            @Override
            public UnivariateFunction recalibrationFunction() {
                return recalibrationFunction;
            }
        };
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        method = (RecalibrationStrategy) helper.unwrap(document, document.getFromDictionary(dictionary, "method"));
        distanceThreshold = document.getDoubleFromDictionary(dictionary, "threshold");
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "method", helper.wrap(document, method));
        document.addToDictionary(dictionary, "threshold", distanceThreshold);
    }
}
