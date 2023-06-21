package de.unijena.bioinf.lcms.ionidentity;

import de.unijena.bioinf.model.lcms.ChromatographicPeak;
import de.unijena.bioinf.model.lcms.CorrelationGroup;
import gnu.trove.list.array.TDoubleArrayList;
import org.slf4j.LoggerFactory;
@Deprecated
public class CorrelationGroupScorer {

    private final double[] LEARNED_COEFFICIENTS = new double[]{2.83620147d, 0.24242094d, 2.34225035d};
    private final double BIAS = -1.53573793;
    private final double[] MEANS = new double[]{ 0.83758789d, 15.94664702d,  3.71505765d};
    private final double[] SCALES = new double[]{0.26010621d, 18.41622738d,  3.13352933d};

    public CorrelationGroupScorer() {

    }

    public double predictProbability(CorrelationGroup ionPair) {
        ionPair = ionPair.ensureLargeToSmall();
        TDoubleArrayList[] lists = extractArrays(ionPair);
        return predictProbability(lists[0],lists[1]);
    }

    public double predictProbability(double[] xs, double[] ys) {
        return predictProbability(new TDoubleArrayList(xs), new TDoubleArrayList(ys));
    }
    public double predictProbability(TDoubleArrayList xs, TDoubleArrayList ys) {
        final double correlation = pearson(xs, ys);
        final double maximumLikelihood = maximumLikelihoodIsotopeScore2(xs,ys);
        final double maximumLikelihoodPerPeak = maximumLikelihood/xs.size();
        // logistic regression
        double predictor = ((correlation-MEANS[0])/SCALES[0])*LEARNED_COEFFICIENTS[0] +
                ((maximumLikelihood-MEANS[1])/SCALES[1])*LEARNED_COEFFICIENTS[1] +
                        ((maximumLikelihoodPerPeak-MEANS[2])/SCALES[2])*LEARNED_COEFFICIENTS[2] + BIAS;
        return 1d / (1 + Math.exp(-predictor));
    }

    private TDoubleArrayList[] extractArrays(CorrelationGroup ionPair) {
        TDoubleArrayList a = new TDoubleArrayList(), b = new TDoubleArrayList();
        final ChromatographicPeak large = ionPair.getLeft(), small = ionPair.getRight();
        int start = small.findScanNumber(ionPair.getStartScanNumber()), ende = small.findScanNumber(ionPair.getEndScanNumber());
        if (start < 0 || ende < 0) {
            LoggerFactory.getLogger(CorrelationGroupScorer.class).error(
                    "Two correlated ions do not enclose each other: [start = " + ionPair.getStartScanNumber() + "; end = " + ionPair.getEndScanNumber() + "]. " +
                            large.toString() + "\n and \n" + small.toString() +
                            "\n with segments \n" + ionPair.getLeftSegment().toString() + "\n and \n" + ionPair.getRightSegment());
        }
        if (start < 0) {
            start = 0;
        }
        if (ende < 0) {
            ende = small.numberOfScans()-1;
        }


        for (int i = start; i <= ende; ++i) {
            int j = large.findScanNumber(small.getScanNumberAt(i));
            if (j >= 0) a.add(large.getIntensityAt(j));
            else a.add(0d);
            b.add(small.getIntensityAt(i));
        }

        return new TDoubleArrayList[]{a,b};
    }


    private static double cosine(TDoubleArrayList a, TDoubleArrayList b) {
        double[] x = normalized(a);
        double[] y = normalized(b);
        double cosine = 0d, l=0d, r=0d;
        for (int k=0; k < a.size(); ++k) {
            cosine += x[k]*y[k];
            l += x[k]*x[k];
            r += y[k]*y[k];
        }
        if (l==0 || r == 0) return 0d;
        return cosine/Math.sqrt(l*r);
    }

