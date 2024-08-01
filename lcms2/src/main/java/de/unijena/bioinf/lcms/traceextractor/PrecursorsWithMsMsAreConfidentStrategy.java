package de.unijena.bioinf.lcms.traceextractor;

import de.unijena.bioinf.lcms.align.MoI;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.ProcessedSample;

public class PrecursorsWithMsMsAreConfidentStrategy implements MassOfInterestConfidenceEstimatorStrategy{
    @Override
    public float estimateConfidence(ProcessedSample sample, ContiguousTrace trace, MoI moi, ConnectRelatedMoIs connector) {
        if (sample.getStorage().getTraceStorage().getMs2ForTrace(trace.getUid()).length>0) {
            return 30f;
        } else return 0f;
    }
}
