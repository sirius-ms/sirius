package de.unijena.bioinf.lcms.adducts;

import de.unijena.bioinf.ChemistryBase.math.NormalDistribution;
import de.unijena.bioinf.ChemistryBase.math.Statistics;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Normalization;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.ms.persistence.model.core.feature.AbstractAlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.MergedMSnSpectrum;
import de.unijena.bioinf.ms.persistence.model.core.trace.AbstractTrace;
import de.unijena.bioinf.ms.persistence.model.core.trace.MergedTrace;
import de.unijena.bioinf.ms.persistence.model.core.trace.SourceTrace;
import de.unijena.bioinf.ms.persistence.model.core.trace.TraceRef;
import de.unijena.bionf.spectral_alignment.ModifiedCosine;
import de.unijena.bionf.spectral_alignment.SpectralSimilarity;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.apache.commons.math.stat.correlation.PearsonsCorrelation;

import java.util.List;
import java.util.Optional;

public class Scorer {

    public static record Correlation(double correlation, int datapoints) {
        short n() {
            if (datapoints>Short.MAX_VALUE) return Short.MAX_VALUE;
            else return (short)datapoints;
        }
        float p() {
            return (float)correlation;
        }
    }

    private final static double DEFAULT_SCORE = 1;

    public final static float SCORE_BONUS_FOR_SIMPLE_EDGES = 2;

    private final NormalDistribution rtDist;

    public Scorer(double rtStandardDeviation) {
        rtDist = new NormalDistribution(0, rtStandardDeviation*rtStandardDeviation);
    }

    public void computeScore(ProjectSpaceTraceProvider provider, AdductEdge edge) {
        AlignedFeatures left = edge.left.getFeature();
        AlignedFeatures right = edge.right.features;
        // simple rtScore

        edge.rtScore = (float)(rtDist.getErrorProbability(edge.left.getRetentionTime() - edge.right.getRetentionTime()));

        if (left.getTraceReference().isPresent() && right.getTraceReference().isPresent()) {
            computeScoreFromCorrelation(provider, edge, left, right);
        }
    }

    public void computeScoreFromCorrelation(ProjectSpaceTraceProvider provider, AdductEdge edge, AlignedFeatures left, AlignedFeatures right) {
        // there are two types of correlation we can use:

        // 1.) we correlate the ratios between each feature across all samples
        Long2DoubleMap leftInts = provider.getIntensities(left);
        Long2DoubleMap rightInts = provider.getIntensities(right);
        Correlation acrossSamples = correlateAcrossSamples(leftInts, rightInts);
        edge.extraSampleCorrelation = acrossSamples.p();
        edge.extraSampleCorrelationCount = acrossSamples.n();
        // 2.) we correlate the merged traces with each other
        Optional<MergedTrace> l = provider.getMergeTrace(left);
        Optional<MergedTrace> r = provider.getMergeTrace(right);
        if (l.isPresent() && r.isPresent()) {
            Correlation pearson = correlateTraces(left.getTraceReference().get(),  l.get(), right.getTraceReference().get(), r.get());
            edge.interSampleCorrelation = pearson.p();
            edge.interSampleCorrelationCount = pearson.n();
        }
        // 3.) we correlate the most-intensive representatives of both
        Correlation repr = correlateRepresentatives(provider, left, right, leftInts, rightInts);
        edge.interSampleCorrelationRepresentative = repr.p();
        edge.interSampleCorrelationRepresentativeCount = repr.n();
    }

    public static Correlation correlateRepresentatives(TraceProvider provider, AbstractAlignedFeatures left, AbstractAlignedFeatures right, Long2DoubleMap leftInts, Long2DoubleMap rightInts) {
        LongOpenHashSet ks = new LongOpenHashSet(leftInts.keySet());
        ks.retainAll(rightInts.keySet());
        double maxIntens = 0d;
        long bestRun = -1;
        for (long key : ks) {
            double i = Math.sqrt(leftInts.get(key) * rightInts.get(key));
            if (i > maxIntens) {
                bestRun = key;
                maxIntens = i;
            }
        }
        if (maxIntens>0) {
            Optional<Pair<TraceRef, SourceTrace>> l = provider.getSourceTrace(left, bestRun);
            Optional<Pair<TraceRef, SourceTrace>> r = provider.getSourceTrace(right, bestRun);
            if (l.isPresent() && r.isPresent()) {
                return correlateTraces(l.get().left(), l.get().right(), r.get().left(), r.get().right());
            }
        }
        return new Correlation(Double.NaN, 0);
    }

    public static Correlation correlateAcrossSamples(Long2DoubleMap left, Long2DoubleMap right) {
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
        if (xs.size() <= 1 || ys.size()<=1) return new Correlation(Double.NaN, 0);
        // normalize both vectors
        double xsum = xs.doubleStream().sum(), ysum = ys.doubleStream().sum();
        for (int k=0; k < xs.size(); ++k) {
            xs.set(k, xs.getDouble(k)/xsum);
            ys.set(k, ys.getDouble(k)/ysum);
        }
        return new Correlation(Statistics.pearson(xs.toDoubleArray(),ys.toDoubleArray()), xs.size());
    }

