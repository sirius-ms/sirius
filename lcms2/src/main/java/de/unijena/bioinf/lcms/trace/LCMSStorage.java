package de.unijena.bioinf.lcms.trace;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.lcms.LCMSStorageFactory;
import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.datatypes.SpectrumDatatype;
import de.unijena.bioinf.lcms.spectrum.Ms1SpectrumHeader;
import de.unijena.bioinf.lcms.spectrum.Ms2SpectrumHeader;
import de.unijena.bioinf.lcms.spectrum.MsSpectrumHeaderDatatype;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.h2.mvstore.*;
import org.h2.mvstore.rtree.MVRTreeMap;
import org.h2.mvstore.rtree.SpatialKey;
import org.h2.mvstore.type.DataType;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class LCMSStorage implements Iterable<ContiguousTrace> {

    private static Deviation DEFAULT_DEVIATION = new Deviation(10);

    public static LCMSStorageFactory temporaryStorage() throws IOException {
        return () -> {
            final File tempFile = File.createTempFile("sirius", ".mvstore");
            tempFile.deleteOnExit();
            return new MVTraceStorage(tempFile.getAbsolutePath());
        };
    }

    public static LCMSStorageFactory persistentStorage(File filename) throws IOException {
        return () -> new MVTraceStorage(filename.getAbsolutePath());
    }

    public abstract TraceNode getTraceNode(ContiguousTrace trace);
    public abstract TraceNode getTraceNode(int id);

    public abstract ContiguousTrace addContigousTrace(ContiguousTrace trace);

    public Optional<ContiguousTrace> getContigousTrace(double mz, int scanId) {
        double v = DEFAULT_DEVIATION.absoluteFor(mz);
        return getContigousTrace(mz-v, mz+v, scanId);
    }

    public abstract Optional<ContiguousTrace> getContigousTrace(double fromMz, double toMz, int scanId);

    public abstract List<ContiguousTrace> getContigousTraces(double fromMz, double toMz, int fromScanId, int toScanId);

    public abstract void addSpectrum(Ms1SpectrumHeader header, SimpleSpectrum spectrum);
    public abstract SimpleSpectrum getSpectrum(int id);

    public abstract Ms2SpectrumHeader addMs2Spectrum(Ms2SpectrumHeader header, SimpleSpectrum spectrum);

    public abstract Iterable<Ms2SpectrumHeader> ms2SpectraHeader();
    public abstract Ms2SpectrumHeader ms2SpectrumHeader(int id);
    public abstract SimpleSpectrum getMs2Spectrum(int id);


    public abstract ContiguousTrace getContigousTrace(int uid);

    public abstract int numberOfScans();

    public abstract int numberOfTraces();

    public abstract void update(TraceNode meta);

    public abstract boolean isTraceAlreadyInChain(ContiguousTrace trace);

    public abstract Iterator<ContiguousTrace> traceByConfidence();

    public abstract List<ContiguousTrace> getContigousTracesByMass(double from, double to);

    public abstract TraceChain addTraceChain(TraceChain chain);

    public abstract Iterator<TraceChain> chains();

    public abstract void updateTraceChain(TraceChain chain);

    /**
     * A storage in inactive mode should not consume much memory, i.e. all data of the storage
     * should be written to disc and the cache should be emptied.
     * The storage cannot be read or write to while being inactive.
     * @param inactive enable the inactive memory safing mode when true. Disabling it when false.
     */
    public abstract void setLowMemoryInactiveMode(boolean inactive);

    /**
     * @return true if the storage is inactive. The storage cannot be read or write to while being inactive.
     */
    public abstract boolean isInactive();

    /*
    TODO: we should make scanpointmapping mutable :/
    its weird to set it afterwards
     */
    public abstract void setMapping(ScanPointMapping mapping);

    public abstract Optional<TraceChain> getChainFor(ContiguousTrace tr);

    public abstract void deleteTrace(int uid);

}

class MVTraceStorage extends LCMSStorage {

