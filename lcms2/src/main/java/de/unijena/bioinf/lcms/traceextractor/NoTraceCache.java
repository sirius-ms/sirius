package de.unijena.bioinf.lcms.traceextractor;

import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.ProcessedSample;

import java.util.Optional;

/**
 * A caching strategy that caches nothing: every trace is picked freshly and is not stored anywhere.
 * <p>
 * Useful whenever a caller only needs a handful of traces and is going to throw them away immediately, for example
 * in tests. Traces handed out by this cache keep a uid of -1, since they were never added to any storage, and
 * nothing prevents the same peak from being picked into two different traces.
 */
public final class NoTraceCache implements TraceCachingStrategy, TraceCachingStrategy.Cache {

    @Override
    public Cache getCacheFor(ProcessedSample sample) {
        return this;
    }

    @Override
    public ContiguousTrace addTraceToCache(ContiguousTrace trace) {
        return trace;
    }

    @Override
    public Optional<ContiguousTrace> getTraceFromCache(int spectrumId, double mz) {
        return Optional.empty();
    }
}