    public static Correlation correlateAcrossSamplesUsingMaximumLikelihood(Long2DoubleMap left, Long2DoubleMap right) {
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
        if (xs.size() <= 0 || ys.size()<=0) return new Correlation(Float.NEGATIVE_INFINITY, 0);
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
        if (disjoint>=0.5) return new Correlation(Float.NEGATIVE_INFINITY, 0);
        return new Correlation(logProb, xs.size());
    }

    private static Correlation correlateLargerWithSmaller(TraceRef refLeft, AbstractTrace left, TraceRef refRight, AbstractTrace right) {
        // we first define the range where we do correlation
        // we use the maximum of the minimum range and the FWHM-Range of the larger trace
        int offsetL = refLeft.getStart();
        int offsetR = refRight.getStart();

        int endL = refLeft.getEnd();
        int endR = refRight.getEnd();

        double intensityThresholdFwhm = left.getIntensities().getFloat(refLeft.getApex())*0.5;
        int l = refLeft.getApex();
        while (l > offsetL && left.getIntensities().getFloat(l) > intensityThresholdFwhm) {
            --l;
        }
        int r = refLeft.getApex();
        while (r < endL && left.getIntensities().getFloat(r) > intensityThresholdFwhm) {
            ++r;
        }
        if (r-l >= 3) {
            offsetL = l;
            endL = r;
        }

        // special case: if traces are massively different in length, we do not correlate them
        // because correlation might be very misleading here. Think of a very long trace that has a
        // rectangular shape and a tiny trace with a normal peak shape. The missing ends of the tiny peak
        // will be filled with zeros, giving the tiny peak a "rectangular" shape, too. Such two totally different
        // peaks would get a high correlation, although they should have a low one.
        final int lengthA = endL-offsetL+1;
        final int lengthB = refRight.getEnd()-refRight.getStart()+1;
        if (lengthA > 4*lengthB) return new Correlation(Double.NaN, 0);

        ///////////////////////
        // start correlation
        ///////////////////////
        final int n = Math.max(endL-offsetL+1, refRight.getEnd()-refRight.getStart()+1);
        DoubleArrayList xs = new DoubleArrayList(n), ys = new DoubleArrayList(n);

        int absoluteStart = offsetL + refLeft.getScanIndexOffsetOfTrace();
        int absoluteEnd = endL + refLeft.getScanIndexOffsetOfTrace();
        {
            int apexRight = refRight.getApex() + refRight.getScanIndexOffsetOfTrace();
            if (apexRight < absoluteStart || apexRight > absoluteEnd) return new Correlation(Double.NaN, 0);
        }
        for (int i= absoluteStart; i <= absoluteEnd; ++i) {
            int lindex = i-refLeft.getScanIndexOffsetOfTrace();
            int rindex = i-refRight.getScanIndexOffsetOfTrace();
            if (lindex >= 0 && lindex < left.getIntensities().size()) xs.add(left.getIntensities().getFloat(lindex));
            else xs.add(0d);

            if (rindex >= 0 && rindex < right.getIntensities().size()) ys.add(right.getIntensities().getFloat(rindex));
            else ys.add(0d);
        }
        return new Correlation(Statistics.pearson(xs.toDoubleArray(), ys.toDoubleArray()), xs.size());


    }

    public static Correlation correlateTraces(TraceRef refLeft, AbstractTrace left, TraceRef refRight, AbstractTrace right) {
        if (left.getIntensities().getFloat(refLeft.getApex()) > right.getIntensities().getFloat(refRight.getApex())) {
            return correlateLargerWithSmaller(refLeft, left, refRight, right);
        } else {
            return correlateLargerWithSmaller(refRight, right, refLeft, left);
        }

    }

    public SimpleSpectrum prepareForCosine(AdductNode node, List<MergedMSnSpectrum> ms2Left) {
        SimpleSpectrum spec = Spectrums.mergePeaksWithinSpectrum(Spectrums.mergeSpectra(ms2Left.stream().map(MergedMSnSpectrum::getPeaks).toArray(SimpleSpectrum[]::new)),
                new Deviation(10), true, false);
        final int cut = Spectrums.getFirstPeakGreaterOrEqualThan(spec, node.getMass()-8);
        if (cut < spec.size()) spec = Spectrums.subspectrum(spec, 0, cut);
        return Spectrums.getNormalizedSpectrum(spec, Normalization.L2());
    }

    public boolean hasMinimumMs2Quality(SimpleSpectrum spec) {
        // we expect at least 3 peaks with intensity above 0.05
        // as well as an intensity difference between largest and smallest peak of at least 10
        final double MAX = Spectrums.getMaximalIntensity(spec);
        double MIN = MAX;
        int count = 0;
        for (int i=0; i < spec.size(); ++i) {
            if (spec.getIntensityAt(i)/MAX >= 0.05) ++count;
            MIN = Math.min(MIN, spec.getIntensityAt(i));
        }
        return count >= 3 && MAX/MIN >= 10;
    }

    public void computeMs2Score(AdductEdge adductEdge, SimpleSpectrum left, SimpleSpectrum right) {
        SpectralSimilarity score = new ModifiedCosine(new Deviation(10)).score(left, right, adductEdge.getLeft().getMass(), adductEdge.getRight().getMass(), 1d);
        adductEdge.ms2score = (float)score.similarity;
    }
}
