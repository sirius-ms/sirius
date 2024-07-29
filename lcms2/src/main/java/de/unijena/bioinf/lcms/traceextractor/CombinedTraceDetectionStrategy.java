package de.unijena.bioinf.lcms.traceextractor;

import de.unijena.bioinf.lcms.trace.ProcessedSample;

import java.util.Arrays;
import java.util.List;

public class CombinedTraceDetectionStrategy implements TraceDetectionStrategy {

    private final List<TraceDetectionStrategy> strategies;

    public CombinedTraceDetectionStrategy(List<TraceDetectionStrategy> strategies) {
        this.strategies = strategies;
    }

    public CombinedTraceDetectionStrategy(TraceDetectionStrategy... strategies) {
        this(Arrays.asList(strategies));
    }

    @Override
    public void findPeaksForExtraction(ProcessedSample sample, Extract callback) {
        for (TraceDetectionStrategy strategy : strategies) {
            strategy.findPeaksForExtraction(sample, callback);
        }
    }
}
