/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.FragmentationTreeConstruction.computation.recalibration;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.math.NormalDistribution;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.MutableMeasurementProfile;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.ft.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.MultipleTreeComputation;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.MassDeviationVertexScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.TreeSizeScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.function.Identity;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;

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

    public static RecalibrationFunction toPolynomial(UnivariateFunction func) {
        if (func instanceof PolynomialFunction) {
            return new RecalibrationFunction(((PolynomialFunction) func).getCoefficients());
        }
        if (func instanceof Identity) return RecalibrationFunction.identity();
        return null;
    }

    @Override
    public Recalibration recalibrate(final FTree tree, final MassDeviationVertexScorer scorer, final boolean force) {
        // get peaks from tree
        final List<Fragment> fragments = new ArrayList<Fragment>(tree.getFragments());
        final FragmentAnnotation<ProcessedPeak> peakAno = tree.getFragmentAnnotationOrThrow(ProcessedPeak.class);
        Collections.sort(fragments, new Comparator<Fragment>() {
            @Override
            public int compare(Fragment o1, Fragment o2) {
                return new Double(o1.getFormula().getMass()).compareTo(o2.getFormula().getMass());
            }
        });
        final SimpleMutableSpectrum spec = new SimpleMutableSpectrum();
        final SimpleMutableSpectrum ref = new SimpleMutableSpectrum();
        final PrecursorIonType ion = tree.getAnnotationOrThrow(PrecursorIonType.class);
        for (Fragment f : fragments) {
            if (peakAno.get(f)==null) continue;
            spec.addPeak(new Peak(peakAno.get(f).getOriginalMz(), peakAno.get(f).getRelativeIntensity()));

            final double referenceMass = ion.getIonization().addToMass(f.getFormula().getMass());

            ref.addPeak(new Peak(referenceMass, peakAno.get(f).getRelativeIntensity()));
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
                final ProcessedInput originalInput = tree.getAnnotationOrThrow(ProcessedInput.class);
                final MutableMeasurementProfile prof = new MutableMeasurementProfile(originalInput.getMeasurementProfile());
                prof.setStandardMs2MassDeviation(prof.getStandardMs2MassDeviation().multiply(deviationScale));
                final TreeScoring treeScoring = tree.getAnnotationOrThrow(TreeScoring.class);
                // TODO: Check if this is working correct
                ProcessedInput pinp = analyzer.preprocessing(originalInput.getOriginalInput(), prof, toPolynomial(f));
                // dirty hack: take over the tree size scorer.... =/

                {
                    final TreeSizeScorer tss = FragmentationPatternAnalysis.getByClassName(TreeSizeScorer.class, analyzer.getFragmentPeakScorers());
                    if (tss!=null) {
                        final TreeSizeScorer.TreeSizeBonus tb = originalInput.getAnnotation(TreeSizeScorer.TreeSizeBonus.class, new TreeSizeScorer.TreeSizeBonus(tss.getTreeSizeScore()));
                        tss.fastReplace(pinp, tb);
                    }
                }
                 MultipleTreeComputation mtc = analyzer.computeTrees(pinp).onlyWith(Arrays.asList(tree.getRoot().getFormula())).withLowerbound(force ? 0 : treeScoring.getOverallScore()).withoutRecalibration();
                if (oldTree != null) mtc = mtc.withBackbones(oldTree);
                correctedTree = mtc.optimalTree();
                if (correctedTree == null) {
                    //assert !force;
                    correctedTree = tree;
                }
                if (deviationScale == 1) {
                    if (correctedTree.getAnnotationOrThrow(TreeScoring.class).getOverallScore() >= oldTree.getAnnotationOrThrow(TreeScoring.class).getOverallScore()) return correctedTree;
                    else return oldTree;
                } else throw new RuntimeException("Feature not longer supported!");
                /*
                final FTree ft2 = analyzer.computeTrees(analyzer.preprocessing(originalInput.getOriginalInput(), prof)).onlyWith(Arrays.asList(tree.getRoot().getFormula())).withLowerbound(0).withoutRecalibration().withBackbones(correctedTree).optimalTree();
                if (ft2 == null) return correctedTree;
                else if (ft2.getAnnotationOrThrow(TreeScoring.class).getOverallScore() > correctedTree.getAnnotationOrThrow(TreeScoring.class).getOverallScore())
                    return ft2;
                return correctedTree;
                */
            }

            private void calculateScoreBonus() {
                if (recalibrationFunction instanceof Identity) {
                    scoreBonus = 0d;
                    return;
                }
                final ProcessedInput input = tree.getAnnotationOrThrow(ProcessedInput.class);
                final Deviation dev = input.getMeasurementProfile().getStandardMs2MassDeviation();
                final PrecursorIonType ion = tree.getAnnotationOrThrow(PrecursorIonType.class);
                double sc = 0d;
                double distance = 0d;
                final FragmentAnnotation<ProcessedPeak> peakAno = tree.getFragmentAnnotationOrThrow(ProcessedPeak.class);
                for (Fragment f : fragments) {
                    if (peakAno.get(f)==null) continue;
                    final double oldMz = peakAno.get(f).getOriginalMz();
                    final double newMz = recalibrationFunction.value(oldMz);
                    distance += Math.abs(newMz - oldMz);
                    final double theoreticalMz = ion.getIonization().addToMass(f.getFormula().getMass());
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
        if (document.hasKeyInDictionary(dictionary, "deviationScale"))
            deviationScale = document.getDoubleFromDictionary(dictionary, "deviationScale");
        else
            deviationScale = 1d;
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "method", helper.wrap(document, method));
        document.addToDictionary(dictionary, "threshold", distanceThreshold);
        document.addToDictionary(dictionary, "deviationScale", deviationScale);
    }
}
