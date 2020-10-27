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

import de.unijena.bioinf.ChemistryBase.math.HighQualityRandom;
import de.unijena.bioinf.GibbsSampling.model.Candidate;
import de.unijena.bioinf.GibbsSampling.model.EdgeScorer;
import de.unijena.bioinf.GibbsSampling.model.GibbsMFCorrectionNetwork;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.jjobs.JJob;
import gnu.trove.list.array.TDoubleArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class ScoreProbabilityDistributionEstimator<C extends Candidate<?>> implements EdgeScorer<C> {
    private static final Logger LOG = LoggerFactory.getLogger(ScoreProbabilityDistributionEstimator.class);
    protected final EdgeScorer<C> edgeScorer;
    protected ScoreProbabilityDistribution scoreProbabilityDistribution;
    protected final double percentageOfEdgesBelowThreshold;
    protected double threshold;
    private static final boolean percentageWithoutZeroScores = true;
    private static final int NUMBER_OF_SAMPLES =  100000;

    public ScoreProbabilityDistributionEstimator(EdgeScorer<C> edgeScorer, ScoreProbabilityDistribution distribution, double percentageOfEdgesBelowThreshold) {
        this.edgeScorer = edgeScorer;
        this.scoreProbabilityDistribution = distribution;
        this.percentageOfEdgesBelowThreshold = percentageOfEdgesBelowThreshold;
    }

    public void prepare(C[][] candidates) {
        double[] sampledScores = sampleScores(candidates); //might be empty

        estimateDistribution(sampledScores);



//        if (percentageWithoutZeroScores) sampledScores = (sampledScores);
        Arrays.sort(sampledScores);
        int idx = (int)(percentageOfEdgesBelowThreshold *sampledScores.length);
        if (idx>=sampledScores.length){
            threshold = scoreProbabilityDistribution.toLogPvalue(0);
        } else {
            threshold = scoreProbabilityDistribution.toLogPvalue(sampledScores[idx]);
        }
        //set threshold estimate from real scores not estimated distribution!

//        System.out.println("prepare old threshold "+threshold);
//        threshold = findThresholdFromCdf();
//        threshold = scoreProbabilityDistribution.toLogPvalue(threshold);
//        System.out.println("prepare threshold "+threshold);
    }

    private double[] excludeZeros(double[] sampledScores){
        TDoubleArrayList withoutZero = new TDoubleArrayList();
        for (double sampledScore : sampledScores){
            if (sampledScore>0) withoutZero.add(sampledScore);
        }
        return withoutZero.toArray();
    }

    protected double[] sampleScores(C[][] candidates){
        long milli = System.currentTimeMillis();
        edgeScorer.prepare(candidates);
        long milli2 = System.currentTimeMillis();
        System.out.println("PREPARING INNER EDGE SCORER TOOK " + (milli2-milli) + " milliseconds");
        double[] sampledScores;
        if (GibbsMFCorrectionNetwork.DEBUG){
            System.out.println("use all scores");
            TDoubleArrayList sampledScoresList = new TDoubleArrayList();
            for (int i = 0; i < candidates.length; i++) {
                C[] c1 = candidates[i];
                for (int j = i+1; j < candidates.length; j++) {
                    C[] c2 = candidates[j];
                    for (int k = 0; k < c1.length; k++) {
                        C cc1 = c1[k];
                        for (int l = 0; l < c2.length; l++) {
                            C cc2 = c2[l];
                            double score = this.edgeScorer.scoreWithoutThreshold(cc1, cc2);
                            if (percentageWithoutZeroScores && score<=0) continue;
                            sampledScoresList.add(score);
                        }
                    }
                }
            }
            sampledScores = sampledScoresList.toArray();
        } else {
            int numberOfTrails = NUMBER_OF_SAMPLES*20;
            HighQualityRandom random = new HighQualityRandom();
            sampledScores = new double[NUMBER_OF_SAMPLES];
            int pos = 0;
            int trialCount = 0;
            while (pos<NUMBER_OF_SAMPLES){
                ++trialCount;
                if (trialCount>numberOfTrails) break;;
                int color1 = random.nextInt(candidates.length);
                int color2 = random.nextInt(candidates.length - 1);
                if(color2 >= color1) {
                    ++color2;
                }

                int mf1 = random.nextInt(candidates[color1].length);
                int mf2 = random.nextInt(candidates[color2].length);
                double score = this.edgeScorer.scoreWithoutThreshold(candidates[color1][mf1], candidates[color2][mf2]);
                if (percentageWithoutZeroScores && score<=0) continue;
                sampledScores[pos++] = score;
            }
            if (pos<NUMBER_OF_SAMPLES) sampledScores = Arrays.copyOf(sampledScores, pos);
        }
        long milli3 = System.currentTimeMillis();
        System.out.println("Sampling TOOK " + (milli3-milli2) + " milliseconds");
        return sampledScores;
    }



    public void setThresholdAndPrepare(C[][] candidates) {
        double[] sampledScores = sampleScores(candidates);
        estimateDistribution(sampledScores);

//        if (percentageWithoutZeroScores) sampledScores = excludeZeros(sampledScores);
        Arrays.sort(sampledScores);
        int idx = (int)(percentageOfEdgesBelowThreshold *sampledScores.length);
        if (idx>=sampledScores.length){
            threshold = 0d;
        } else {
            threshold = sampledScores[idx];
        }
//        System.out.println("old threshold "+threshold);
//        threshold = findThresholdFromCdf();
//        System.out.println("threshold "+threshold);

        //set adjusted threshold for scorer
        edgeScorer.setThreshold(threshold);
        edgeScorer.prepare(candidates);


        //set threshold which corresponds to p-value of distribution which resample the 'correct' score

        threshold = scoreProbabilityDistribution.toLogPvalue(threshold);

        if (GibbsMFCorrectionNetwork.DEBUG) System.out.println("true log p value is "+threshold);

    }

    protected double findThresholdFromCdf(){
        double lb = 0d;
        double ub = Double.POSITIVE_INFINITY;
        double score = 1d;
        double resultingEdgeRatio = scoreProbabilityDistribution.cdf(score);
        while (Math.abs(resultingEdgeRatio-percentageOfEdgesBelowThreshold)>0.0001){
            if (resultingEdgeRatio<percentageOfEdgesBelowThreshold){
                lb = Math.max(lb, score);
            } else {
                ub = Math.min(ub, score);
            }

            if (Double.isInfinite(ub)){
                score = 2*score;
            } else {
                score = (ub-lb)/2d+lb;
            }
            resultingEdgeRatio = scoreProbabilityDistribution.cdf(score);
        }
        return score;
    }

    protected void estimateDistribution(double[] sampledScores){
        if (sampledScores==null || sampledScores.length==0){
            LOG.warn("Cannot estimate score distribution. Too few examples. Using default parameters.");
            this.scoreProbabilityDistribution.setDefaultParameters();
        } else {
            this.scoreProbabilityDistribution.estimateDistribution(sampledScores);
        }

    }

    @Override
    public void setThreshold(double threshold) {
        throw new NoSuchMethodError();
    }


    @Override
    public double getThreshold() {
        return threshold;
    }

    public double score(C candidate1, C candidate2) {
        double score = this.edgeScorer.score(candidate1, candidate2);
        double prob = this.scoreProbabilityDistribution.toLogPvalue(score);
        return prob;
    }

    @Override
    public double scoreWithoutThreshold(C candidate1, C candidate2) {
        double score = this.edgeScorer.scoreWithoutThreshold(candidate1, candidate2);
        double prob = this.scoreProbabilityDistribution.toLogPvalue(score);
        return prob;
    }

    public ScoreProbabilityDistribution getProbabilityDistribution() {
        return this.scoreProbabilityDistribution;
    }

    public void clean() {
        this.edgeScorer.clean();
    }

    public double[] normalization(C[][] candidates, double minimum_number_matched_peaks_losses) {
        return new double[0];
    }



    ////////////// kaidu ////////////////

    @Override
    public BasicJJob<Object> getPrepareJob(C[][] candidates) {
        return new BasicMasterJJob<Object>(JJob.JobType.CPU) {
            @Override
            protected Object compute() throws Exception {
                long milli = System.currentTimeMillis();
                submitSubJob(edgeScorer.getPrepareJob(candidates)).takeResult();
                long milli2 = System.currentTimeMillis();
                System.out.println("PREPARING INNER EDGE SCORER TOOK " + (milli2-milli) + " milliseconds");
                double[] sampledScores;
                int numberOfTrails = NUMBER_OF_SAMPLES*20;
                HighQualityRandom random = new HighQualityRandom();
                sampledScores = new double[NUMBER_OF_SAMPLES];
                int pos = 0;
                int trialCount = 0;
                while (pos<NUMBER_OF_SAMPLES){
                    ++trialCount;
                    if (trialCount>numberOfTrails) break;;
                    int color1 = random.nextInt(candidates.length);
                    int color2 = random.nextInt(candidates.length - 1);
                    if(color2 >= color1) {
                        ++color2;
                    }

                    int mf1 = random.nextInt(candidates[color1].length);
                    int mf2 = random.nextInt(candidates[color2].length);
                    double score = edgeScorer.scoreWithoutThreshold(candidates[color1][mf1], candidates[color2][mf2]);
                    if (percentageWithoutZeroScores && score<=0) continue;
                    sampledScores[pos++] = score;
                }
                if (pos<NUMBER_OF_SAMPLES) sampledScores = Arrays.copyOf(sampledScores, pos);
                long milli3 = System.currentTimeMillis();
                System.out.println("Sampling TOOK " + (milli3-milli2) + " milliseconds");

                estimateDistribution(sampledScores);
                Arrays.sort(sampledScores);
                int idx = (int)(percentageOfEdgesBelowThreshold *sampledScores.length);
                if (idx>=sampledScores.length){
                    threshold = scoreProbabilityDistribution.toLogPvalue(0);
                } else {
                    threshold = scoreProbabilityDistribution.toLogPvalue(sampledScores[idx]);
                }

                return "";
            }
        };
    }
}
