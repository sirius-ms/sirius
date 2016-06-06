package de.unijena.bioinf.ConfidenceScore;

import de.unijena.bioinf.fingerid.PredictionPerformance;

/**
 * Created by Marcus Ludwig on 10.03.16.
 */
public class PredictionPerformanceWrapper {
    protected int tp;
    protected int fp;
    protected int tn;
    protected int fn;

    private de.unijena.bioinf.fingerid.PredictionPerformance predictionPerformance;

    PredictionPerformanceWrapper(){
        this.tp = 0;
        this.fp = 0;
        this.tn = 0;
        this.fn = 0;
        calc();
    }


    public PredictionPerformanceWrapper(int tp, int fp, int tn, int fn) {
        this.tp = tp;
        this.fp = fp;
        this.tn = tn;
        this.fn = fn;
        calc();
    }

    void calc(){
        this.predictionPerformance = new de.unijena.bioinf.fingerid.PredictionPerformance(tp, fp, tn, fn);
    }

    void merge(PredictionPerformanceWrapper other) {
        this.tp += other.tp;
        this.fp += other.fp;
        this.tn += other.tn;
        this.fn += other.fn;
    }

    public PredictionPerformance getPredictionPerformance() {
        return predictionPerformance;
    }


}
