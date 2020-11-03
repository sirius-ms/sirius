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

package de.unijena.bioinf.GibbsSampling.model.distributions;

import java.util.Arrays;

public class EmpiricalScoreProbabilityDistribution implements ScoreProbabilityDistribution {
    final double[] scores;
    final double[] pValues;

    /**
     *
     * @param scores
     * @param pValues
     */
    @Deprecated
    public EmpiricalScoreProbabilityDistribution(double[] scores, double[] pValues) {
        this.scores = scores;
        this.pValues = pValues;
    }

    public void estimateDistribution(double[] exampleValues) {
    }

    @Override
    public void setDefaultParameters() {

    }

    public double toPvalue(double score) {
        int idx = Arrays.binarySearch(this.scores, score);
        if(idx >= 0) {
            return this.pValues[idx];
        } else {
            int insertIdx = -(idx + 1);
            return insertIdx >= this.pValues.length - 1?this.pValues[this.pValues.length - 1]:(insertIdx == 0?this.pValues[0]:this.interpolate(score, insertIdx));
        }
    }

    private double interpolate(double score, int insertIdx) {
        return ((score - this.scores[insertIdx]) * this.pValues[insertIdx] + (this.scores[insertIdx + 1] - score) * this.pValues[insertIdx + 1]) / (this.scores[insertIdx + 1] - this.scores[insertIdx]);
    }

    @Override
    public double toLogPvalue(double score) {
        return Math.log(toPvalue(score));
    }

    @Override
    public double cdf(double score) {
        return 1-toPvalue(score);
    }

    public ScoreProbabilityDistribution clone() {
        return new EmpiricalScoreProbabilityDistribution(this.scores, this.pValues);
    }
}
