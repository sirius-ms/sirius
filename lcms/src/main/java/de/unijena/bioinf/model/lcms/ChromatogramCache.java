/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.model.lcms;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ChromatogramCache {

    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

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
        lock.readLock().lock();
        try {
            ChromatographicPeakSet s = cache.get(key);
            if (s == null) return null;
            return s.retrieve(dp);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void add(ChromatographicPeak peak) {
        double mzAt = peak.getMzAt(peak.getSegments().firstKey());
        final int keyDown = (int)Math.floor(mzAt*10);
        final int keyUp = (int)Math.ceil(mzAt*10);
        store(peak, keyDown);
        if (keyUp>keyDown)
            store(peak,keyUp);
    }

    private void store(ChromatographicPeak peak, int key) {
        lock.writeLock().lock();
        try {
            ChromatographicPeakSet set = cache.get(key);
            if (set == null) {
                set = new ChromatographicPeakSet();
                cache.put(key, set);
            }
            set.store(peak);
        } finally {
            lock.writeLock().unlock();
        }
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
                final double mzDiff = Math.abs(dp.getMass() - p.getMzAt(i));
                final double intDiff = Math.abs(dp.getIntensity() - p.getIntensityAt(i));
                if (mzDiff < 1e-8 && intDiff < 1e-3)
                    return p;
            }
            return null;
        }
    }

}
