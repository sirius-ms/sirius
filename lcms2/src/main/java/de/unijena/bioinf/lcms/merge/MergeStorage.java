package de.unijena.bioinf.lcms.merge;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.lcms.LCMSStorageFactory;
import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.trace.ContigousTraceDatatype;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.MergedTrace;
import de.unijena.bioinf.lcms.trace.TraceSeparationAndUnificationMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.rtree.MVRTreeMap;
import org.h2.mvstore.rtree.SpatialKey;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public abstract class MergeStorage {

    public static MergeMvStorage createTemporaryStorage(ScanPointMapping mapping) throws IOException {
        final File tempFile = File.createTempFile("sirius", ".mvstore");
        tempFile.deleteOnExit();
        return new MergeMvStorage(tempFile.getAbsolutePath(), mapping);
    }

}

class MergeMvStorage extends MergeStorage {
    private MVStore storage;
    private ScanPointMapping mapping;
    private int cacheSizeInMegabytes;

    TraceSeparationAndUnificationMap traceSeparationAndUnificationMap;

    MVMap<Integer, MergedTrace> mergedTraces;
    MVMap<Integer,ContiguousTrace> finishedTraces;

    public MergeMvStorage(String file, ScanPointMapping mapping) {
        this.mapping = mapping;
        MVStore.Builder builder = new MVStore.Builder();
        this.cacheSizeInMegabytes = getDefaultCacheSize();
        this.storage = builder.fileName(file).cacheSize(cacheSizeInMegabytes).open();
        traceSeparationAndUnificationMap = TraceSeparationAndUnificationMap.getMapFromMvStore(storage, "merge");
        this.mergedTraces = storage.openMap("mergedTraces", new MVMap.Builder<Integer,MergedTrace>().valueType(new ContigousTraceDatatype()));
        this.finishedTraces = storage.openMap("finishedTraces", new MVMap.Builder<Integer,ContiguousTrace>().valueType(new ContigousTraceDatatype()));

    }

    private static int getDefaultCacheSize() {
        int numberOfThreads = SiriusJobs.getGlobalJobManager().getCPUThreads();
        long maximumMemoryAvailable = Runtime.getRuntime().maxMemory()/(1024*1024);
        long memoryPerCore = (maximumMemoryAvailable-2048)/numberOfThreads;
        return (int)Math.max(16, Math.min(memoryPerCore, 1024));
    }

}
