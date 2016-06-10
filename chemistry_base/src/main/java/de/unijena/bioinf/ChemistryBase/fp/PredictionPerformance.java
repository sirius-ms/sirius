package de.unijena.bioinf.ChemistryBase.fp;

import java.util.Locale;

/**
 * measures F1-Score, accuracy, recall and precision of the predictions
 */
public final class PredictionPerformance {

    public final static class Modify {

        private Modify(int tp, int fp, int tn, int fn, double pseudoCount) {
            this.tp = tp;
            this.fp = fp;
            this.tn = tn;
            this.fn = fn;
            this.pseudoCount = pseudoCount;
        }

        private Modify(int tp, int fp, int tn, int fn) {
            this(tp,fp,tn,fn,0);
        }

        private int tp,fp,tn,fn;
        private double pseudoCount;

        public PredictionPerformance done() {
            return new PredictionPerformance(tp,fp,tn,fn);
        }

        public PredictionPerformance done(PredictionPerformance performance) {
            performance.set(tp,fp,tn,fn);
            return performance;
        }

        public Modify setPseudoCount(double pseudoCount) {
            this.pseudoCount = pseudoCount;
            return this;
        }

        public Modify update(boolean[] truths, boolean[] predictions) {
            for (int k=0; k < truths.length; ++k) update(truths[k], predictions[k]);
            return this;
        }

        public Modify update(boolean truth, boolean predicted) {
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
            return this;
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

        public double getPseudoCount() {
            return pseudoCount;
        }
    }

    private int tp, fp, tn, fn;
    private double pseudoCount;
    private double f, precision, recall, accuracy, specitivity;

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

    public PredictionPerformance withPseudoCount(double pseudoCount) {
        return new PredictionPerformance(tp,fp,tn,fn, pseudoCount);
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
        this.pseudoCount = perf.pseudoCount;
        calc();
    }

    public PredictionPerformance(int tp, int fp, int tn, int fn) {
        this(tp, fp, tn, fn, 0d);
    }

    public PredictionPerformance(int tp, int fp, int tn, int fn, double pseudoCount) {
        this.tp = tp;
        this.fp = fp;
        this.tn = tn;
        this.fn = fn;
        this.pseudoCount = pseudoCount;
        calc();
    }

    public boolean getSmallerClass() {
        if (tp +fn > tn+fp) return false;
        else return true;
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

    public double getSpecitivity() {return specitivity;}

    public double getPseudoCount() {
        return pseudoCount;
    }

    public int numberOfSamples() {
        return tp+fp+tn+fn;
    }

    public double numberOfSamplesWithPseudocounts() {
        return tp+fp+tn+fn+4*pseudoCount;
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
        final double TP, FP, TN, FN;
        if (positive > negative) {
            TP = tn+pseudoCount; FP = fn+pseudoCount; TN = tp+pseudoCount; FN = fp+pseudoCount;
        } else {
            TP=tp+pseudoCount; FP=fp+pseudoCount; TN=tn+pseudoCount; FN=fn+pseudoCount;
        }

        // now calculate F score related to the smaller class

        accuracy = (TP + TN) / (TP + FP + TN + FN);
        if (TP + FN == 0) recall = 0d;
        else recall = ((double) tp) / (tp + fn);
        if (TN+FP == 0) specitivity = 0d;
        else specitivity = TN/(TN+FP);
        if (TP + FP == 0) precision = 0d;
        else precision = (TP) / (TP + FP);
        if (precision != 0 && recall != 0) {
            f = (2 * precision * recall) / (precision + recall);
        } else {
            f = 0;
        }
    }

}
