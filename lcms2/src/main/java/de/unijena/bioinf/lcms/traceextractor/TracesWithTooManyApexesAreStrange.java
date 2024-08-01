package de.unijena.bioinf.lcms.traceextractor;

import de.unijena.bioinf.lcms.align.MoI;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.ProcessedSample;

public class TracesWithTooManyApexesAreStrange implements MassOfInterestConfidenceEstimatorStrategy{
    @Override
    public float estimateConfidence(ProcessedSample sample, ContiguousTrace trace, MoI moi, ConnectRelatedMoIs connector) {
        if (trace.getSegments().length==1) return 10f;
        if (trace.getSegments().length>=10) return -3*(float)Math.sqrt(trace.getSegments().length); // this one is strange
        return 0f;
    }
}
