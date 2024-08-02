package de.unijena.bioinf.ms.persistence.model.core.trace;

import lombok.Getter;

/**
 * While merged features point to a trace in a merged run,
 * individual features point to traces in the projected
 * run and in the raw run. As indizes are quite expensive, we just
 * keep both information in one Trace object and TraceRef object.
 */
public class RawTraceRef extends TraceRef {

    int rawStart, rawEnd, rawApex, rawScanIndexOfset;

    public RawTraceRef() {
    }

    public RawTraceRef(long traceId, int scanIndexOffsetOfTrace, int start, int apex, int end, int rawStart, int rawApex, int rawEnd, int rawScanIndexOfset) {
        super(traceId, scanIndexOffsetOfTrace, start, apex, end);
        if (rawStart < 0)
            throw new IllegalArgumentException(String.format("start must be >= 0 (was %d)", rawStart));
        if (rawApex < 0)
            throw new IllegalArgumentException(String.format("apex must be >= 0 (was %d)", rawApex));
        if (rawEnd < 0)
            throw new IllegalArgumentException(String.format("end must be >= 0 (was %d)", rawEnd));
        if (rawApex < rawStart)
            throw new IllegalArgumentException(String.format("start (was %d) must be <= apex (was %d)", rawStart, rawApex));
        if (rawApex > rawEnd)
            throw new IllegalArgumentException(String.format("apex (was %d) must be <= end (was %d)", rawApex, rawEnd));

        this.rawStart = rawStart;
        this.rawEnd = rawEnd;
        this.rawApex = rawApex;
        this.rawScanIndexOfset = rawScanIndexOfset;
    }

    public int getRawStart() {
        return rawStart;
    }

    public void setRawStart(int rawStart) {
        this.rawStart = rawStart;
    }

    public int getRawEnd() {
        return rawEnd;
    }

    public void setRawEnd(int rawEnd) {
        this.rawEnd = rawEnd;
    }

    public int getRawApex() {
        return rawApex;
    }

    public void setRawApex(int rawApex) {
        this.rawApex = rawApex;
    }

    public int getRawScanIndexOfset() {
        return rawScanIndexOfset;
    }

    public void setRawScanIndexOfset(int rawScanIndexOfset) {
        this.rawScanIndexOfset = rawScanIndexOfset;
    }
}
