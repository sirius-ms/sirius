package de.unijena.bioinf.lcms.traceextractor;

import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.LCMSStorage;
import de.unijena.bioinf.lcms.trace.ProcessedSample;

import java.util.Optional;

/**
 * Picked trace should be cached, i.e. we have to avoid that a single (scanId, m/z) pair is picked twice
 * in different traces.
 */
public interface TraceCachingStrategy {

    public interface Cache {

        public abstract ContiguousTrace addTraceToCache(ContiguousTrace trace);

        public Optional<ContiguousTrace> getTraceFromCache(int spectrumId, double mz);

    }

    public Cache getCacheFor(ProcessedSample sample);

}
