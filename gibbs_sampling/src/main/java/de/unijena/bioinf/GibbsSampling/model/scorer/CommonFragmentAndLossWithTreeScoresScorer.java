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
import de.unijena.bioinf.GibbsSampling.model.FragmentWithIndex;
import de.unijena.bioinf.GibbsSampling.model.FragmentsCandidate;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class CommonFragmentAndLossWithTreeScoresScorer extends CommonFragmentAndLossScorer{

    public CommonFragmentAndLossWithTreeScoresScorer(double threshold) {
        super(threshold);
        MINIMUM_NUMBER_MATCHED_PEAKS_LOSSES = 0.1; //changed from 5
    }


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
                double bestScore = maxScore[pos];
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
                    bestPossibleScore += fragment.getScore();
                }
                for (FragmentWithIndex loss : compoundCandidate.getLosses()) {
                    bestPossibleScore += loss.getScore();
                }
                bestPossibleScoreOverAll = Math.max(bestPossibleScoreOverAll, bestPossibleScore);
            }

            norm[i] = bestPossibleScoreOverAll-minimum_number_matched_peaks_losses;
        }

        return norm;
    }

    @Override
    protected double scoreMatchedPeaks(PeakWithExplanation peak1, PeakWithExplanation peak2){
        return Math.max(0, Math.min(peak1.bestScore,peak2.bestScore));//changed to take minimum score of both (at least enables reasonable normalization)
    }

    @Override
    protected double scoreMatchedFragments(FragmentWithIndex fragment1, FragmentWithIndex fragment2){
        return Math.max(0, Math.min(fragment1.getScore(),fragment2.getScore()));//changed to take minimum score of both (at least enables reasonable normalization)
    }
}
