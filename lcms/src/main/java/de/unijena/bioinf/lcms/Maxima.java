package de.unijena.bioinf.lcms;

import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.List;

public class Maxima {

    protected double[] signal;

    protected double[] smoothedSignal;

    MaximumNode root;

    public Maxima(double[] signal) {
        this.signal = signal;
        root = new MaximumNode(0, signal.length);
    }

    public int[] getMaximaLocations() {
        TIntArrayList indizes = new TIntArrayList();
        root.traverse(indizes);
        return indizes.toArray();
    }

    // LEGACY support
    public Extrema toExtrema() {
        int[] indizes = getMaximaLocations();
        // find minima between all indizes

        Extrema extrema = new Extrema();
        if (indizes[0]==0) {
            // start with maximum. Rare exceptional case
            extrema.addExtremum(0, signal[0]);
        } else {
            // otherwise, find smallest intensity in starting region
            int smi=argmin(signal, 0, indizes[0]);
            extrema.addExtremum(smi, signal[smi]);
            extrema.addExtremum(indizes[0], signal[indizes[0]]);
        }
        for (int i=1; i < indizes.length; ++i) {
            // minimum
            int smi = argmin(signal, indizes[i-1]+1, indizes[i]);
            extrema.addExtremum(smi, signal[smi]);
            // and maximum
            extrema.addExtremum(indizes[i], signal[indizes[i]]);
        }
        return extrema;
    }

    private static int argmin(double[] signal, int from, int to) {
        int smi=from;
        for (int i=from; i < to; ++i) {
            if (signal[i] < signal[smi]) smi = i;
        }
        return smi;
    }

    public boolean split(double absoluteThreshold, double relativeThreshold) {
        return root.split(absoluteThreshold,relativeThreshold);
    }

    public void useSmoothedFilterForGuidance(double[] smoothedSignal) {
        this.smoothedSignal=smoothedSignal;
    }

    protected class MaximumNode {
        protected int apex;
        protected MaximumNode left, right;
        protected int from, to;

        public MaximumNode(int from, int to) {
            this.left = null;
            this.right = null;
            this.from = from;
            this.to = to;
            this.apex = from;
            for (int k=from; k < to; ++k) {
                if (signal[k] > signal[apex]) apex = k;
            }
        }
        public MaximumNode(int apex, int from, int to) {
            this.left = null;
            this.right = null;
            this.from = from;
            this.to = to;
            this.apex = apex;
        }

        protected void traverse(TIntArrayList indizes) {
            if (left!=null) left.traverse(indizes);
            indizes.add(apex);
            if (right!=null) right.traverse(indizes);
        }

        public boolean splitRight(double absoluteThreshold, double relativeThreshold) {
            if (to-apex <= 3) return false;
            double maximumAbsDist = 0d;
            int newApex = apex;
            double[] mins = screen(apex, to, signal);
            double[] minsSmoothed = smoothedSignal==null ? null : screen(apex, to, smoothedSignal);

            for (int k=apex+1; k < to; ++k) {
                double newDist = signal[k] - mins[k-apex];
                if (newDist > maximumAbsDist && newDist >= absoluteThreshold && newDist/signal[k] >= relativeThreshold) {
                    final double smoothedDist = smoothedSignal==null ? Double.MAX_VALUE : smoothedSignal[k] - minsSmoothed[k-apex];
                    if (smoothedSignal==null || (smoothedDist >= absoluteThreshold && smoothedDist/smoothedSignal[k] >= relativeThreshold)) {
                        maximumAbsDist = newDist;
                        newApex = k;
                    }
                }
            }
            if ( newApex != apex ) {
                this.right = new MaximumNode(newApex, apex, to);
                return true;
            } else return false;
        }

        public boolean splitLeft(double absoluteThreshold, double relativeThreshold) {
            if (apex - from  <= 3) return false;
            double maximumAbsDist = 0d;
            int newApex = apex;
            double[] mins = screen(from, apex, signal);
            double[] minsSmoothed = smoothedSignal==null ? null : screen(from, apex, smoothedSignal);

            for (int k=apex-1; k >= from; --k) {
                double newDist = signal[k] - mins[k-from];
                if (newDist > maximumAbsDist && newDist >= absoluteThreshold && newDist/signal[k] >= relativeThreshold) {
                    final double smoothedDist = smoothedSignal==null ? Double.MAX_VALUE : smoothedSignal[k] - minsSmoothed[k-from];
                    if (smoothedSignal==null || (smoothedDist >= absoluteThreshold && smoothedDist/smoothedSignal[k] >= relativeThreshold)) {
                        maximumAbsDist = newDist;
                        newApex = k;
                    }
                }
            }

            if (newApex != apex) {
                this.left = new MaximumNode(newApex, from, apex);
                return true;
            } else return false;
        }

        public boolean split(double absoluteThreshold, double relativeThreshold) {
            boolean changed=false;
            if (left==null) {
                changed |= splitLeft(absoluteThreshold,relativeThreshold);
            } else changed |= left.split(absoluteThreshold,relativeThreshold);
            if (right==null) {
                changed |= splitRight(absoluteThreshold,relativeThreshold);
            } else changed |= right.split(absoluteThreshold,relativeThreshold);
            return changed;
        }
    }


    private double[] screen(int from, int to, double[] signal) {
        double[][] sc = new double[2][to-from];
        sc[0][0] = signal[from];
        sc[1][to-from-1] = signal[to-1];
        for (int k=from+1; k < to; ++k) {
            sc[0][k-from] = Math.min(sc[0][k-from - 1], signal[k]);
        }
        for (int k=to-2; k >= from; --k) {
            sc[1][k-from] = Math.min(sc[1][k-from + 1], signal[k]);
        }
        for (int k=0; k < sc[0].length; ++k) {
            sc[0][k] = Math.max(sc[0][k],sc[1][k]);
        }
        return sc[0];
    }

}
