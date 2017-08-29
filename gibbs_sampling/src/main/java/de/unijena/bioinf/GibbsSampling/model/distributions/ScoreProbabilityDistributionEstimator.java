package de.unijena.bioinf.GibbsSampling.model.distributions;

import de.unijena.bioinf.ChemistryBase.math.HighQualityRandom;
import de.unijena.bioinf.GibbsSampling.model.Candidate;
import de.unijena.bioinf.GibbsSampling.model.EdgeScorer;
import gnu.trove.list.array.TDoubleArrayList;

public class ScoreProbabilityDistributionEstimator<C extends Candidate<?>> implements EdgeScorer<C> {
    private static final boolean DEBUG = false;
    protected final EdgeScorer<C> edgeScorer;
    protected ScoreProbabilityDistribution scoreProbabilityDistribution;

    public ScoreProbabilityDistributionEstimator(EdgeScorer<C> edgeScorer, ScoreProbabilityDistribution distribution) {
        this.edgeScorer = edgeScorer;
        this.scoreProbabilityDistribution = distribution;
    }

    public void prepare(C[][] candidates) {
        edgeScorer.prepare(candidates);
        int numberOfSamples = 100000;
        HighQualityRandom random = new HighQualityRandom();
        double[] sampledScores = new double[numberOfSamples];

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

        this.scoreProbabilityDistribution.estimateDistribution(sampledScores);


        //todo just if EdgeThresholdMinConnectionsFilter not used!!!!!!!!!!!!!!!
        //set adjusted threshold for scorer
//        edgeScorer.setThreshold(scoreProbabilityDistribution.getThreshold());
//        edgeScorer.prepare(candidates);
    }


    public void setThresholdAndPrepare(C[][] candidates) {
        edgeScorer.prepare(candidates);


        if (DEBUG){
            System.out.println("use all scores");
            TDoubleArrayList sampledScores = new TDoubleArrayList();
            for (int i = 0; i < candidates.length; i++) {
                C[] c1 = candidates[i];
                for (int j = i+1; j < candidates.length; j++) {
                    C[] c2 = candidates[j];
                    for (int k = 0; k < c1.length; k++) {
                        C cc1 = c1[k];
                        for (int l = 0; l < c2.length; l++) {
                            C cc2 = c2[l];
                            sampledScores.add(this.edgeScorer.scoreWithoutThreshold(cc1, cc2));
                        }
                    }
                }
            }
            this.scoreProbabilityDistribution.estimateDistribution(sampledScores.toArray());
        } else {
            int numberOfSamples = 100000;
            HighQualityRandom random = new HighQualityRandom();
            double[] sampledScores = new double[numberOfSamples];

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
            this.scoreProbabilityDistribution.estimateDistribution(sampledScores);
        }



        //todo just if EdgeThresholdMinConnectionsFilter not used!!!!!!!!!!!!!!!
        //set adjusted threshold for scorer
        edgeScorer.setThreshold(scoreProbabilityDistribution.getThreshold());
        edgeScorer.prepare(candidates);
    }

    @Override
    public void setThreshold(double threshold) {
        throw new NoSuchMethodError();
    }


    public double score(C candidate1, C candidate2) {
        double score = this.edgeScorer.score(candidate1, candidate2);
        double prob = this.scoreProbabilityDistribution.toPvalue(score);
//        double prob = ((ExponentialDistribution)this.scoreProbabilityDistribution).toPvalue2(score);
        return prob;
    }

    @Override
    public double scoreWithoutThreshold(C candidate1, C candidate2) {
        double score = this.edgeScorer.scoreWithoutThreshold(candidate1, candidate2);
        double prob = this.scoreProbabilityDistribution.toPvalue(score);
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
