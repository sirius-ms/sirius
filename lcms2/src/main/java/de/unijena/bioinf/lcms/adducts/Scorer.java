package de.unijena.bioinf.lcms.adducts;

import de.unijena.bioinf.ChemistryBase.math.Statistics;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.trace.MergedTrace;
import de.unijena.bioinf.ms.persistence.model.core.trace.TraceRef;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;

import java.util.Optional;

public class Scorer {

    private final static double DEFAULT_SCORE = 1;

    public Scorer() {
    }

    public double computeScore(ProjectSpaceTraceProvider provider, AdductEdge edge) {
        AlignedFeatures left = edge.left.getFeature();
        AlignedFeatures right = edge.right.features;
        if (left.getTraceRef().isPresent() && right.getTraceRef().isPresent()) {
            return computeScoreFromCorrelation(provider, edge, left, right);
        } else {
            return computeScoreWithoutCorrelation(left, right);
        }
    }

    private double computeScoreWithoutCorrelation(AlignedFeatures left, AlignedFeatures right) {
        if (left.getRetentionTime().compareTo(right.getRetentionTime())==0) {
            return DEFAULT_SCORE;
        } else return Double.NEGATIVE_INFINITY;
    }

    public double computeScoreFromCorrelation(ProjectSpaceTraceProvider provider, AdductEdge edge, AlignedFeatures left, AlignedFeatures right) {
        // there are two types of correlation we can use:

        // 1.) we correlate the ratios between each feature across all samples
        double logProb = correlateAcrossSamples(provider.getIntensities(left), provider.getIntensities(right));
        edge.ratioScore = (float)logProb;
        // 2.) we correlate the merged traces with each other
        Optional<MergedTrace> l = provider.getMergeTrace(left);
        Optional<MergedTrace> r = provider.getMergeTrace(right);
        if (l.isPresent() && r.isPresent()) {
            double pearson = correlateTraces(left.getTraceRef().get(),  l.get(), right.getTraceRef().get(), r.get());
            edge.correlationScore = (float)pearson;
        }
        return 0d;
    }

    private double correlateAcrossSamples(Long2DoubleMap left, Long2DoubleMap right) {
        DoubleArrayList xs = new DoubleArrayList(), ys = new DoubleArrayList();
        left.keySet().forEach(key->{
            xs.add(left.getOrDefault(key,0d));
            ys.add(right.getOrDefault(key,0d));
        });
        right.keySet().forEach(key->{
            if (!left.containsKey(key)) {
                xs.add(0d);
                ys.add(right.getOrDefault(key, 0d));
            }
        });
        // normalize both vectors
        double xsum = xs.doubleStream().sum(), ysum = ys.doubleStream().sum();
        for (int k=0; k < xs.size(); ++k) {
            xs.set(k, xs.getDouble(k)/xsum);
            ys.set(k, ys.getDouble(k)/ysum);
        }
        // compute likelihood
        final double sigmaA = 0.05, sigmaR = 0.1;
        double logProb = 0d;
        double disjoint = 0d;
        for (int k=0; k < xs.size(); ++k) {
            if (xs.getDouble(k)+ys.getDouble(k) < 0.01) {
                continue; // ignore super tiny peaks
            }
            final double larger = Math.max(xs.getDouble(k), ys.getDouble(k));
            if (xs.getDouble(k)<=0 || ys.getDouble(k)<=0) disjoint += larger;
            final double delta = xs.getDouble(k)-ys.getDouble(k);
            final double peakProbability = Math.exp(-(delta * delta) / (2 * (sigmaA * sigmaA + larger * larger * sigmaR * sigmaR))) / (2 * Math.PI * larger * sigmaR * sigmaA);
            final double sigma = larger*2*sigmaR + 2*sigmaA;
            logProb += Math.log(peakProbability) - Math.log(Math.exp(-(sigma*sigma)/(2*(sigmaA*sigmaA + larger*larger*sigmaR*sigmaR)))/(2*Math.PI*larger*sigmaR*sigmaR));
        }
        if (disjoint>=0.5) return Float.NEGATIVE_INFINITY;
        return logProb;
    }

    private double correlateTraces(TraceRef refLeft, MergedTrace left, TraceRef refRight, MergedTrace right) {
        int offsetL = refLeft.getStart();
        int offsetR = refRight.getStart();
        // complete correlation
        final int n = Math.max(refLeft.getEnd()-refLeft.getStart(), refRight.getEnd()-refRight.getStart());
        DoubleArrayList xs = new DoubleArrayList(n), ys = new DoubleArrayList(n);
        for (int i= Math.min(offsetL,offsetR); i <= Math.max(refLeft.getEnd(),refRight.getEnd()); ++i) {
            if (i >= offsetL && i <= refLeft.getEnd()) xs.add(left.getIntensities().getFloat(i));
            else xs.add(0d);

            if (i >= offsetR && i <= refRight.getEnd()) ys.add(right.getIntensities().getFloat(i));
            else ys.add(0d);
        }
        return Statistics.pearson(xs.toDoubleArray(), ys.toDoubleArray());

    }
}
