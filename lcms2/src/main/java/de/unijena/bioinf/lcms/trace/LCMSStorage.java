package de.unijena.bioinf.lcms.trace;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.lcms.LCMSStorageFactory;
import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.align.AlignmentStorage;
import de.unijena.bioinf.lcms.align.MvBasedAlignmentStorage;
import de.unijena.bioinf.lcms.merge.MergeMvStorage;
import de.unijena.bioinf.lcms.merge.MergeStorage;
import de.unijena.bioinf.lcms.spectrum.SpectrumStorage;
import de.unijena.bioinf.lcms.statistics.SampleStats;
import de.unijena.bioinf.lcms.statistics.SampleStatsDataType;
import org.h2.mvstore.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

public abstract class LCMSStorage{

    private static Deviation DEFAULT_DEVIATION = new Deviation(10);

    public static LCMSStorageFactory temporaryStorage() {
        return () -> {
            final File tempFile = File.createTempFile("sirius", ".mvstore");
            tempFile.deleteOnExit();
            return new MVTraceStorage(tempFile.getAbsolutePath());
        };
    }
    public static LCMSStorageFactory persistentStorage(File filename) throws IOException {
        return () -> new MVTraceStorage(filename.getAbsolutePath());
    }

    public abstract TraceRectangleMap getRectangleMap(String prefix);

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

    public abstract void setMapping(ScanPointMapping mapping);

    public abstract void setStatistics(SampleStats stats);
    public abstract SampleStats getStatistics();

    public abstract AlignmentStorage getAlignmentStorage();

    public abstract MergeStorage getMergeStorage();

    public abstract SpectrumStorage getSpectrumStorage();

    public abstract TraceStorage getTraceStorage();
}

class MVTraceStorage extends LCMSStorage {

    private MVStore storage;
    private MVMap<Integer, SampleStats> statisticsObj;
    private ScanPointMapping mapping;
    private MvBasedAlignmentStorage alignmentStorage;
    private TraceStorage.MvTraceStorage traceStorage;
    private SpectrumStorage.MvSpectrumStorage spectrumStorage;

    private int cacheSizeInMegabytes;

    public MVTraceStorage(String file) {
        MVStore.Builder builder = new MVStore.Builder();
        this.cacheSizeInMegabytes = getDefaultCacheSize();
        this.storage = builder.fileName(file).cacheSize(cacheSizeInMegabytes).open();
        this.statisticsObj = storage.openMap("statistics", new MVMap.Builder<Integer,SampleStats>().valueType(new SampleStatsDataType()));
        this.alignmentStorage = new MvBasedAlignmentStorage(storage);
    }

    @Override
    public AlignmentStorage getAlignmentStorage() {
        if (alignmentStorage==null) {
            synchronized (this) {
                if (alignmentStorage==null) alignmentStorage = new MvBasedAlignmentStorage(storage);
            }
        }
        return alignmentStorage;
    }

    @Override
    public SpectrumStorage getSpectrumStorage() {
        if (spectrumStorage==null) {
            synchronized (this) {
                if (spectrumStorage==null) spectrumStorage = new SpectrumStorage.MvSpectrumStorage(storage);
            }
        }
        return spectrumStorage;
    }


    @Override
    public TraceStorage getTraceStorage() {
        if (traceStorage==null) {
            synchronized (this) {
                if (traceStorage==null) traceStorage = new TraceStorage.MvTraceStorage(storage, mapping);
            }
        }
        return traceStorage;
    }


    @Override
    public MergeStorage getMergeStorage() {
        if (mergeMvStorage==null) {
            synchronized (this) {
                if (mergeMvStorage==null) mergeMvStorage = new MergeMvStorage(storage);
            }
        }
        return mergeMvStorage;
    }

    private MergeMvStorage mergeMvStorage;

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
    public void setStatistics(SampleStats stats) {
        statisticsObj.put(0, stats);
    }

    @Override
    public SampleStats getStatistics() {
        return statisticsObj.get(0);
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


    private HashMap<String, TraceRectangleMap> rectangleMaps = new HashMap<>();
    @Override
    public TraceRectangleMap getRectangleMap(String prefix) {
        TraceRectangleMap rects = rectangleMaps.get(prefix);
        if (rects!=null) return rects;
        else {
            synchronized (this) {
                rects = rectangleMaps.get(prefix);
                if (rects!=null) return rects;
                rects = new TraceRectangleMapByRVMap(storage, prefix);
                rectangleMaps.put(prefix, rects);
                return rects;
            }
        }
    }

}