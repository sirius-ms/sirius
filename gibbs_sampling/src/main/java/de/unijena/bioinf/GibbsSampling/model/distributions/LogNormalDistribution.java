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

import de.unijena.bioinf.ChemistryBase.math.MathUtils;
import de.unijena.bioinf.GibbsSampling.model.GibbsMFCorrectionNetwork;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.procedure.TDoubleProcedure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

public class LogNormalDistribution implements ScoreProbabilityDistribution {
    private static final Logger LOG = LoggerFactory.getLogger(LogNormalDistribution.class);
    private double logMean;
    private double logVar;
    private boolean robustEstimator;
    private static final double MAD_SCALE_FACTOR = 1.482602218505602;
    private static final double DEFAULT_LOGMEAN = -1.8184825393688084;
    private static final double DEFAULT_LOGVAR = 0.1640657366079353;
    private static final boolean DEBUG = false;

    public LogNormalDistribution(double logMean, double logVar) {
        this.logMean = logMean;
        this.logVar = logVar;
    }

    public LogNormalDistribution(boolean robustEstimator) {
        this.robustEstimator = robustEstimator;
        setDefaultParameters();
    }

    public void estimateDistribution(double[] exampleValues) {
        if (robustEstimator) estimateParametersRobust(exampleValues);
        else estimateParameters(exampleValues);
        if (GibbsMFCorrectionNetwork.DEBUG) System.out.println("logmean " + logMean + " logvar " + logVar);

    }

    @Override
    public void setDefaultParameters() {
        logMean = DEFAULT_LOGMEAN;
        logVar = DEFAULT_LOGVAR;
    }

    private void estimateParameters(double[] exampleValues){
        int l = 0;
        double logMean = 0.0D;
        double logVar = 0.0D;

        for(int i = 0; i < exampleValues.length; ++i) {
            double v = exampleValues[i];
            if(v > 0.0D) {
                logMean += Math.log(v);
                ++l;
            }
        }

        if (l<10){
            LOG.warn("Cannot estimate score distribution. Too few examples. Using default values.");
            if (logMean==0 || logVar==0){
                logMean = DEFAULT_LOGMEAN;
                logVar = DEFAULT_LOGVAR;
            }
            return;
        }

        logMean /= (double)l;

        for(int i = 0; i < exampleValues.length; ++i) {
            double v = exampleValues[i];
            if(v > 0.0D) {
                double s = Math.log(v) - logMean;
                logVar += s * s;
            }
        }

        logVar /= (double)(l - 1);
        this.logMean = logMean;
        this.logVar = logVar;
        if (DEBUG) {
            System.out.println("Parameter estimate for log normal distribution: logmean: " + String.valueOf(logMean) + ", logvar: " + String.valueOf(logVar));
        }
    }

    private void estimateParametersRobust(double[] exampleValues){
        TDoubleArrayList nonZeroSampleValues = new TDoubleArrayList();
        for (double exampleValue : exampleValues) {
            if (exampleValue>0d) nonZeroSampleValues.add(Math.log(exampleValue));
        }
        nonZeroSampleValues.sort();

        if (nonZeroSampleValues.size()<10){
            LOG.warn("Cannot estimate score distribution. Too few examples. Using default values.");
            if (logMean==0 || logVar==0){
                logMean = DEFAULT_LOGMEAN;
                logVar = DEFAULT_LOGVAR;
            }
            return;
        }


        double logMedian = nonZeroSampleValues.get((int)(nonZeroSampleValues.size()/2));
        double[] distanceFromMedian = new double[nonZeroSampleValues.size()];
        int[] counter = new int[]{0};
        nonZeroSampleValues.forEach(new TDoubleProcedure() {
            @Override
            public boolean execute(double v) {
                distanceFromMedian[counter[0]++] = Math.abs(v - logMedian);
                return true;
            }
        });

        Arrays.sort(distanceFromMedian);
        double logMAD = distanceFromMedian[(int)(nonZeroSampleValues.size()/2)];

        this.logMean = logMedian;
        this.logVar = Math.pow(MAD_SCALE_FACTOR*logMAD,2);

        if (DEBUG){
            System.out.println("Robust parameter estimate for log normal distribution: logmean: "+String.valueOf(logMean)+", logvar: "+String.valueOf(logVar));
            try {
                BufferedWriter writer = Files.newBufferedWriter(Paths.get("lognorm_dist_estimation_train_values.csv"));
                for (double exampleValue : exampleValues) {
                    writer.write(String.valueOf(exampleValue));
                    writer.newLine();
                }
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public double toPvalue(double score) {
        return 1-this.cdf(score);
    }

    @Override
    public double toLogPvalue(double score) {
        return Math.log(toPvalue(score));
    }

    public double cdf(double value) {
        return MathUtils.cdf(Math.log(value), this.logMean, this.logVar);
    }

    public LogNormalDistribution clone() {
        return new LogNormalDistribution(this.robustEstimator);
    }
}
