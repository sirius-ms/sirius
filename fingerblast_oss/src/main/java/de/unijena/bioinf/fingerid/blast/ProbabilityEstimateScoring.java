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

package de.unijena.bioinf.fingerid.blast;

import de.unijena.bioinf.ChemistryBase.fp.FPIter;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.fingerid.blast.parameters.ParameterStore;

public class ProbabilityEstimateScoring implements FingerblastScoring<ProbabilityFingerprint> {

    private PredictionPerformance performances[];

    private double[] is, isnot;
    private double alpha, threshold, minSamples;

    public ProbabilityEstimateScoring(PredictionPerformance[] performances) {
        this.performances = performances;
        this.alpha = 1d/performances[0].withPseudoCount(0.25d).numberOfSamplesWithPseudocounts();
        this.is = new double[performances.length];
        this.isnot = is.clone();
    }

    @Override
    public ProbabilityFingerprint extractParameters(ParameterStore store) {
        return store.getFP().orElseThrow();
    }

    @Override
    public void prepare(ProbabilityFingerprint fingerprint) {
        int k=0;
        for (FPIter fp : fingerprint) {
            if (performances[k].getSmallerClassSize() < minSamples || performances[k].getF() < threshold) {
                is[k] = isnot[k] = 0d;
            } else {
                final double platt = laplaceSmoothing(fp.getProbability(), alpha);
                is[k] = Math.log(platt);
                isnot[k] = Math.log(1d-platt);
            }
            ++k;
        }
    }

    @Override
    public double score(ProbabilityFingerprint fingerprint, Fingerprint databaseEntry) {
        int k=0;
        double score=0d;
        for (FPIter iter : databaseEntry) {
            score += (iter.isSet() ? is[k] : isnot[k]);
            ++k;
        }
        return score;
    }

    private static double laplaceSmoothing(double probability, double alpha) {
        return (probability + alpha) / (1d + 2d * alpha);
    }


    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public double getMinSamples() {
        return minSamples;
    }

    public void setMinSamples(double minSamples) {
        this.minSamples = minSamples;
    }

}
