/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.GibbsSampling.model.scorer;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.math.ByMedianEstimatable;
import de.unijena.bioinf.ChemistryBase.math.ParetoDistribution;
import de.unijena.bioinf.ChemistryBase.math.RealDistribution;
import de.unijena.bioinf.GibbsSampling.model.FragmentWithIndex;
import de.unijena.bioinf.GibbsSampling.model.FragmentsCandidate;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class CommonFragmentAndLossScorerNoiseIntensityWeighted extends CommonFragmentAndLossScorer {

    final RealDistribution distribution;
    private double beta;
    private double maxClip;

    public CommonFragmentAndLossScorerNoiseIntensityWeighted() {
        this(0d);
    }

    public CommonFragmentAndLossScorerNoiseIntensityWeighted(double threshold) {
        super(threshold);
        MINIMUM_NUMBER_MATCHED_PEAKS_LOSSES = 1d; //changed from 5
        beta = 0.00001;
        double xmin = 0.002;
//        double medianNoise = 0.005;
        double medianNoise = 0.015;
        ByMedianEstimatable<? extends RealDistribution> estimatableDistribution = ParetoDistribution.getMedianEstimator(xmin);
        distribution = estimatableDistribution.extimateByMedian(medianNoise);

    }


    protected double peakIsNoNoise(double relativeIntensity) {
        if (relativeIntensity>=1d) return 1d;
        final double clipping = 1d - distribution.getCumulativeProbability(1d);
        final double peakIntensity = relativeIntensity;
        final double noiseProbability = 1d-distribution.getCumulativeProbability(peakIntensity);
        final double clippingCorrection = (noiseProbability-clipping+beta)/(1-clipping+beta);
        return 1d-clippingCorrection;
    }



//    public CommonFragmentAndLossScorerNoiseIntensityWeighted(double threshold, double medianNoise) {
//        super(threshold);
//        MINIMUM_NUMBER_MATCHED_PEAKS_LOSSES = 1d; //changed from 5
//        beta = 0.00001;
////        double xmin = 0.002;
////        double medianNoise = 0.005;
//        double xmin = medianNoise/10d;
//        maxClip = medianNoise*100d;
//        ByMedianEstimatable<? extends RealDistribution> estimatableDistribution = ParetoDistribution.getMedianEstimator(xmin);
//        distribution = estimatableDistribution.extimateByMedian(medianNoise);
//
//    }
//
//    private double peakIsNoNoise(double relativeIntensity) {
////        if (relativeIntensity>=1d) return 1d;
////        final double clipping = 1d - distribution.getCumulativeProbability(1d);
//        if (relativeIntensity>=maxClip) return 1d;
//        final double clipping = 1d - distribution.getCumulativeProbability(maxClip);
//        final double peakIntensity = relativeIntensity;
//        final double noiseProbability = 1d-distribution.getCumulativeProbability(peakIntensity);
//        final double clippingCorrection = (noiseProbability-clipping+beta)/(1-clipping+beta);
//        return 1d-clippingCorrection;
//    }



    /**
     *
     * @param currentCandidates
     * @param useFragments true: normal spectrum and fragments, false: inverted spectrum and losses
     * @return
     */
    @Override
    protected PeakWithExplanation[] getPeaksWithExplanations(FragmentsCandidate[] currentCandidates, final boolean useFragments){
        Set<Ionization> ions = collectIons(currentCandidates);

        int maxIdx = -1;
        for (FragmentsCandidate currentCandidate : currentCandidates) {
            for (FragmentWithIndex scoredFragment : currentCandidate.getCandidate().getFragments()) {
                final short idx = scoredFragment.getIndex();
                if (idx>maxIdx) maxIdx = idx;
            }
        }

        TObjectIntHashMap<Ionization> ionToIdx = new TObjectIntHashMap<>(ions.size());
        int pos = 0;
        for (Ionization ion : ions) {
            ionToIdx.put(ion, pos++);
        }

        // idx to number of peaks
        maxIdx += 1;


        Set<MolecularFormula>[] matchedFragments;
        double[] maxScore;//todo use 0 as min?
        if (useFragments){
            matchedFragments = new Set[maxIdx*ions.size()];
            maxScore = new double[maxIdx*ions.size()];
        }  else {
            matchedFragments = new Set[maxIdx];
            maxScore = new double[maxIdx];
        }
        for(int j = 0; j < currentCandidates.length; ++j) {
            FragmentsCandidate c = currentCandidates[j];
            PrecursorIonType currentIon = c.getIonType();
            final FragmentWithIndex[] fragments;
            final short[] indices;
            if (useFragments){
                fragments = c.getFragments();
                for (int i = 0; i < fragments.length; i++) {
                    final MolecularFormula formula = fragments[i].getFormula();
                    final double score = fragments[i].getScore();
                    final int idx = fragments[i].getIndex()+maxIdx*ionToIdx.get(fragments[i].getIonization());
                    if (matchedFragments[idx]==null){
                        matchedFragments[idx] = new HashSet<>();
                    }
                    matchedFragments[idx].add(formula);
                    if (score>maxScore[idx]) maxScore[idx] = score;
                }

            } else {
                fragments = c.getLosses();

                for (int i = 0; i < fragments.length; i++) {
                    final MolecularFormula formula = fragments[i].getFormula();
                    final double score = fragments[i].getScore();
                    final short idx = fragments[i].getIndex();
                    if (matchedFragments[idx]==null){
                        matchedFragments[idx] = new HashSet<>();
                    }
                    matchedFragments[idx].add(formula);
                    if (score>maxScore[idx]) maxScore[idx] = score;
                }

            }

        }


        int numOfRealPeaks = 0;
        for (Set<MolecularFormula> matched : matchedFragments) {
            if (matched!=null) ++numOfRealPeaks;
        }

        PeakWithExplanation[] peaksWithExplanations = new PeakWithExplanation[numOfRealPeaks];

        pos = 0;
        for (int j = 0; j < matchedFragments.length; j++) {
            if (matchedFragments[j]!=null){
                final MolecularFormula[] mfArray = matchedFragments[j].toArray(new MolecularFormula[0]);
                final double mass = meanMass(mfArray);
                double bestScore = maxScore[j];
                peaksWithExplanations[pos] = new PeakWithExplanation(mfArray, mass, bestScore);
                ++pos;
            }
        }

        Arrays.sort(peaksWithExplanations);

        return peaksWithExplanations;
    }


    @Override
    public double[] normalization(FragmentsCandidate[][] candidates, double minimum_number_matched_peaks_losses) {
        double[] norm = new double[candidates.length];

        for(int i = 0; i < candidates.length; ++i) {
            FragmentsCandidate[] compoundCandidates = candidates[i];
            double bestPossibleScoreOverAll = -1;

            for (FragmentsCandidate compoundCandidate : compoundCandidates) {
                double bestPossibleScore = 0d;
                for (FragmentWithIndex fragment : compoundCandidate.getFragments()) {
                    bestPossibleScore += peakIsNoNoise(fragment.getScore());
                }
                for (FragmentWithIndex loss : compoundCandidate.getLosses()) {
                    bestPossibleScore += peakIsNoNoise(loss.getScore());
                }
                bestPossibleScoreOverAll = Math.max(bestPossibleScoreOverAll, bestPossibleScore);
            }

            norm[i] = bestPossibleScoreOverAll-minimum_number_matched_peaks_losses;
        }

        return norm;
    }

    @Override
    protected double scoreMatchedPeaks(PeakWithExplanation peak1, PeakWithExplanation peak2){
        return Math.max(0, peakIsNoNoise(peak1.bestScore)*peakIsNoNoise(peak2.bestScore));//todo totally inefficient to do this here
    }

    @Override
    protected double scoreMatchedFragments(FragmentWithIndex fragment1, FragmentWithIndex fragment2){
        return Math.max(0, peakIsNoNoise(fragment1.getScore())*peakIsNoNoise(fragment2.getScore()));
    }
}
