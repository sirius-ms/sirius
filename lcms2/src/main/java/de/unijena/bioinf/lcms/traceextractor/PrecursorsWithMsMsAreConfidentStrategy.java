package de.unijena.bioinf.lcms.traceextractor;

import de.unijena.bioinf.lcms.align2.MoI;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.ProcessedSample;

public class PrecursorsWithMsMsAreConfidentStrategy implements MassOfInterestConfidenceEstimatorStrategy{
    @Override
    public float estimateConfidence(ProcessedSample sample, ContiguousTrace trace, MoI moi) {
        if (sample.getTraceStorage().getMs2ForTrace(trace.getUid()).length>0) {
            return 30f;
        } else return 0f;
    }
}
