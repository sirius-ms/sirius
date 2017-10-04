package de.unijena.bioinf.GibbsSampling.model.distributions;

import de.unijena.bioinf.ChemistryBase.math.HighQualityRandom;
import de.unijena.bioinf.GibbsSampling.model.Candidate;
import de.unijena.bioinf.GibbsSampling.model.EdgeScorer;
import gnu.trove.list.array.TDoubleArrayList;

import java.util.Arrays;

public class ScoreProbabilityDistributionEstimator<C extends Candidate<?>> implements EdgeScorer<C> {
    private static final boolean DEBUG = true;
    protected final EdgeScorer<C> edgeScorer;
    protected ScoreProbabilityDistribution scoreProbabilityDistribution;
    protected final double percentageOfEdgesToUse;
    protected double threshold;
    private static final boolean percentageWithoutZeroScores = true;

    public ScoreProbabilityDistributionEstimator(EdgeScorer<C> edgeScorer, ScoreProbabilityDistribution distribution, double percentageOfEdgesToUse) {
        this.edgeScorer = edgeScorer;
        this.scoreProbabilityDistribution = distribution;
        this.percentageOfEdgesToUse = percentageOfEdgesToUse;
    }

    public void prepare(C[][] candidates) {
        double[] sampledScores = sampleScores(candidates);

        estimateDistribution(sampledScores);



        if (percentageWithoutZeroScores) sampledScores = excludeZeros(sampledScores);
        Arrays.sort(sampledScores);
        int idx = (int)(percentageOfEdgesToUse*sampledScores.length);
        threshold = scoreProbabilityDistribution.toLogPvalue(sampledScores[idx]);


        //todo just if EdgeThresholdMinConnectionsFilter not used!!!!!!!!!!!!!!!
        //set adjusted threshold for scorer
//        edgeScorer.setThreshold(scoreProbabilityDistribution.getThreshold());
//        edgeScorer.prepare(candidates);
    }

    private double[] excludeZeros(double[] sampledScores){
        TDoubleArrayList withoutZero = new TDoubleArrayList();
        for (double sampledScore : sampledScores){
            if (sampledScore>0) withoutZero.add(sampledScore);
        }
        return withoutZero.toArray();
    }

    protected double[] sampleScores(C[][] candidates){
        edgeScorer.prepare(candidates);

        double[] sampledScores;
        if (DEBUG){
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
                            sampledScoresList.add(this.edgeScorer.scoreWithoutThreshold(cc1, cc2));
                        }
                    }
                }
            }
            sampledScores = sampledScoresList.toArray();
        } else {
            int numberOfSamples = 100000;
            HighQualityRandom random = new HighQualityRandom();

            for(int i = 0; i < numberOfSamples; ++i) {
                int color1 = random.nextInt(candidates.length);
                int color2 = random.nextInt(candidates.length - 1);
                if(color2 >= color1) {
                    ++color2;
                }

                int mf1 = random.nextInt(candidates[color1].length);
                int mf2 = random.nextInt(candidates[color2].length);
                sampledScores[i] = this.edgeScorer.scoreWithoutThreshold(candidates[color1][mf1], candidates[color2][mf2]);
            }
        }
        return sampledScores;
    }



    public void setThresholdAndPrepare(C[][] candidates) {
//        edgeScorer.prepare(candidates);
//
//        double[] sampledScores;
//        if (DEBUG){
//            System.out.println("use all scores");
//            TDoubleArrayList sampledScoresList = new TDoubleArrayList();
//            for (int i = 0; i < candidates.length; i++) {
//                C[] c1 = candidates[i];
//                for (int j = i+1; j < candidates.length; j++) {
//                    C[] c2 = candidates[j];
//                    for (int k = 0; k < c1.length; k++) {
//                        C cc1 = c1[k];
//                        for (int l = 0; l < c2.length; l++) {
//                            C cc2 = c2[l];
//                            sampledScoresList.add(this.edgeScorer.scoreWithoutThreshold(cc1, cc2));
//                        }
//                    }
//                }
//            }
//            sampledScores = sampledScoresList.toArray();
//        } else {
//            sampledScores = sampleScores(candidates);
//        }
        double[] sampledScores = sampleScores(candidates);
        estimateDistribution(sampledScores);

        if (percentageWithoutZeroScores) sampledScores = excludeZeros(sampledScores);
        Arrays.sort(sampledScores);
        int idx = (int)(percentageOfEdgesToUse*sampledScores.length);
        threshold = sampledScores[idx];


        //todo just if EdgeThresholdMinConnectionsFilter not used!!!!!!!!!!!!!!!
        //set adjusted threshold for scorer
        edgeScorer.setThreshold(threshold);
        edgeScorer.prepare(candidates);


        //set threshold which corresponds to p-value of distribution which resample the 'correct' score

        threshold = scoreProbabilityDistribution.toLogPvalue(threshold);

        if (DEBUG) System.out.println("true log p value is "+threshold);

    }

    protected void estimateDistribution(double[] sampledScores){
        this.scoreProbabilityDistribution.estimateDistribution(sampledScores);
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
//        double prob = ((ExponentialDistribution)this.scoreProbabilityDistribution).toPvalue2(score);
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

    public double[] normalization(C[][] candidates) {
        return new double[0];
    }
}
