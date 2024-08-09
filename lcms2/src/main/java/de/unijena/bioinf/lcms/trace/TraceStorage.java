package de.unijena.bioinf.lcms.trace;

import de.unijena.bioinf.lcms.ScanPointMapping;
import org.dizitart.no2.mvstore.MVSpatialKey;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.rtree.MVRTreeMap;
import org.h2.mvstore.rtree.Spatial;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class TraceStorage implements Iterable<ContiguousTrace>  {

    /*
        TRACE
     */
    public abstract void deleteTrace(int uid);
    public abstract ContiguousTrace getContigousTrace(int uid);
    public abstract Optional<ContiguousTrace> getContigousTrace(double fromMz, double toMz, int scanId);
    public abstract List<ContiguousTrace> getContigousTracesByMass(double fromMz, double toMz);
    public abstract List<ContiguousTrace> getContigousTraces(double fromMz, double toMz, int fromScanId, int toScanId);

    public abstract int numberOfTraces();

    public abstract ContiguousTrace addContigousTrace(ContiguousTrace trace);

    /*
        MS2-Spectrum TO TRACE
     */
    public abstract ContiguousTrace getTraceForMs2(int ms2headerId);
    public abstract void setTraceForMs2(int ms2headerid, int traceId);
    public abstract int[] getMs2ForTrace(int traceId);

    public static class MvTraceStorage extends TraceStorage {
        private MVMap<Integer, ContiguousTrace> traceMap;
        private MVMap<Integer, int[]> trace2ms2;
        private MVMap<Integer, Integer> ms2headers2Traces;
        private MVRTreeMap<Integer> spatialTraceMap;
        private ScanPointMapping mapping;
        private AtomicInteger uids;

        public MvTraceStorage(MVStore storage, ScanPointMapping mapping) {
            this.mapping = mapping;
            this.traceMap = storage.openMap("contiguousTraces",
                    new MVMap.Builder<Integer,ContiguousTrace>().valueType(new ContigousTraceDatatype()));
            this.ms2headers2Traces = storage.openMap("ms2headers2Traces");
            this.trace2ms2 = storage.openMap("trace2ms");
            this.uids = new AtomicInteger(0);
            this.spatialTraceMap = storage.openMap("contiguousTracesSpatialKey", new MVRTreeMap.Builder<>());

        }

        @Override
        public ContiguousTrace getTraceForMs2(int ms2headerId) {
            return traceMap.get(ms2headers2Traces.get(ms2headerId));
        }

        @Override
        public void setTraceForMs2(int ms2headerid, int traceId) {
            ms2headers2Traces.put(ms2headerid, traceId);
            synchronized (this) {
                int[] ints = trace2ms2.get(traceId);
                if (ints == null) trace2ms2.put(traceId, new int[]{ms2headerid});
                else {
                    ints = Arrays.copyOf(ints, ints.length + 1);
                    ints[ints.length - 1] = ms2headerid;
                    trace2ms2.put(traceId, ints);
                }
            }
        }

        @Override
        public int[] getMs2ForTrace(int traceId) {
            int[] xs = trace2ms2.get(traceId);
            if (xs==null) return new int[0];
            else return xs;
        }


        @Override
        public void deleteTrace(int uid) {
            ContiguousTrace t = this.traceMap.get(uid);
            if (t!=null) {
                if (t.endId >= t.startId) {
                    MVSpatialKey key = new MVSpatialKey(t.uid, (float)t.averageMz, (float)t.averageMz, t.startId, t.endId);
                    MVRTreeMap.RTreeCursor<Integer> it = spatialTraceMap.findIntersectingKeys(key);
                    while (it.hasNext()) {
                        Spatial next = it.next();
                        if (next.getId()==uid) {
                            spatialTraceMap.remove(next);
                            break;
                        }
                    }
                }
            }
            this.traceMap.remove(uid);
        }


        @Override
        public Optional<ContiguousTrace> getContigousTrace(double fromMz, double toMz, int scanId) {
            MVSpatialKey key = new MVSpatialKey(0, (float)fromMz, (float)toMz, scanId, scanId);
            MVRTreeMap.RTreeCursor<Integer> it = spatialTraceMap.findIntersectingKeys(key);
            for(Spatial k; it.hasNext();) {
                k = it.next();
                ContiguousTrace tr = traceMap.get((int)k.getId());
                double avgmz = tr.averagedMz();
                if (avgmz <= toMz && avgmz >= fromMz) {
                    return Optional.of(tr.withMapping(mapping));
                }
            }
            return Optional.empty();
        }

        @Override
        public List<ContiguousTrace> getContigousTraces(double fromMz, double toMz, int fromScanId, int toScanId) {
            MVSpatialKey key = new MVSpatialKey(0, (float)fromMz, (float)toMz, fromScanId, toScanId);
            MVRTreeMap.RTreeCursor<Integer> it = spatialTraceMap.findIntersectingKeys(key);
            List<ContiguousTrace> outp = new ArrayList<>();
            for(Spatial k; it.hasNext();) {
                k = it.next();
                ContiguousTrace tr = traceMap.get((int)k.getId());
                double avgmz = tr.averagedMz();
                if (avgmz <= toMz && avgmz >= fromMz /*&& tr.startId >= fromScanId && tr.endId() >= toScanId*/) {
                    outp.add(tr.withMapping(mapping));
                }
            }
            return outp;
        }

        @Override
        public int numberOfTraces() {
            return traceMap.size();
        }


        @Override
        public ContiguousTrace getContigousTrace(int uid) {
            return traceMap.get(uid).withMapping(mapping);
        }



        @Override
        public List<ContiguousTrace> getContigousTracesByMass(double from, double to) {
            final MVSpatialKey key = new MVSpatialKey(0, (float)from, (float)to, Integer.MIN_VALUE, Integer.MAX_VALUE);
            final ArrayList<ContiguousTrace> traces = new ArrayList<>();
            MVRTreeMap.RTreeCursor<Integer> iter = spatialTraceMap.findIntersectingKeys(key);
            while (iter.hasNext()) {
                traces.add(traceMap.get((int)(iter.next().getId())).withMapping(mapping));
            }
            return traces;
        }



        public ContiguousTrace addContigousTrace(ContiguousTrace trace) {
            if (trace.uid >= 0) {
                // just replace entry in map
                traceMap.put(trace.uid, trace);
                return trace;
            }
            while (true) {
                int currentIndex = uids.get();
                MVSpatialKey key = new MVSpatialKey(currentIndex, (float) trace.minMz(), (float) trace.maxMz(), trace.startId(), trace.endId());
                MVRTreeMap.RTreeCursor<Integer> it = spatialTraceMap.findIntersectingKeys(key);
                for (Spatial k; it.hasNext(); ) {
                    k = it.next();
                    ContiguousTrace contiguousTrace = traceMap.get((int)k.getId());
                    if (contiguousTrace.apex() == trace.apex()) {
                        return contiguousTrace.withMapping(mapping);
                    } else {
                        LoggerFactory.getLogger(LCMSStorage.class).warn("Overlapping traces found!");
                    }
                }
                if (uids.compareAndSet(currentIndex, currentIndex + 1)) {
                    ContiguousTrace value = trace.withUID((int)key.getId());
                    spatialTraceMap.add(key, (int)key.getId());
                    traceMap.put((int)key.getId(), value);
                    return value.withMapping(mapping);
                }
            }
        }


        @NotNull
        @Override
        public Iterator<ContiguousTrace> iterator() {
            Iterator<ContiguousTrace> iterator = traceMap.values().iterator();
            return new Iterator<ContiguousTrace>() {
                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public ContiguousTrace next() {
                    return iterator.next().withMapping(mapping);
                }
            };
        }
    }

}
