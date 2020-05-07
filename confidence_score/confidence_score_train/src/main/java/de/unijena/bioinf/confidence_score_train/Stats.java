package de.unijena.bioinf.confidence_score_train;

import java.util.Arrays;

/**
 * Created by Marcus Ludwig on 11.03.16.
 */
public class Stats {
    private final Instance[] instances;
    private final int[] classCount;
    private final double[][] rocDataPoints;

    private final double auc;
    public Stats(double[] score, boolean[] trueClass){
        if (score.length!=trueClass.length) throw new IllegalArgumentException("differ in length");
        this.instances = new Instance[score.length];
        this.classCount = new int[2];
        for (int i = 0; i < score.length; i++) {
            final Instance instances = new Instance(score[i], trueClass[i]);
            if (trueClass[i]) classCount[0]++;
            else classCount[1]++;
            this.instances[i]= instances;
        }

        Arrays.sort(instances);
        this.auc = computeAUC();
        this.rocDataPoints = computeROC();
    }

    public double getAUC(){
        return auc;
    }

    /**
     *
     * @return xy dataDoints x=FPR, y=TPR
     */
    public double[][] getRocDataPoints() {
        return rocDataPoints;
    }

    public double[][] computeROC(){
        int tp = classCount[0];
        int fp = classCount[1];
        int tn = 0;
        int fn = 0;

        XY[] sensFPRPairs = new XY[instances.length+1];
        int pos = 0;
        sensFPRPairs[pos++] = new XY(falsePositiveRate(tp, fp, tn, fn), sensitivity(tp, fp, tn, fn));
        for (Instance instance : instances) {
            if (instance.trueClass) {
                tp--;
                fn++;
            } else {
                fp--;
                tn++;
            }
            sensFPRPairs[pos++] = new XY(falsePositiveRate(tp, fp, tn, fn), sensitivity(tp, fp, tn, fn));
        }

        Arrays.sort(sensFPRPairs);

        double[][] dataPoints = new double[sensFPRPairs.length][];
        for (int i = 0; i < sensFPRPairs.length; i++) {
            final XY sensFPRPair = sensFPRPairs[i];
            final double[] dataPoint = new double[]{sensFPRPair.x, sensFPRPair.y};
            dataPoints[i] = dataPoint;
        }

        return dataPoints;
    }

    /*
    computation see:
    Hand, David J.; and Till, Robert J. (2001); A simple generalization of the area under the ROC curve for multiple class classification problems, Machine Learning, 45, 171–186.
    Aˆ = (S0 − n0(n0 + 1)/2) / n0*n1
     */
    private double computeAUC(){
        double S0 = 0d;
        int rank = 1; //TODO starting with 1?
        for (Instance instance : instances) {
            if (instance.trueClass) S0 += rank;
            rank++;
        }

        int n0 = classCount[0];
        int n1 = classCount[1];

        final double auc = (S0 - n0*(n0+1)/2) /(n0*n1);
        return auc;
    }

    /**
     * = racall
     * @return
     */
    private double sensitivity(int tp, int fp, int tn, int fn){
        if (tp + fn == 0) return 0d;
        return  ((double) tp) / (tp + fn);
    }

    /**
     * = 1- specificity
     * @return
     */
    private double falsePositiveRate(int tp, int fp, int tn, int fn){
        if (fp + tn == 0) return 0d;
        return ((double) fp) / (fp + tn);
    }

    private class Instance implements Comparable<Instance>{
        private double score;
        private boolean trueClass;

        public Instance(double score, boolean trueClass) {
            this.score = score;
            this.trueClass = trueClass;
        }

        @Override
        public int compareTo(Instance o) {
            return Double.compare(score, o.score);
        }
    }

    private class XY implements Comparable<XY> {
        private final double x;
        private final double y;

        public XY(double x, double y) {
            this.x = x;
            this.y = y;
        }


        @Override
        public int compareTo(XY o) {
            int comp = Double.compare(x, o.x);
            if (comp!=0) return comp;
            return Double.compare(y, o.y);
        }
    }
}
