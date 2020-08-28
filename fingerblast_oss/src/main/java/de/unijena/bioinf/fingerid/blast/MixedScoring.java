package de.unijena.bioinf.fingerid.blast;

import de.unijena.bioinf.ChemistryBase.fp.*;

public class MixedScoring implements FingerblastScoring {

    private PredictionPerformance performances[];
    private double[] tp,fp,tn,fn;
    private double[] logOneMinusRecall, logOneminusSpecificity, logRecall, logSpecificity;
    private double alpha;
    private double threshold = 0.25, minSamples=25;

    public MixedScoring(PredictionPerformance[] performances) {
        this.performances = performances.clone();
        this.alpha = 1d/performances[0].withPseudoCount(0.25d).numberOfSamplesWithPseudocounts();
        this.tp = new double[performances.length];
        this.fp = new double[performances.length];
        this.tn = new double[performances.length];
        this.fn = new double[performances.length];
        this.logRecall = new double[performances.length];
        this.logSpecificity = new double[performances.length];
        this.logOneMinusRecall = new double[performances.length];
        this.logOneminusSpecificity = new double[performances.length];
        for (int k=0; k < performances.length; ++k) {
            this.performances[k] = performances[k].withPseudoCount(0.25d);
            logOneMinusRecall[k] = Math.log(1d - this.performances[k].getRecall());
            logOneminusSpecificity[k] = Math.log(1d - this.performances[k].getSpecitivity());
            logRecall[k] = Math.log(this.performances[k].getRecall());
            logSpecificity[k] = Math.log(this.performances[k].getSpecitivity());
        }

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
    public void prepare(ProbabilityFingerprint fingerprint) {
        int k=0;
        for (FPIter iter : fingerprint) {
            final double platt = laplaceSmoothing(iter.getProbability());
            final double logplatt = Math.log(platt);
            final double lognotplatt = Math.log(1d-platt);
            tp[k] = (2d / 4d) * logplatt + (2d / 4d) * logRecall[k];
            fp[k] = (2d / 4d) * lognotplatt + (2d / 4d) * logOneminusSpecificity[k];
            tn[k] = (2d / 4d) * lognotplatt + (2d / 4d) * logSpecificity[k];
            fn[k] = (2d / 4d) * logplatt + (2d/4d) * logOneMinusRecall[k];

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
}
