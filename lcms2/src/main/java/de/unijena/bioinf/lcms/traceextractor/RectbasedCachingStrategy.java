package de.unijena.bioinf.lcms.traceextractor;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.lcms.trace.*;
import org.h2.mvstore.MVStore;

import java.util.Optional;

/**
 * Store rectangles for each trace.
 * Fast lookup of traces using these rectangles.
 */
public class RectbasedCachingStrategy implements TraceCachingStrategy {
    // floating point accuracy for two traces to be equivalent
    private final static double MINDEV = 1e-8;
    @Override
    public Cache getCacheFor(ProcessedSample sample) {
        TraceRectangleMap map = sample.getTraceStorage().getRectangleMap("traceRectangles");
        Deviation allowedMassDeviation = sample.getTraceStorage().getStatistics().getMs1MassDeviationWithinTraces();
        LCMSStorage storage = sample.getTraceStorage();
        return new Cache() {
            @Override
            public ContiguousTrace addTraceToCache(ContiguousTrace trace) {
                ContiguousTrace contiguousTrace = storage.addContigousTrace(trace);
                map.addRect(contiguousTrace.rectWithRts());
                return contiguousTrace;
            }

            @Override
            public Optional<ContiguousTrace> getTraceFromCache(int spectrumId, double mz) {
                final double delta = allowedMassDeviation.absoluteFor(mz);
                double rt = sample.getMapping().getRetentionTimeAt(spectrumId);
                for (Rect rect : map.overlappingRectangle(new Rect(mz-delta,mz+delta, rt, rt, mz))) {
                    if (rect.containsRt(rt) && allowedMassDeviation.inErrorWindow(mz, rect.avgMz)) {
                        ContiguousTrace trace = storage.getContigousTrace(rect.id);
                        if (trace!=null && trace.inRange(spectrumId) && Math.abs(trace.mz(spectrumId)-mz) <= MINDEV) {
                            return Optional.of(trace);
                        }
                    }
                }
                return Optional.empty();
            }
        };
    }
}
