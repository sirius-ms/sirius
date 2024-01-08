package de.unijena.bioinf.lcms.traceextractor;

import de.unijena.bioinf.lcms.align2.MoI;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.ProcessedSample;

import java.util.List;

public class MassOfInterestCombinedStrategy implements MassOfInterestConfidenceEstimatorStrategy{

    private final MassOfInterestConfidenceEstimatorStrategy[] strategies;

    public MassOfInterestCombinedStrategy(MassOfInterestConfidenceEstimatorStrategy... strategies) {
        this.strategies = strategies;
    }

    @Override
    public float estimateConfidence(ProcessedSample sample, ContiguousTrace trace, MoI moi) {
        float x=0f;
        for (MassOfInterestConfidenceEstimatorStrategy strategy : strategies) {
            x += strategy.estimateConfidence(sample,trace,moi);
        }
        return x;
    }
}
