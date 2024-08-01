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

import de.unijena.bioinf.ChemistryBase.fp.FPIter2;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.fingerid.blast.parameters.ParameterStore;
import org.jetbrains.annotations.Nullable;

public class SimpleMaximumLikelihoodScoring implements FingerblastScoring<Object> {

    protected final PredictionPerformance[] performances;
    protected final double[] tp, fn, fp, tn;
    private double threshold, minSamples;

    public SimpleMaximumLikelihoodScoring(PredictionPerformance[] perf) {
        this.performances = new PredictionPerformance[perf.length];
        tp = new double[perf.length];
        fn = tp.clone();
        fp = tp.clone();
        tn = tp.clone();
        for (int k=0; k < perf.length; ++k) {
            this.performances[k] = perf[k].withPseudoCount(0.25d);
            tp[k] = Math.log(performances[k].getRecall());
            fn[k] = Math.log(1d-performances[k].getRecall());
            fp[k] = Math.log(1d - performances[k].getSpecitivity());
            tn[k] = Math.log(performances[k].getSpecitivity());
        }
    }

    @Override
    public Object extractParameters(ParameterStore ignored) {
        return null;
    }

    @Override
    public void prepare(@Nullable Object ignored) {

    }


    @Override
    public double score(ProbabilityFingerprint fingerprint, Fingerprint databaseEntry) {
        double score=0d;
        int k=-1;
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
        }
        return score;
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
