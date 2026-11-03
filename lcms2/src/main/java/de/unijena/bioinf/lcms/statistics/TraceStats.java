package de.unijena.bioinf.lcms.statistics;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.align.MoI;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.trace.Trace;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegment;
import de.unijena.bioinf.lcms.traceextractor.MassOfInterestConfidenceEstimatorStrategy;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import lombok.Builder;
import lombok.Value;

import java.io.FileNotFoundException;
import java.io.PrintStream;

@Builder
@Value
public class TraceStats {

    private final double minMz, maxMz;
    private final Deviation averageDeviationWithinFwhm;
    private final double averagePeakWidth;
    private final int numberOfHighQualityTraces;

    public static TraceStats collect(ProcessedSample sample) {
        double minMz = Double.POSITIVE_INFINITY, maxMz = Double.NEGATIVE_INFINITY, peakWidth=0d, absDev=0, relDev=0;
        int count=0, smallc=0, largec=0; int best=0;
        final ScanPointMapping M = sample.getMapping();
        int good=0;
        IntOpenHashSet ids = new IntOpenHashSet();
        for (MoI moi : sample.getStorage().getAlignmentStorage()) {
            if (moi.getConfidence() >= MassOfInterestConfidenceEstimatorStrategy.CONFIDENT) {
                ++good;
            }
            if (moi.getConfidence()>=MassOfInterestConfidenceEstimatorStrategy.KEEP_FOR_ALIGNMENT && moi.hasIsotopes()) ids.add(moi.getTraceId());
        }
        double averagePeakWidth = 0d;
        count = 0;
        for (int id : ids) {
            ContiguousTrace trace = sample.getStorage().getTraceStorage().getContigousTrace(id);
            if (trace.getSegments().length==0) continue;
            minMz = Math.min(trace.averagedMz(), minMz);
            maxMz = Math.max(trace.averagedMz(), maxMz);
            double a = 0; int c=0;
            for (TraceSegment seg : trace.getSegments()) {
                if (seg.rightEdge-seg.leftEdge >= 1) {
                    averagePeakWidth += estimatePeakWidth(sample.getMapping(), trace, seg);
                    ++count;
                }
                for (int i=seg.leftEdge; i <= seg.rightEdge; ++i) {
                    a += Math.abs(trace.mz(i)-trace.averagedMz());
                    ++c;
                }
            }
            a /= c;
            if (trace.averagedMz() < 350) {
                absDev += a;
                ++smallc;
            }
            if (trace.averagedMz() >= 250) {
                relDev += Deviation.fromMeasurementAndReference(trace.averagedMz()+a, trace.averagedMz()).getPpm();
                ++largec;
            }
        }
        double ppm, abs;
        if (largec < 50) {
            ppm = 5;
        } else ppm = relDev/largec;
        if (smallc < 50) {
            abs = ppm*1e-6*200;
        } else abs = absDev/smallc;
        Deviation dev = new Deviation(ppm, abs);
        return TraceStats.builder().minMz(minMz).maxMz(maxMz).averagePeakWidth(averagePeakWidth/count).numberOfHighQualityTraces(good).
                averageDeviationWithinFwhm(dev).build();
    }

    /**
     * The average peak width of a sample's MS1 traces, available as soon as the traces have been picked.
     * <p>
     * Same estimator as {@link #collect}, over a different population and for a different reason. {@code collect}
     * averages over the traces that carry a confident mass of interest with isotopes, which is the better population
     * but only exists once {@code extractMoIsForAlignment} has run. The DIA decision is made before that and needs a
     * peak width to judge whether a window is revisited inside an elution, so it averages over every segmented trace
     * instead. Both call {@link #estimatePeakWidth}, so the quantity cannot drift between the two.
     *
     * @return the intensity weighted retention time standard deviation, a gaussian sigma and not a width at half
     * maximum, or zero if the sample has no segmented trace to measure
     */
    public static double averagePeakWidthOf(ProcessedSample sample) {
        double total = 0d;
        int count = 0;
        for (ContiguousTrace trace : sample.getStorage().getTraceStorage()) {
            for (TraceSegment seg : trace.getSegments()) {
                if (seg.rightEdge - seg.leftEdge >= 1) {
                    total += estimatePeakWidth(sample.getMapping(), trace, seg);
                    ++count;
                }
            }
        }
        return count == 0 ? 0d : total / count;
    }

    private static double estimatePeakWidth(ScanPointMapping mapping, Trace trace, TraceSegment seg) {
        double meanStd = 0d; double intsum = 0d;
        for (int i=seg.leftEdge; i <= seg.rightEdge; ++i) {
            meanStd += mapping.getRetentionTimeAt(i)*trace.intensity(i);
            intsum += trace.intensity(i);
        }
        meanStd /= intsum;
        double variance = 0d;
        for (int i=seg.leftEdge; i <= seg.rightEdge; ++i) {
            variance += Math.pow((mapping.getRetentionTimeAt(i) - meanStd), 2)*trace.intensity(i)/intsum;
        }
        return Math.sqrt(variance);
    }

}
