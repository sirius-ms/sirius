package de.unijena.bioinf.lcms.merge;

import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.TraceRectangleMap;

public interface MergeStorage extends Iterable<MergedTrace> {

    public TraceRectangleMap getRectangleMap();

    public void addMerged(MergedTrace mergedTrace);

    ContiguousTrace addTrace(ContiguousTrace contiguousTrace);

    MergedTrace getMerged(int uid);

    ContiguousTrace getTrace(int uid);

    void removeMerged(int uid);

    public int getFreeIsotopeMergeUid();
}
