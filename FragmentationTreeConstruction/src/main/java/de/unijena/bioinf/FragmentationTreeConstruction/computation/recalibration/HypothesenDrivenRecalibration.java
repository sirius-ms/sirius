package de.unijena.bioinf.FragmentationTreeConstruction.computation.recalibration;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.math.NormalDistribution;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.MassDeviationVertexScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.model.FragmentationTree;
import de.unijena.bioinf.FragmentationTreeConstruction.model.TreeFragment;
import org.apache.commons.math3.analysis.UnivariateFunction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class HypothesenDrivenRecalibration implements RecalibrationMethod {

    private SpectrumRecalibration method;

    public HypothesenDrivenRecalibration() {
        this.method = new MedianSlope();
    }

    public HypothesenDrivenRecalibration(SpectrumRecalibration recalibrationMethod) {
        this.method = recalibrationMethod;
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
                return null;  //TODO: implement
            }

            private void calculateScoreBonus() {
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
                recomputeTree = (avgDist > 0.0005d);
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
        method = (SpectrumRecalibration) helper.unwrap(document, document.getFromDictionary(dictionary, "method"));
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "method", helper.wrap(document, method));
    }
}