    public static double kullbackLeibler(TDoubleArrayList a, TDoubleArrayList b, int size) {
        double[] x =normalized(a);
        double[] y = normalized(b);
        double l=0d, r=0d;
        for (int k=0; k < size; ++k) {
            double lx = Math.log(x[k]), ly = Math.log(y[k]);
            l += x[k] * (lx-ly);
            r += y[k] * (ly-lx);
        }
        return l+r;
    }

    public static double[] normalized(TDoubleArrayList a) {
        final double[] b = a.toArray();
        if (b.length<1) return b;
        double sum = a.sum();
        for (int k=0; k < b.length; ++k) {
            b[k] /= sum;
        }
        return b;
    }
    public static double[] normalizedToMax(TDoubleArrayList a) {
        final double[] b = a.toArray();
        if (b.length<1) return b;
        double mx = a.max();
        for (int k=0; k < b.length; ++k) {
            b[k] /= mx;
        }
        return b;
    }


    public static double pearson(TDoubleArrayList a, TDoubleArrayList b) {
        final int n = b.size();
        double meanA=0d;
        double meanB=0d;
        for (int i=0; i < n; ++i) {
            meanA += a.getQuick(i);
            meanB += b.getQuick(i);
        }
        meanA /= n;
        meanB /= n;
        double va=0d, vb=0d, vab=0d;
        for (int i=0; i < n; ++i) {
            final double x = a.getQuick(i)-meanA;
            final double y = b.getQuick(i)-meanB;
            va += x*x;
            vb += y*y;
            vab += x*y;
        }
        if (va*vb==0) return 0d;
        return vab / Math.sqrt(va*vb);
    }

    private final static double LAMBDA2 = 200d, SIGMA_ABS2 = 0.1, SIGMA_REL2 = 0.01;
    public static double maximumLikelihoodIsotopeScore2(TDoubleArrayList xslist, TDoubleArrayList yslist) {
        double currentScore = 0d;
        double maxScore = 0d;
        int maxLenLeft, maxLenRight = 0;
        int apex = findApex(xslist);
        double penalty = 0d;
        final double[] xs = normalizedToMax(xslist);
        final double[] ys = normalizedToMax(yslist);
        final int n = xs.length;
        // we start at position apex and then extend pattern to both sides
        for (int i=apex-1; i >= 0; --i) {
            final double intensityScore = LAMBDA2*xs[i];
            penalty += intensityScore;
            final double delta = xs[i]-ys[i];
            final double matchScore = Math.exp(-(delta*delta)/(2*(SIGMA_ABS2*SIGMA_ABS2 + xs[i]*xs[i]*SIGMA_REL2*SIGMA_REL2)))/(2*Math.PI*xs[i]*SIGMA_REL2*SIGMA_REL2);
            final double score = intensityScore+Math.log(matchScore);
            currentScore += score;
            if (currentScore>=maxScore) {
                maxScore = currentScore;
                maxLenLeft = apex-i;
            }
        }
        currentScore = maxScore;
        // we start at position apex and then extend pattern to both sides
        for (int i=apex+1; i < xs.length; ++i) {
            final double intensityScore = LAMBDA2*xs[i];
            penalty += intensityScore;
            final double delta = xs[i]-ys[i];
            final double matchScore = Math.exp(-(delta*delta)/(2*(SIGMA_ABS2*SIGMA_ABS2 + xs[i]*xs[i]*SIGMA_REL2*SIGMA_REL2)))/(2*Math.PI*xs[i]*SIGMA_REL2*SIGMA_REL2);
            final double score = intensityScore+Math.log(matchScore);
            currentScore += score;
            if (currentScore>=maxScore) {
                maxScore = currentScore;
                maxLenRight = i-apex;
            }
        }
        return maxScore-penalty;
    }

    private static int findApex(TDoubleArrayList xs) {
        int apex=0;
        double apexInt = Double.NEGATIVE_INFINITY;
        for (int i=0; i < xs.size(); ++i) {
            if (xs.getQuick(i)>apexInt) {
                apex = i; apexInt = xs.getQuick(i);
            }
        }
        return apex;
    }


}
