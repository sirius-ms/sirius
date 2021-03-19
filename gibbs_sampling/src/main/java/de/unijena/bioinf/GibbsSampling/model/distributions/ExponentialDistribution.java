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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExponentialDistribution implements ScoreProbabilityDistribution {
    private static final Logger LOG = LoggerFactory.getLogger(ExponentialDistribution.class);
    private double lambda;
    private boolean estimateByMedian;
    private final double DEFAULT_LAMBDA = 7d;


    public ExponentialDistribution(double lambda) {
        this.lambda = lambda;
    }

    public ExponentialDistribution(boolean estimateByMedian) {
        this.estimateByMedian = estimateByMedian;
        setDefaultParameters();
    }

    public ExponentialDistribution() {
        this(false);
    }

    @Override
    public void setDefaultParameters() {
        lambda = DEFAULT_LAMBDA;
    }

    @Override
    public void estimateDistribution(double[] exampleValues) {
        int l = 0;
        double sum = 0.0D;
        TDoubleArrayList values = new TDoubleArrayList(exampleValues.length);

        for(int lambdaByMedian = 0; lambdaByMedian < exampleValues.length; ++lambdaByMedian) {
            double v = exampleValues[lambdaByMedian];
            if(v > 0.0D) {
                sum += v;
                values.add(v);
                ++l;
            }
        }

        if (values.size()<10){
            LOG.warn("Cannot estimate score distribution. Too few examples. Using default values.");
            this.lambda = DEFAULT_LAMBDA;
        }else {
            this.lambda = (double)l / sum;
            values.sort();
            double median = values.get(values.size() / 2);
            if (GibbsMFCorrectionNetwork.DEBUG) {
                System.out.println("lambda estimate " + this.lambda);
                System.out.println("mean: " + values.sum() / (double)l + " | estimate: " + this.lambda);
                System.out.println("median "+median);

            }
            if(this.estimateByMedian) {
                this.lambda = Math.log(2.0D) / median;
            }
        }

        if (Double.isNaN(lambda) || Double.isInfinite(lambda)){
            LOG.warn("Problem estimating score distribution. Using default values.");
            lambda = DEFAULT_LAMBDA;
        }


        if (GibbsMFCorrectionNetwork.DEBUG) System.out.println("lambda estimate " + this.lambda);
    }

    public void setLambda(double lambda){
        this.lambda = lambda;
    }

    @Override
    public double toPvalue(double score) {
        return Math.exp(-this.lambda * score);
    }

    @Override
    public double toLogPvalue(double score) {
                return -this.lambda * score;
    }

    public double cdf(double value) {
        return 1.0D - Math.exp(-this.lambda * value);
    }

    public ScoreProbabilityDistribution clone() {
        return new ExponentialDistribution(this.estimateByMedian);
    }
}
