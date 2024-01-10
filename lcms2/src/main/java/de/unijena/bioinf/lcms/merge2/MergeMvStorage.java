package de.unijena.bioinf.lcms.merge2;

import de.unijena.bioinf.lcms.trace.ContigousTraceDatatype;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.TraceRectangleMap;
import de.unijena.bioinf.lcms.trace.TraceRectangleMapByRVMap;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

public class MergeMvStorage implements MergeStorage{

    private final MVStore store;

    private final MVMap<Integer, MergedTrace> mergedTraces;
    private final MVMap<Integer, ContiguousTrace> underlyingTraces;
    private final TraceRectangleMap rectangleMap;

    private final AtomicInteger traceCounter;

    public MergeMvStorage(MVStore store) {
        this.store = store;
        mergedTraces = store.openMap("mergedTraces", new MVMap.Builder<Integer,MergedTrace>().valueType(new MergedTrace.DataType()));
        underlyingTraces = store.openMap("underlyingTraces", new MVMap.Builder<Integer,ContiguousTrace>().valueType(new ContigousTraceDatatype()));
        rectangleMap = new TraceRectangleMapByRVMap(store, "merge");
        traceCounter = new AtomicInteger();
    }

    @Override
    public TraceRectangleMap getRectangleMap() {
        return rectangleMap;
    }

    @Override
    public void addMerged(MergedTrace mergedTrace) {
        mergedTraces.put(mergedTrace.getUid(), mergedTrace);
    }

    @Override
    public ContiguousTrace addTrace(ContiguousTrace contiguousTrace) {
        ContiguousTrace tr = contiguousTrace.withUID(traceCounter.getAndIncrement());
        underlyingTraces.put(tr.getUid(), tr);
        return tr;
    }

    @Override
    public MergedTrace getMerged(int uid) {
        return mergedTraces.get(uid);
    }

    @Override
    public ContiguousTrace getTrace(int uid) {
        return underlyingTraces.get(uid);
    }

    @Override
    public void removeMerged(int uid) {
        mergedTraces.remove(uid);
    }

    @NotNull
    @Override
    public Iterator<MergedTrace> iterator() {
        return mergedTraces.values().iterator();
    }
}
