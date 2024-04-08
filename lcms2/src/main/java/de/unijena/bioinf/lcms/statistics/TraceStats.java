package de.unijena.bioinf.lcms.statistics;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.align.MoI;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegment;
import de.unijena.bioinf.lcms.traceextractor.MassOfInterestConfidenceEstimatorStrategy;
import lombok.Builder;
import lombok.Value;

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
        for (ContiguousTrace trace : sample.getStorage().getTraceStorage()) {
            if (trace.getSegments().length==0) continue;
            minMz = Math.min(trace.averagedMz(), minMz);
            maxMz = Math.max(trace.averagedMz(), maxMz);
            double a = 0; int c=0;
            for (TraceSegment seg : trace.getSegments()) {
                if (seg.rightEdge-seg.leftEdge >= 1) {
                    peakWidth += M.getRetentionTimeAt(seg.rightEdge) - M.getRetentionTimeAt(seg.leftEdge);
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
        int good=0;
        for (MoI moi : sample.getStorage().getAlignmentStorage()) {
            if (moi.getConfidence() >= MassOfInterestConfidenceEstimatorStrategy.CONFIDENT) {
                ++good;
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
        System.out.println(dev);
        return TraceStats.builder().minMz(minMz).maxMz(maxMz).averagePeakWidth(peakWidth/count).numberOfHighQualityTraces(good).
                averageDeviationWithinFwhm(dev).build();
    }
}
