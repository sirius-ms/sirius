package de.unijena.bioinf.ChemistryBase.fp;

import java.util.Locale;

/**
 * measures F1-Score, accuracy, recall and precision of the predictions
 */
public final class PredictionPerformance {

    public final static class Modify {

        private Modify(double tp, double fp, double tn, double fn, double pseudoCount, boolean relabeling) {
            this.tp = tp;
            this.fp = fp;
            this.tn = tn;
            this.fn = fn;
            this.pseudoCount = pseudoCount;
            this.relabeling = relabeling;
        }

        private Modify(double tp, double fp, double tn, double fn, double pseudoCount) {
            this(tp, fp, tn, fn, pseudoCount, true);
        }

        private Modify(double tp, double fp, double tn, double fn) {
            this(tp,fp,tn,fn,0,true);
        }

        private double tp,fp,tn,fn;
        private double pseudoCount;
        private boolean relabeling;

        public PredictionPerformance done() {
            return new PredictionPerformance(tp,fp,tn,fn,pseudoCount,relabeling);
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

        public Modify update(boolean[] truths, boolean[] predictions,double weight) {
            for (int k=0; k < truths.length; ++k) update(truths[k], predictions[k],weight);
            return this;
        }

        public Modify update(boolean truth, boolean predicted) {
            return update(truth,predicted,1d);
        }

        public Modify update(boolean truth, boolean predicted, double weight) {
            if (truth) {
                if (predicted) {
                    tp+=weight;
                } else {
                    fn+=weight;
                }
            } else{
                if (predicted) {
                    fp+=weight;
                } else {
                    tn+=weight;
                }
            }
            return this;
        }

        public boolean isRelabeling() {
            return relabeling;
        }

        public void setRelabeling(boolean relabeling) {
            this.relabeling = relabeling;
        }

        public double getTp() {
            return tp;
        }

        public void setTp(double tp) {
            this.tp = tp;
        }

        public double getFp() {
            return fp;
        }

        public void setFp(double fp) {
            this.fp = fp;
        }

        public double getTn() {
            return tn;
        }

        public void setTn(double tn) {
            this.tn = tn;
        }

        public double getFn() {
            return fn;
        }

        public void setFn(double fn) {
            this.fn = fn;
        }

        public double getPseudoCount() {
            return pseudoCount;
        }
    }

    private double tp, fp, tn, fn;
    private double pseudoCount;
    private double f, precision, recall, accuracy, specitivity;
    private final boolean allowRelabeling;

    @Override
    public String toString() {
        return String.format(Locale.US, "tp=%.1f\tfp=%.1f\ttn=%.1f\tfn=%.1f\tf1=%.4f\tprecision=%.4f\trecall=%.4f\taccuracy=%.4f", tp,fp,tn,fn,f,precision,recall,accuracy);
    }

    /**
     * If true (default mode), PredictionPerformance will always use the smaller class. If false, PredictionPerformance
     * will use the POSITIVE class.
     */
    public PredictionPerformance withRelabelingAllowed(boolean value) {
        return new PredictionPerformance(tp, fp, tn, fn, pseudoCount, value);
    }

    public Modify modify() {
        return new Modify(tp,fp,tn,fn);
    }

    @Deprecated
    public void set(double tp, double fp, double tn, double fn) {
        this.tp = tp;
        this.fp = fp;
        this.tn = tn;
        this.fn = fn;
        calc();
    }

    public PredictionPerformance withPseudoCount(double pseudoCount) {
        return new PredictionPerformance(tp,fp,tn,fn, pseudoCount, allowRelabeling);
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
        double tp=0, fp=0, tn=0, fn=0;
        for (String s : parts) {
            final int p = s.indexOf('=');
            if ( p < 0) throw new IllegalArgumentException();
            final String token = s.substring(0, p);
            final String value = s.substring(p+1);
            final double v = Double.parseDouble(value);
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
        this(0,0,0,0,0,true);
    }

    public PredictionPerformance(PredictionPerformance perf) {
        this.tp=perf.getTp();
        this.fp=perf.getFp();
        this.tn=perf.getTn();
        this.fn=perf.getFn();
        this.pseudoCount = perf.pseudoCount;
        this.allowRelabeling = perf.allowRelabeling;
        calc();
    }

    public PredictionPerformance(double tp, double fp, double tn, double fn) {
        this(tp, fp, tn, fn, 0d, true);
    }

    public PredictionPerformance(double tp, double fp, double tn, double fn, double pseudoCount) {
        this(tp, fp, tn, fn, pseudoCount, true);
    }

    public PredictionPerformance(double tp, double fp, double tn, double fn, double pseudoCount, boolean allowRelabeling) {
        this.tp = tp;
        this.fp = fp;
        this.tn = tn;
        this.fn = fn;
        this.pseudoCount = pseudoCount;
        this.allowRelabeling = allowRelabeling;
        calc();
    }

    public boolean getSmallerClass() {
        if (tp +fn > tn+fp) return false;
        else return true;
    }

    public double getSmallerClassSize() {
        return Math.min(tp+fn, tn+fp);
    }

    public double getTp() {
        return tp;
    }

    public double getFp() {
        return fp;
    }

    public double getTn() {
        return tn;
    }

    public double getFn() {
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

    public double numberOfSamples() {
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
        final double positive = tp+fn;
        final double negative = tn+fp;
        final double TP, FP, TN, FN;
        if (allowRelabeling && positive > negative) {
            TP = tn+pseudoCount; FP = fn+pseudoCount; TN = tp+pseudoCount; FN = fp+pseudoCount;
        } else {
            TP=tp+pseudoCount; FP=fp+pseudoCount; TN=tn+pseudoCount; FN=fn+pseudoCount;
        }

        // now calculate F score related to the smaller class

        accuracy = (TP + TN) / (TP + FP + TN + FN);
        if (TP + FN == 0) recall = 0d;
        else recall = (TP) / (TP + FN);
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
