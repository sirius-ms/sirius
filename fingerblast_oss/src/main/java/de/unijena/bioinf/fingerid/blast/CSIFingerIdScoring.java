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

import de.unijena.bioinf.ChemistryBase.fp.*;
import de.unijena.bioinf.fingerid.blast.parameters.ParameterStore;

public class CSIFingerIdScoring implements FingerblastScoring<ProbabilityFingerprint> {

    private PredictionPerformance[] performances;
    private double[] tp,fp,tn,fn;
    private double[] logOneMinusRecall, logOneminusSpecificity;
    private double alpha;
    private double threshold = 0.25, minSamples=25;

    public CSIFingerIdScoring(PredictionPerformance[] performances) {
        this.performances = performances.clone();
        this.alpha = 1d/performances[0].withPseudoCount(0.25d).numberOfSamplesWithPseudocounts();
        this.tp = new double[performances.length];
        this.fp = new double[performances.length];
        this.tn = new double[performances.length];
        this.fn = new double[performances.length];
        this.logOneMinusRecall = new double[performances.length];
        this.logOneminusSpecificity = new double[performances.length];
        for (int k=0; k < performances.length; ++k) {
            this.performances[k] = performances[k].withPseudoCount(0.25d);
            logOneMinusRecall[k] = Math.log(1d - this.performances[k].getRecall());
            logOneminusSpecificity[k] = Math.log(1d - this.performances[k].getSpecitivity());
        }

    }

    public PredictionPerformance[] getPerformances() {
        return performances;
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

    @Override
    public ProbabilityFingerprint extractParameters(ParameterStore store) {
        return store.getFP().orElseThrow();
    }

    @Override
    public void prepare(final ProbabilityFingerprint fingerprint) {
        int k=0;
        for (FPIter iter : fingerprint) {
            final double platt = laplaceSmoothing(iter.getProbability());
            final double logplatt = Math.log(platt);
            final double lognotplatt = Math.log(1d-platt);
            tp[k] = (3d / 4d) * logplatt + (1d / 4d) *
                    logOneMinusRecall[k];
            fp[k] = (3d / 4d) * lognotplatt;
            tn[k] = (3d / 4d) * lognotplatt + (1d / 4d) * logOneminusSpecificity[k];
            fn[k] = (3d / 4d) * logplatt;

            ++k;
        }
    }

    private double laplaceSmoothing(double probability) {
        return (probability + alpha) / (1d + 2d * alpha);
    }

    @Override
    public double score(ProbabilityFingerprint fingerprint, Fingerprint databaseEntry) {
        double score=0d;
        int k=-1;
        if (!fingerprint.isCompatible(databaseEntry)) throw new RuntimeException("Fingerprints are not compatible");
        int X = 0;
        for (FPIter2 iter : fingerprint.foreachPair(databaseEntry)) {
            ++k;
            if (performances[k].getF() < threshold  || performances[k].getSmallerClassSize() < minSamples) continue;
            if (iter.isRightSet()) {
                if (iter.isLeftSet()) {
                    score += tp[k];
                } else {
                    score += fn[k];
                }
            } else {
                if (iter.isLeftSet()) {
                    score += fp[k];
                } else {
                    score += tn[k];
                }
            }
            ++X;
        }
        return score;
    }
}
