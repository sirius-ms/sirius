package de.unijena.bioinf.model.lcms;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.ArrayList;
import java.util.Optional;

public class ChromatogramCache {

    private final TIntObjectHashMap<ChromatographicPeakSet> cache;

    public ChromatogramCache() {
        this.cache = new TIntObjectHashMap<>();
    }

    public Optional<ChromatographicPeak> retrieve(ScanPoint scanPoint) {
        final int keyDown = (int)Math.floor(scanPoint.getMass()*10);
        ChromatographicPeak r = retrieve(scanPoint, keyDown);
        if (r==null) {
            final int keyUp = (int)Math.ceil(scanPoint.getMass()*10);
            if (keyUp!=keyDown) r = retrieve(scanPoint, keyUp);
        }
        return r==null ? Optional.empty() : Optional.of(r);
    }

    private ChromatographicPeak retrieve(ScanPoint dp, int key) {
        ChromatographicPeakSet s = cache.get(key);
        if (s==null) return null;
        return s.retrieve(dp);
    }

    public void add(ChromatographicPeak peak) {
        double mzAt = peak.getMzAt(peak.getSegments().first().apex);
        final int keyDown = (int)Math.floor(mzAt*10);
        final int keyUp = (int)Math.ceil(mzAt*10);
        store(peak, keyDown);
        if (keyUp>keyDown)
            store(peak,keyUp);
    }

    private void store(ChromatographicPeak peak, int key) {
        ChromatographicPeakSet set = cache.get(key);
        if (set==null) {
            set = new ChromatographicPeakSet();
            cache.put(key,set);
        }
        set.store(peak);
    }

    protected static class ChromatographicPeakSet {

        private final ArrayList<ChromatographicPeak> peaks;

        public ChromatographicPeakSet() {
            this.peaks = new ArrayList<>();
        }

        public void store(ChromatographicPeak peak) {
            peaks.add(peak);
        }

        public ChromatographicPeak retrieve(ScanPoint dp) {
            for (ChromatographicPeak p : peaks) {
                int i = p.findScanNumber(dp.getScanNumber());
                if (i<0) continue;
                final double mzDiff = dp.getMass() - p.getMzAt(i);
                final double intDiff = dp.getIntensity() - p.getIntensityAt(i);
                if (mzDiff < 1e-8 && intDiff < 1e-3)
                    return p;
            }
            return null;
        }
    }

}
