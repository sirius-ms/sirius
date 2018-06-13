package de.unijena.bioinf.fingerid.blast;

import de.unijena.bioinf.ChemistryBase.fp.FPIter;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;

public class ProbabilityEstimateScoring implements FingerblastScoring {

    private PredictionPerformance performances[];

    private double[] is, isnot;
    private double alpha, threshold, minSamples;

    public ProbabilityEstimateScoring(PredictionPerformance[] performances) {
        this.performances = performances;
        this.alpha = 1d/performances[0].withPseudoCount(0.25d).numberOfSamplesWithPseudocounts();
        this.is = new double[performances.length];
        this.isnot = is.clone();
    }

    @Override
    public void prepare(ProbabilityFingerprint fingerprint) {
        int k=0;
        for (FPIter fp : fingerprint) {
            if (performances[k].getSmallerClassSize() < minSamples || performances[k].getF() < threshold) {
                is[k] = isnot[k] = 0d;
            } else {
                final double platt = laplaceSmoothing(fp.getProbability(), alpha);
                is[k] = Math.log(platt);
                isnot[k] = Math.log(1d-platt);
            }
            ++k;
        }
    }

    @Override
    public double score(ProbabilityFingerprint fingerprint, Fingerprint databaseEntry) {
        int k=0;
        double score=0d;
        for (FPIter iter : databaseEntry) {
            score += (iter.isSet() ? is[k] : isnot[k]);
            ++k;
        }
        return score;
    }

    private static double laplaceSmoothing(double probability, double alpha) {
        return (probability + alpha) / (1d + 2d * alpha);
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