    private MVStore storage;
    private MVMap<Integer, ContiguousTrace> traceMap;
    private MVRTreeMap<Integer> spatialTraceMap;
    private MVMap<Integer, TraceNode> nodeMap;

    private MVMap<Integer, SimpleSpectrum> spectraMap, ms2SpectraMap;
    private MVMap<Integer, Ms2SpectrumHeader> ms2headers;
    private MVMap<Integer, Ms1SpectrumHeader> ms1Headers;

    private MVMap<Integer, int[]> chains;

    private MVMap<Integer, Integer> trace2chain;

    private ScanPointMapping mapping;

    private AtomicInteger uids, chainIds, ms2spectraIds;

    private int cacheSizeInMegabytes;

    public MVTraceStorage(String file) {
        MVStore.Builder builder = new MVStore.Builder();
        this.cacheSizeInMegabytes = getDefaultCacheSize();
        this.storage = builder.fileName(file).cacheSize(cacheSizeInMegabytes).open();
        this.traceMap = storage.openMap("contiguousTraces",
                new MVMap.Builder<Integer,ContiguousTrace>().valueType(new ContigousTraceDatatype()));
                this.spatialTraceMap = storage.openMap("contiguousTracesSpatialKey", new MVRTreeMap.Builder<>());
        this.nodeMap = storage.openMap("traceNodes");
        System.out.println(nodeMap.getKeyType());
        this.trace2chain = storage.openMap("trace2chain");
        this.chains = storage.openMap("chains");
        this.spectraMap = storage.openMap("spectra",
                new MVMap.Builder<Integer,SimpleSpectrum>().valueType(new SpectrumDatatype()));
        this.ms2headers = storage.openMap("ms2headers", new MVMap.Builder<Integer,Ms2SpectrumHeader>().valueType(new MsSpectrumHeaderDatatype()));
        this.ms2SpectraMap = storage.openMap("ms2spectraMap",
                new MVMap.Builder<Integer,SimpleSpectrum>().valueType(new SpectrumDatatype()));
        this.ms1Headers = storage.openMap("ms1headerMap", new MVMap.Builder<Integer,Ms1SpectrumHeader>().valueType(new MsSpectrumHeaderDatatype()));
        this.uids = new AtomicInteger(traceMap.size());
        this.chainIds = new AtomicInteger(this.chains.size());
        this.ms2spectraIds = new AtomicInteger(ms2headers.size());
    }

    private static int getDefaultCacheSize() {
        int numberOfThreads = SiriusJobs.getGlobalJobManager().getCPUThreads();
        long maximumMemoryAvailable = Runtime.getRuntime().maxMemory()/(1024*1024);
        long memoryPerCore = (maximumMemoryAvailable-2048)/numberOfThreads;
        return (int)Math.max(16, Math.min(memoryPerCore, 1024));
    }

    public ScanPointMapping getMapping() {
        return mapping;
    }

    public void setMapping(ScanPointMapping mapping) {
        this.mapping = mapping;
    }

    @Override
    public Optional<TraceChain> getChainFor(ContiguousTrace tr) {
        Integer i = this.trace2chain.get(tr.uniqueId());
        if (i==null) return Optional.empty();
        final int[] trids = chains.get(i);
        return Optional.of(new TraceChain(i, Arrays.stream(trids).mapToObj(x->getContigousTrace(x)).toArray(ContiguousTrace[]::new)));
    }

    @Override
    public void deleteTrace(int uid) {
        ContiguousTrace t = this.traceMap.get(uid);
        if (t!=null) {
            if (t.endId >= t.startId) {
                SpatialKey key = new SpatialKey(t.uid, (float)t.averageMz, (float)t.averageMz, t.startId, t.endId);
                Iterator<SpatialKey> it = spatialTraceMap.findIntersectingKeys(key);
                while (it.hasNext()) {
                    SpatialKey next = it.next();
                    if (next.getId()==uid) {
                        spatialTraceMap.remove(next);
                        break;
                    }
                }
            }
        }
        this.traceMap.remove(uid);
        this.trace2chain.remove(uid);
        this.nodeMap.remove(uid);
    }

