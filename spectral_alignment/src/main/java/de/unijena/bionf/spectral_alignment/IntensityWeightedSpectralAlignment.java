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

package de.unijena.bionf.spectral_alignment;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.OrderedSpectrum;
import it.unimi.dsi.fastutil.ints.IntIntImmutablePair;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

/**
 * find best scoring alignment, intensity weighted. Each peak matches at most one peak in the other spectrum.
 */
public class IntensityWeightedSpectralAlignment extends AbstractSpectralMatching {

    public IntensityWeightedSpectralAlignment(Deviation deviation) {
        super(deviation);
    }

    @Override
    public SpectralSimilarity score(OrderedSpectrum<Peak> left, OrderedSpectrum<Peak> right, double precursorLeft, double precursorRight) {
        return score1To1(left, right);
    }

    public SpectralSimilarity score(OrderedSpectrum<Peak> left, OrderedSpectrum<Peak> right) {
        return score1To1(left, right);
    }

    /**
     * one peak can only match one peak in the other spectrum
     */
    public SpectralSimilarity score1To1(OrderedSpectrum<Peak> left, OrderedSpectrum<Peak> right) {
        if (left.isEmpty() || right.isEmpty()) return new SpectralSimilarity(0d, 0);
        MatchesMatrix backtrace = new MatchesMatrix();
        double[] scoreRowBefore = new double[left.size()];
        double[] scoreRow = new double[left.size()];
        int i = 0, j = 0, i_lower;

        final int nl=left.size(), nr=right.size();
        while (i < nl && left.getMzAt(i) < 0.5d) ++i; //skip negative peaks of inversed spectra
        while (j < nr && right.getMzAt(j) < 0.5d) ++j;
        i_lower=i;
        while (i < nl && j < nr) {
            Peak lp = left.getPeakAt(i);
            Peak rp = right.getPeakAt(j);
            final double difference = lp.getMass()- rp.getMass();
            final double allowedDifference = maxAllowedDifference(Math.min(lp.getMass(), rp.getMass()));

            double matchScore;
            if (Math.abs(difference) <= allowedDifference) {
                matchScore = scorePeaks(lp,rp);
            } else {
                matchScore = 0d;
            }
            if (i==0) {
                scoreRow[i] = Math.max(scoreRowBefore[i], matchScore);
                if (matchScore==scoreRow[i]){
                    backtrace.setMatch(i,j);
                }
            } else {
                //might not have been updated because of this whole window moving but it must hold that scoreRowBefore[i]>=scoreRowBefore[i-1] and scoreRow[i]>=scoreRow[i-1]
                if (scoreRowBefore[i] <= scoreRowBefore[i-1]) {
                    scoreRowBefore[i] = scoreRowBefore[i-1];
                }
//                scoreRowBefore[i] = Math.max(scoreRowBefore[i], scoreRowBefore[i-1]); //might not have been updated because of this whole window moving but it must hold that scoreRowBefore[i]>=scoreRowBefore[i-1] and scoreRow[i]>=scoreRow[i-1]
                scoreRow[i] = Math.max(Math.max(scoreRow[i-1], scoreRowBefore[i]), matchScore+scoreRowBefore[i-1]); //todo max(scoreRow[i]...?
                if (matchScore+scoreRowBefore[i-1]==scoreRow[i]){
                    backtrace.setMatch(i,j);
                }
            }


            if (Math.abs(difference) <= allowedDifference) {
                ++i;
            } else if (difference > 0) {
                ++j;

                double[] tmp = scoreRowBefore;
                scoreRowBefore = scoreRow;
                scoreRow = tmp; //could be an empty array, but in this way we save creation time
                //update horizontal gaps???
                for (int k = i_lower; k <= i; k++) {
                    scoreRow[k] = scoreRowBefore[k]; //todo alway greater equal? Math.max(scoreRow[k], scoreRowBefore[k]);

                }
                i = i_lower;
            } else {
                ++i;
                i_lower = i; //todo or i-1?
            }
            if (i>=nl){
                if (j>=nr-1) break;
                ++j;
                double[] tmp = scoreRowBefore;
                scoreRowBefore = scoreRow;
                scoreRow = tmp; //could be an empty array, but in this way we save creation time
                //update horizontal gaps???
                for (int k = i_lower; k <= nl-1; k++) {
                    scoreRow[k] = scoreRowBefore[k]; //todo alway greater equal? Math.max(scoreRow[k], scoreRowBefore[k]);
                }
                i = i_lower;
            }
        }

        //find best score
        double maxScore = Double.NEGATIVE_INFINITY;
        int maxScorePos = -1;
        double maxMzRight = right.getMzAt(right.size()-1);
        for (int k = Math.min(i_lower,scoreRow.length-1); k < scoreRow.length; k++) {
            double s = scoreRow[k];
            double mz = left.getMzAt(k);
            if (s>=maxScore){
                maxScorePos = k;
                maxScore = s;
            }

            if (maxMzRight<mz && maxAllowedDifference(maxMzRight)<(mz-maxMzRight)){
                break;
            }
        }

        int matchedPeaks = backtraceAndCountMatchedPeaks(left, right, backtrace, maxScorePos, (j==nr?nr-1:j));
        return  new SpectralSimilarity(maxScore, matchedPeaks);

    }

    protected int backtraceAndCountMatchedPeaks(OrderedSpectrum<Peak> left, OrderedSpectrum<Peak> right, MatchesMatrix backtrace, int imax, int jmax){
        //todo take only one best match. should result in same number of peaks!?!?
        int i = imax;
        int j = jmax;

        final BitSet usedIndicesLeft = new BitSet();
        final BitSet usedIndicesRight = new BitSet();

        while (i>=0 && j>=0) {
            Peak lp = left.getPeakAt(i);
            Peak rp = right.getPeakAt(j);
            final double difference = lp.getMass()- rp.getMass();
            final double allowedDifference = maxAllowedDifference(Math.min(lp.getMass(), rp.getMass()));

            double matchScore;
            if (Math.abs(difference) <= allowedDifference) {
                matchScore = scorePeaks(lp,rp);
            } else {
                matchScore = 0d;
            }

            if (backtrace.hasMatch(i,j)){
                if (matchScore>0) {
                    usedIndicesLeft.set(i);
                    usedIndicesRight.set(j);
                }
                --i;
                --j;
            } else if (lp.getMass()>=rp.getMass()){
                --i;
            } else {
                --j;
            }

        }
        return Math.min(usedIndicesLeft.cardinality(), usedIndicesRight.cardinality());
    }

    protected double scorePeaks(Peak lp, Peak rp) {
        return lp.getIntensity()*rp.getIntensity();
    }

    protected double maxAllowedDifference(double mz) {
        return deviation.absoluteFor(mz);
    }

    protected static class MatchesMatrix {
        Set<IntIntImmutablePair> pairs;

        public MatchesMatrix() {
            pairs = new HashSet<>();
        }

        public void setMatch(int leftIdx, int rightIdx){
            pairs.add(new IntIntImmutablePair(leftIdx, rightIdx));
        }

        public boolean hasMatch(int leftIdx, int rightIdx){
            return pairs.contains(new IntIntImmutablePair(leftIdx, rightIdx));
        }
    }
}
