package de.unijena.bioinf.lcms.traceextractor;

import de.unijena.bioinf.lcms.align.MoI;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.ProcessedSample;

/**
 * Extract Mass of Interest and calculate a confidence score for each MoI.
 * If two MoIs are related to each other, they can be connected. For connected MoIs their confidence
 * is the maximum of the individual MoIs. As such, either both MoIs are rejected or none of them.
 */
public interface MassOfInterestConfidenceEstimatorStrategy {

    public static final float
            // negative confident mois are immediately rejected
            REJECT = -1,

            // confidence of 0 is used for alignment, but rejected if it does not align
            ACCEPT = 0,
            // confidence above 10 is used for alignment and kept, even if it does not align
            KEEP_FOR_ALIGNMENT = 20,
            // confidence above 100 is used in initial alignment to build the backbone
            CONFIDENT = 100;

    public float estimateConfidence(ProcessedSample sample, ContiguousTrace trace, MoI moi, ConnectRelatedMoIs connector);
}