    @Override
    public Optional<ContiguousTrace> getContigousTrace(double fromMz, double toMz, int scanId) {
        SpatialKey key = new SpatialKey(0, (float)fromMz, (float)toMz, scanId, scanId);
        Iterator<SpatialKey> it = spatialTraceMap.findIntersectingKeys(key);
        for(SpatialKey k; it.hasNext();) {
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
        SpatialKey key = new SpatialKey(0, (float)fromMz, (float)toMz, fromScanId, toScanId);
        Iterator<SpatialKey> it = spatialTraceMap.findIntersectingKeys(key);
        List<ContiguousTrace> outp = new ArrayList<>();
        for(SpatialKey k; it.hasNext();) {
            k = it.next();
            ContiguousTrace tr = traceMap.get((int)k.getId());
            double avgmz = tr.averagedMz();
            if (avgmz <= toMz && avgmz >= fromMz && tr.apex() >= fromScanId && tr.apex() <= toScanId) {
                outp.add(tr.withMapping(mapping));
            }
        }
        return outp;
    }

    @Override
    public void addSpectrum(Ms1SpectrumHeader header, SimpleSpectrum spectrum) {
        spectraMap.put(header.getUid(), spectrum);
        ms1Headers.put(header.getUid(), header);
    }

    @Override
    public SimpleSpectrum getSpectrum(int id) {
        return spectraMap.get(id);
    }

    @Override
    public Ms2SpectrumHeader addMs2Spectrum(Ms2SpectrumHeader header, SimpleSpectrum spectrum) {
        int id = ms2spectraIds.incrementAndGet();
        Ms2SpectrumHeader ms2SpectrumHeader = header.withUid(id);
        ms2SpectraMap.put(id, spectrum);
        ms2headers.put(id, ms2SpectrumHeader);
        return ms2SpectrumHeader;
    }

    @Override
    public Iterable<Ms2SpectrumHeader> ms2SpectraHeader() {
        return this.ms2headers.values();
    }

    @Override
    public Ms2SpectrumHeader ms2SpectrumHeader(int id) {
        return this.ms2headers.get(id);
    }

    @Override
    public SimpleSpectrum getMs2Spectrum(int id) {
        return this.ms2SpectraMap.get(id);
    }

    @Override
    public ContiguousTrace getContigousTrace(int uid) {
        return traceMap.get(uid).withMapping(mapping);
    }

    @Override
    public int numberOfScans() {
        return mapping.length();
    }

    @Override
    public int numberOfTraces() {
        return traceMap.size();
    }

    @Override
    public void update(TraceNode meta) {
        nodeMap.put(meta.uid, meta);
    }

    @Override
    public boolean isTraceAlreadyInChain(ContiguousTrace trace) {
        return trace2chain.containsKey(trace.uniqueId());
    }

    @Override
    public Iterator<ContiguousTrace> traceByConfidence() {
        MVMap<ConfidentTraceIndex, Integer> nodeByConfidence = storage.openMap("nodeByConfidence");
        nodeByConfidence.clear();
        for (TraceNode node : nodeMap.values()) {
            nodeByConfidence.put(new ConfidentTraceIndex(node.confidenceScore, node.uid), node.uid);
        }
        return nodeByConfidence.values().stream().map(this::getContigousTrace).iterator();
    }

    @Override
    public List<ContiguousTrace> getContigousTracesByMass(double from, double to) {
        final SpatialKey key = new SpatialKey(0, (float)from, (float)to, Integer.MIN_VALUE, Integer.MAX_VALUE);
        final ArrayList<ContiguousTrace> traces = new ArrayList<>();
        MVRTreeMap.RTreeCursor iter = spatialTraceMap.findIntersectingKeys(key);
        while (iter.hasNext()) {
            traces.add(traceMap.get((int)(iter.next().getId())).withMapping(mapping));
        }
        return traces;
    }

    @Override
    public TraceChain addTraceChain(TraceChain chain) {
        int[] ids = chain.getTraceIds();
        int chainId = chainIds.incrementAndGet();
        chains.put(chainId, ids);
        for (int id : ids) {
            trace2chain.put(id, chainId);
        }
        return chain.withUid(chainId);

    }


    @Override
    public void updateTraceChain(TraceChain chain) {
        int[] ids = chain.getTraceIds();
        int chainId = chain.getUid();
        chains.put(chainId, ids);
        for (int id : ids) {
            trace2chain.put(id, chainId);
        }
    }

    private boolean inactiveMode=false;
    @Override
    public void setLowMemoryInactiveMode(boolean inactive) {
        if (inactive==this.inactiveMode) return;
        if (inactive) {
            this.inactiveMode = true;
            storage.setCacheSize(1); // hacky workaround for clearing the cache
        } else {
            this.inactiveMode = false;
            storage.setCacheSize(this.cacheSizeInMegabytes); // restore the cache
        }
    }

    @Override
    public boolean isInactive() {
        return this.inactiveMode;
    }

    @Override
    public Iterator<TraceChain> chains() {
        final Cursor<Integer, int[]> cursor = chains.cursor(0);
        return new Iterator<TraceChain>() {

            @Override
            public boolean hasNext() {
                return cursor.hasNext();
            }

            @Override
            public TraceChain next() {
                int value = cursor.next();
                int[] traces = cursor.getValue();
                return new TraceChain(value, Arrays.stream(traces).mapToObj(x->getContigousTrace(x)).toArray(ContiguousTrace[]::new));
            }
        };
    }

    @Override
    public TraceNode getTraceNode(ContiguousTrace trace) {
        return getTraceNode(trace.uniqueId());
    }
    @Override
    public TraceNode getTraceNode(int id) {
        TraceNode traceNode = nodeMap.get(id);
        if (traceNode==null) {
            traceNode = new TraceNode(id, 0f);
            TraceNode old = nodeMap.putIfAbsent(traceNode.uid, traceNode);
            return old==null ? traceNode : old;
        } else return traceNode;
    }

    public ContiguousTrace addContigousTrace(ContiguousTrace trace) {
        if (trace.uid >= 0) {
            // just replace entry in map
            traceMap.put(trace.uid, trace);
            return trace;
        }
        while (true) {
            int currentIndex = uids.get();
            SpatialKey key = new SpatialKey(currentIndex, (float) trace.minMz(), (float) trace.maxMz(), trace.startId(), trace.endId());
            Iterator<SpatialKey> it = spatialTraceMap.findIntersectingKeys(key);
            for (SpatialKey k; it.hasNext(); ) {
                k = it.next();
                ContiguousTrace contiguousTrace = traceMap.get((int)k.getId());
                if (contiguousTrace.apex() == trace.apex()) {
                    return contiguousTrace;
                } else {
                    LoggerFactory.getLogger(LCMSStorage.class).warn("Overlapping traces found!");
                }
            }
            if (uids.compareAndSet(currentIndex, currentIndex + 1)) {
                ContiguousTrace value = trace.withUID((int)key.getId());
                spatialTraceMap.add(key, (int)key.getId());
                traceMap.put((int)key.getId(), value);
                return value;
            }
        }
    }


    private static class ConfidentTraceIndex implements Comparable<ConfidentTraceIndex>, Serializable {
        private final float confidence;
        private final int traceId;

        public ConfidentTraceIndex(float confidence, int traceId) {
            this.confidence = confidence;
            this.traceId = traceId;
        }

        @Override
        public int compareTo(@NotNull MVTraceStorage.ConfidentTraceIndex o) {
            int c = Double.compare(confidence, o.confidence);
            if (c==0) c = Integer.compare(traceId, o.traceId);
            return c;
        }
    }

    @NotNull
    @Override
    public Iterator<ContiguousTrace> iterator() {
        return traceMap.values().iterator();
    }
}