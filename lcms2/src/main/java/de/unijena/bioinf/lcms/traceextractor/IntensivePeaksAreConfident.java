package de.unijena.bioinf.lcms.traceextractor;

import de.unijena.bioinf.lcms.align.MoI;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.ProcessedSample;

public class IntensivePeaksAreConfident implements MassOfInterestConfidenceEstimatorStrategy{
    @Override
    public float estimateConfidence(ProcessedSample sample, ContiguousTrace trace, MoI moi, ConnectRelatedMoIs connector) {
        return moi.getIntensity()*500f;
    }
}
