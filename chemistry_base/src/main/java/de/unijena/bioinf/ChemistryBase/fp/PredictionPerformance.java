package de.unijena.bioinf.ChemistryBase.fp;

import java.util.Locale;

/**
 * measures F1-Score, accuracy, recall and precision of the predictions
 */
public final class PredictionPerformance {

    public final static class Modify {

        private  Modify(int tp, int fp, int tn, int fn) {
            this.tp = tp;
            this.fp = fp;
            this.tn = tn;
            this.fn = fn;
        }

        private int tp,fp,tn,fn;

        public PredictionPerformance done() {
            return new PredictionPerformance(tp,fp,tn,fn);
        }

        public PredictionPerformance done(PredictionPerformance performance) {
            performance.set(tp,fp,tn,fn);
            return performance;
        }

        public void update(boolean[] truths, boolean[] predictions) {
            for (int k=0; k < truths.length; ++k) update(truths[k], predictions[k]);
        }

        public void update(boolean truth, boolean predicted) {
            if (truth) {
                if (predicted) {
                    ++tp;
                } else {
                    ++fn;
                }
            } else{
                if (predicted) {
                    ++fp;
                } else {
                    ++tn;
                }
            }
        }

        public int getTp() {
            return tp;
        }

        public void setTp(int tp) {
            this.tp = tp;
        }

        public int getFp() {
            return fp;
        }

        public void setFp(int fp) {
            this.fp = fp;
        }

        public int getTn() {
            return tn;
        }

        public void setTn(int tn) {
            this.tn = tn;
        }

        public int getFn() {
            return fn;
        }

        public void setFn(int fn) {
            this.fn = fn;
        }
    }

    private int tp, fp, tn, fn;
    private double f, precision, recall, accuracy;

    @Override
    public String toString() {
        return String.format(Locale.US, "tp=%d\tfp=%d\ttn=%d\tfn=%d\tf1=%f\tprecision=%f\trecall=%f\taccuracy=%f", tp,fp,tn,fn,f,precision,recall,accuracy);
    }

    public Modify modify() {
        return new Modify(tp,fp,tn,fn);
    }

    public void set(int tp, int fp, int tn, int fn) {
        this.tp = tp;
        this.fp = fp;
        this.tn = tn;
        this.fn = fn;
        calc();
    }

    public void set(PredictionPerformance other) {
        set(other.tp,other.fp,other.tn,other.fn);
    }

    public void set(PredictionPerformance.Modify other) {
        set(other.tp,other.fp,other.tn,other.fn);
    }

    public static double averageF1(PredictionPerformance[] ps) {
        double f1=0d;
        for (PredictionPerformance p : ps) f1 += p.getF();
        f1 /= ps.length;
        return f1;
    }

    public static PredictionPerformance fromString(String string) {
        String[] parts = string.split("\t");
        int tp=0, fp=0, tn=0, fn=0;
        for (String s : parts) {
            final int p = s.indexOf('=');
            if ( p < 0) throw new IllegalArgumentException();
            final String token = s.substring(0, p);
            final String value = s.substring(p+1);
            final int v = Integer.parseInt(value);
            switch (token) {
                case "tp": tp = v; break;
                case "fp": fp = v; break;
                case "tn": tn = v; break;
                case "fn": fn = v; break;
                default:
            }
        }
        return new PredictionPerformance(tp, fp, tn, fn);
    }

    public PredictionPerformance() {

    }

    public PredictionPerformance(PredictionPerformance perf) {
        this.tp=perf.getTp();
        this.fp=perf.getFp();
        this.tn=perf.getTn();
        this.fn=perf.getFn();
        calc();
    }
    public PredictionPerformance(int tp, int fp, int tn, int fn) {
        this.tp = tp;
        this.fp = fp;
        this.tn = tn;
        this.fn = fn;
        calc();
    }

    public int getTp() {
        return tp;
    }

    public int getFp() {
        return fp;
    }

    public int getTn() {
        return tn;
    }

    public int getFn() {
        return fn;
    }

    public double getF() {
        return f;
    }

    public double getPrecision() {
        return precision;
    }

    public double getRecall() {
        return recall;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public void reset() {
        tp = fp = tn = fn = 0;
        accuracy=0d; precision=0d; recall=0d;
    }

    public void merge(PredictionPerformance other) {
        this.tp += other.tp;
        this.fp += other.fp;
        this.tn += other.tn;
        this.fn += other.fn;
        calc();
    }

    public void calc() {

        // first take the smaller class
        final int positive = tp+fn;
        final int negative = tn+fp;
        final int TP, FP, TN, FN;
        if (positive > negative) {
            TP = tn; FP = fn; TN = tp; FN = fp;
        } else {
            TP=tp; FP=fp; TN=tn; FN=fn;
        }
        // now calculate F score related to the smaller class

        accuracy = ((double) TP + TN) / (TP + FP + TN + FN);
        if (TP + FN == 0) recall = 0d;
        else recall = ((double) tp) / (tp + fn);
        if (TP + FP == 0) precision = 0d;
        else precision = ((double) TP) / (TP + FP);
        if (precision != 0 && recall != 0) {
            f = (2 * precision * recall) / (precision + recall);
        } else {
            f = 0;
        }
    }

}
