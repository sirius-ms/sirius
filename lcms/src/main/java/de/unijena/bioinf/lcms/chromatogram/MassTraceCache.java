package de.unijena.bioinf.lcms.chromatogram;

import com.google.common.collect.Range;
import de.unijena.bioinf.model.lcms.ScanPoint;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.ArrayList;
import java.util.List;

/**
 * stores mass traces such that we never compute the same trace twice.
 */
public class MassTraceCache {


    private final ArrayList<MassTrace> cachedTraces;
    private final TIntObjectHashMap<TraceSet> cache;

    public MassTraceCache() {
        this.cache = new TIntObjectHashMap<>();
        this.cachedTraces = new ArrayList<>();
    }

    public List<MassTrace> getAllMassTraces() {
        return cachedTraces;
    }

    public MassTrace retrieve(ScanPoint scanPoint) {
        final int keyDown = (int)Math.floor(scanPoint.getMass()*10);
        MassTrace r = retrieve(scanPoint, keyDown);
        if (r==null) {
            final int keyUp = (int)Math.ceil(scanPoint.getMass()*10);
            if (keyUp!=keyDown) r = retrieve(scanPoint, keyUp);
        }
        return r==null ? MassTrace.empty() : r;
    }

    private MassTrace retrieve(ScanPoint dp, int key) {
        TraceSet s = cache.get(key);
        if (s==null) return null;
        return s.retrieve(dp);
    }

    public void add(MassTrace peak) {
        int index = cachedTraces.size();
        cachedTraces.add(peak);
        Range<Double> mzAt = peak.getMzRange();
        int keyDown = (int)Math.floor(mzAt.lowerEndpoint()*10);
        final int keyUp = (int)Math.ceil(mzAt.upperEndpoint()*10);
        store(keyDown, index);
        while (keyUp>=++keyDown) {
            store(keyDown, index);
        }
    }

    private void store(int key, int index) {
        TraceSet set = cache.get(key);
        if (set==null) {
            set = new TraceSet();
            cache.put(key,set);
        }
        set.store(index);
    }

    protected class TraceSet {

        private final TIntArrayList traces;

        public TraceSet() {
            this.traces = new TIntArrayList();
        }

        public void store(int index) {
            traces.add(index);
        }

        public MassTrace retrieve(ScanPoint dp) {
            for (int i=0; i < traces.size(); ++i) {
                final MassTrace t = cachedTraces.get(traces.getQuick(i));
                int index = t.findScanNumber(dp.getScanNumber());
                if (index<0) continue;
                final double mzDiff = dp.getMass() - t.getMzAtIndex(index);
                final double intDiff = dp.getIntensity() - t.getIntensityAtIndex(index);
                if (mzDiff < 1e-8 && intDiff < 1e-3)
                    return t;
            }
            return null;
        }
    }

}
