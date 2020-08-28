package de.unijena.bioinf.fingerid.blast;

import de.unijena.bioinf.ChemistryBase.fp.FPIter2;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;

public class SimpleMaximumLikelihoodScoring implements FingerblastScoring {

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
    public void prepare(ProbabilityFingerprint fingerprint) {

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
