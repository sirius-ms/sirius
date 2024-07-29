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

import de.unijena.bioinf.GibbsSampling.model.GibbsMFCorrectionNetwork;
import gnu.trove.list.array.TDoubleArrayList;

public class ParetoDistribution implements ScoreProbabilityDistribution {
    private double xmin;
    private double alpha;
    private boolean estimateByMedian;

    @Deprecated
    /*
    default parameters missing.
     */
    public ParetoDistribution(double xmin, boolean estimateByMedian) {
        this.estimateByMedian = estimateByMedian;
        this.xmin = Math.max(xmin,0.001);
        if (GibbsMFCorrectionNetwork.DEBUG) System.out.println("pareto xmin "+xmin);
    }

    @Deprecated
    public ParetoDistribution(double xmin) {
        this(xmin, false);
    }

    public void estimateDistribution(double[] exampleValues) {
        double lnThres = Math.log(this.xmin);
        int l = 0;
        double sum = 0.0D;
        TDoubleArrayList values = new TDoubleArrayList(exampleValues.length);
        double[] median = exampleValues;
        int var9 = exampleValues.length;

        for(int alphaByMedian = 0; alphaByMedian < var9; ++alphaByMedian) {
            double v = median[alphaByMedian];
            if(v >= this.xmin) {
                sum += Math.log(v) - lnThres;
                values.add(v);
                ++l;
            }
        }

        this.alpha = (double)l / sum;
        values.sort();
        double var14 = values.get(values.size() / 2);
        if (GibbsMFCorrectionNetwork.DEBUG) {
            System.out.println("mean: " + values.sum() / (double)l + " | estimate: " + this.alpha);
            System.out.println("median: " + var14 + " | estimate: " + Math.log(2.0D) / Math.log(var14 / this.xmin));
        }

        if(this.estimateByMedian) {
            double var13 = Math.log(2.0D) / Math.log(var14 / this.xmin);
            if(var13 < this.xmin) {
                System.out.println("median smaller than x_min: fallback to estimation by mean");
            } else {
                this.alpha = var13;
            }
        }

        if (GibbsMFCorrectionNetwork.DEBUG) System.out.println("alpha estimate " + this.alpha);
    }

    @Override
    public void setDefaultParameters() {
        throw new NoSuchMethodError("Not implemented yet");
    }

    public double toPvalue(double score) {
        return score <= this.xmin ? 1d : Math.pow(xmin / score, alpha);
    }

    @Override
    public double toLogPvalue(double score) {
        return Math.log(toPvalue(score));
    }

    public double cdf(double value) {
        return value <= this.xmin?0.0D:1.0D - Math.pow(this.xmin / (value + this.xmin), this.alpha);
    }

    public ScoreProbabilityDistribution clone() {
        return new ParetoDistribution(this.xmin, this.estimateByMedian);
    }
}
