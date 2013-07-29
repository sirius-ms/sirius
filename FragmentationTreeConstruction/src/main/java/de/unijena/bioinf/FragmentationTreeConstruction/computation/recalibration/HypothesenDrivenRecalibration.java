package de.unijena.bioinf.FragmentationTreeConstruction.computation.recalibration;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.math.NormalDistribution;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.MutableMeasurementProfile;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.MassDeviationVertexScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.model.FragmentationTree;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Ms2ExperimentImpl;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Ms2SpectrumImpl;
import de.unijena.bioinf.FragmentationTreeConstruction.model.TreeFragment;
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

    public HypothesenDrivenRecalibration() {
        this(new MedianSlope(), 0.0002d);
    }

    public HypothesenDrivenRecalibration(RecalibrationStrategy recalibrationMethod, double distanceThreshold) {
        this.method = recalibrationMethod;
        this.distanceThreshold = distanceThreshold;
    }


    @Override
    public Recalibration recalibrate(final FragmentationTree tree, final MassDeviationVertexScorer scorer) {
        // get peaks from tree
        final List<TreeFragment> fragments = new ArrayList<TreeFragment>(tree.getFragments());
        Collections.sort(fragments, new Comparator<TreeFragment>() {
            @Override
            public int compare(TreeFragment o1, TreeFragment o2) {
                return new Double(o1.getPeak().getMz()).compareTo(o2.getPeak().getMz());
            }
        });
        final SimpleMutableSpectrum spec = new SimpleMutableSpectrum();
        final SimpleMutableSpectrum ref = new SimpleMutableSpectrum();
        final Ionization ion = tree.getIonization();
        for (TreeFragment f : fragments) {
            spec.addPeak(f.getPeak());
            ref.addPeak(new Peak(ion.addToMass(f.getDecomposition().getFormula().getMass()), f.getPeak().getRelativeIntensity()));
        }
        final UnivariateFunction recalibrationFunction = method.recalibrate(spec, ref);
        return new Recalibration() {
            private double scoreBonus = Double.NaN;
            private FragmentationTree correctedTree = null;
            private boolean recomputeTree = false;
            @Override
            public double getScoreBonus() {
                if (Double.isNaN(scoreBonus)) {
                    calculateScoreBonus();
                }
                return scoreBonus;
            }

            @Override
            public FragmentationTree getCorrectedTree(FragmentationPatternAnalysis analyzer) {
                if (correctedTree != null) return correctedTree;
                else return recomputeTree(analyzer);
            }

            private FragmentationTree recomputeTree(FragmentationPatternAnalysis analyzer) {
                getScoreBonus();
                final UnivariateFunction f = recalibrationFunction;
                if (f instanceof Identity) {
                    correctedTree = tree;
                    return tree;
                }
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
                correctedTree = analyzer.computeTrees(analyzer.preprocessing(exp)).onlyWith(Arrays.asList(tree.getRoot().getFormula())).withLowerbound(tree.getScore()).withoutRecalibration().optimalTree();
                if (correctedTree == null) correctedTree = tree;
                return correctedTree;
            }

            private void calculateScoreBonus() {
                if (recalibrationFunction instanceof Identity) {
                    scoreBonus=0d;
                    return;
                }
                final Deviation dev = tree.getInput().getExperimentInformation().getMeasurementProfile().getStandardMs2MassDeviation();
                final Ionization ion = tree.getIonization();
                double sc=0d;
                double distance = 0d;
                for (TreeFragment f : fragments) {
                    final double oldMz = f.getPeak().getMz();
                    final double newMz = recalibrationFunction.value(oldMz);
                    distance += Math.abs(newMz-oldMz);
                    final NormalDistribution dist = scorer.getDistribution(newMz, f.getRelativePeakIntensity(), tree.getInput());
                    final double newScore = Math.log(dist.getCumulativeProbability(newMz-ion.addToMass(f.getDecomposition().getFormula().getMass())));
                    final double oldScore = Math.log(dist.getCumulativeProbability(oldMz-ion.addToMass(f.getDecomposition().getFormula().getMass())));
                    sc += (newScore - oldScore);
                }
                this.scoreBonus = sc;
                final double avgDist = distance/fragments.size();
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
