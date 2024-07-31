/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
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


import lombok.Getter;
import org.apache.commons.lang3.Range;

import java.util.*;

public interface ChromatographicPeak {

    int numberOfScans();

    double getMzAt(int k);
    double getIntensityAt(int k);
    long getRetentionTimeAt(int k);
    int getScanNumberAt(int k);
    Range<Long> getRetentionTime();
    NavigableMap<Integer, Segment> getSegments();
    ScanPoint getScanPointAt(int k);

    default public boolean samePeak(ChromatographicPeak other) {
        if (this==other) return true;
        if (!getRetentionTime().isOverlappedBy(other.getRetentionTime())) return false;
        ScanPoint u = getApexPeak();
        ScanPoint v = other.getApexPeak();
        return (u.getScanNumber()==v.getScanNumber() && Math.abs(u.getMass()-v.getMass())<0.001 && Math.abs(u.getIntensity()-v.getIntensity())< (u.getIntensity()*0.001) );
    }

    default public ScanPoint getApexPeak() {
        ScanPoint apex = null;
        for (int apexId : getSegments().keySet()) {
            ScanPoint p = getScanPointAt(apexId);
            if (apex==null || p.getIntensity()>apex.getIntensity()) {
                apex = p;
            }
        }
        return apex;
    }

    int findClosestIndexByRt(long rt);

    Optional<Segment> getSegmentWithApexId(int apexId);

    ScanPoint getScanPointForScanId(int scanId);
    default ScanPoint getRightEdge() {
        return getScanPointAt(numberOfScans()-1);
    }
    default ScanPoint getLeftEdge() {
        return getScanPointAt(0);
    }

    /**
     * searches for two minima and a maximum such that the given scanNumber is between the minima and closeby the maximum
     * @return index of the maximum
     */
    public default Optional<Segment> getSegmentForScanId(int scanId) {
        // in theory we can improve the worst-case runtime here
        // but I doubt that it will be faster in average
        for (Segment s : getSegments().values()) {
            if (scanId >= s.getStartScanNumber() && scanId <= s.getEndScanNumber())
                return Optional.of(s);
        }
        return Optional.empty();
    }

    /**
     * returns the scanIndex for a given scanNumber, or -(insertionPoint-1)
     */
    int findScanNumber(int scanNumber);

    /**
     * returns a mutable variant of this peak. This might be or might not be a copy of this object!
     * Use clone() if you want to enforce a copy.
     */
    public default MutableChromatographicPeak mutate() {
        return new MutableChromatographicPeak(this);
    }

    /*
    public default Segment createSegmentFromIndizes(int from, int toInclusive) {
        double intens = 0d; int apx=0;
        for (int i=from; i <= toInclusive; ++i) {
            if (getIntensityAt(i)>intens) {
                apx = i;
                intens = getIntensityAt(i);
            }
        }
        return new Segment(this, from,apx, toInclusive);
    }

     */

    default Set<Integer> scanNumbers() {
        final HashSet<Integer> set = new HashSet<>();
        for (int k=0; k <  numberOfScans(); ++k) {
            set.add(getScanNumberAt(k));
        }
        return set;
    }

    default int index2scanNumber(int index) {
        return getScanNumberAt(index);
    }
    default Range<Integer> index2scanNumber(Range<Integer> indizes) {
        int from = getScanNumberAt(indizes.getMinimum());
        int to = getScanNumberAt(indizes.getMaximum());
        return Range.of(from, to);
    }

    class Segment {

        @Getter
        protected final ChromatographicPeak peak;

        @Getter
        protected final int startIndex, endIndex, apexIndex;
        protected final int fwhmStart, fwhmEnd;

        @Getter
        protected boolean noise;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Segment segment = (Segment) o;
            return apexIndex == segment.apexIndex && peak.equals(segment.peak);
        }

        @Override
        public int hashCode() {
            return Objects.hash(peak, apexIndex);
        }

        Segment(ChromatographicPeak peak, int startIndex, int apexIndex, int endIndex, boolean noise) {
            this.peak = peak;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.apexIndex = apexIndex;
            final Range<Integer> fwhm = calculateFWHM(0.5);
            this.fwhmEnd = fwhm.getMaximum();
            this.fwhmStart = fwhm.getMinimum();
            this.noise = noise;
        }

        void setMinMaxScanIndex(int[] scanIndex, int surrounding) {
            int k=Math.max(0,startIndex-surrounding);
            scanIndex[0] = Math.min(scanIndex[0],peak.getScanNumberAt(k));
            k=Math.min(peak.numberOfScans()-1,endIndex+surrounding);
            scanIndex[1] = Math.max(scanIndex[1],peak.getScanNumberAt(k));
        }

        public double getApexIntensity() {
            return peak.getIntensityAt(apexIndex);
        }

        public String toString() {
            return "Segment(" + peak.getScanNumberAt(startIndex) + " ... " + peak.getScanNumberAt(endIndex) + "), apex = " + peak.getScanNumberAt(apexIndex)+  ", " + (endIndex-startIndex+1) + " spans from " + (retentionTimeSpan().getMinimum()/60000d) + " .. " + (retentionTimeSpan().getMaximum()/60000d)  +  " min over " + retentionTimeWidth()/1000d + " seconds.";
        }

        public long fwhm() {
            return peak.getRetentionTimeAt(fwhmEnd)-peak.getRetentionTimeAt(fwhmStart);
        }
        public long fwhm(double percentile) {
            Range<Integer> r = calculateFWHM(percentile);
            if (r.getMinimum().equals(r.getMaximum())) {
                int a = Math.min(endIndex, r.getMaximum() + 1);
                int b = Math.max(startIndex, r.getMinimum() - 1);
                return Math.min(peak.getRetentionTimeAt(r.getMaximum())-peak.getRetentionTimeAt(a),
                        peak.getRetentionTimeAt(b)-peak.getRetentionTimeAt(r.getMinimum()));
            }
            return peak.getRetentionTimeAt(r.getMaximum())-peak.getRetentionTimeAt(r.getMinimum());
        }

        public int getFwhmStartIndex() {
            return fwhmStart;
        }

        public int getFwhmEndIndex() {
            return fwhmEnd;
        }

        public Range<Integer> calculateFWHMMinPeaks(double threshold, int minPeaks) {
            Range<Integer> range = calculateFWHM(threshold);
            int a = range.getMinimum(), b = range.getMaximum();
            if (b-a+1 >= minPeaks) return range;
            // extend range until it reaches minPeaks
            while (b-a+1 < minPeaks) {
                double intLeft = (a > startIndex) ? peak.getIntensityAt(a-1) : Double.NEGATIVE_INFINITY;
                double intRight = (b < endIndex) ? peak.getIntensityAt(b+1) : Double.NEGATIVE_INFINITY;
                if (Double.isFinite(intLeft) && intLeft>intRight) --a;
                else if (Double.isFinite(intRight)) ++b;
                else break;
            }
            return Range.of(a,b);
        }

        public Range<Integer> calculateFWHM(double threshold) {
            double intApex = peak.getIntensityAt(apexIndex);
            double halveMaximum = intApex*threshold;
            int i,j;
            for (i= apexIndex; i >= startIndex; --i) {
                if (peak.getIntensityAt(i) < halveMaximum)
                    break;
            }
            ++i;
            for (j= apexIndex; j <= endIndex; ++j) {
                if (peak.getIntensityAt(j) < halveMaximum)
                    break;
            }
            --j;
            if (i>j) return Range.of(apexIndex, apexIndex);
            return Range.of(i,j);
        }

        public int getStartScanNumber() {
            return peak.getScanNumberAt(startIndex);
        }

        public int getEndScanNumber() {
            return peak.getScanNumberAt(endIndex);
        }

        public int getApexScanNumber() {
            return peak.getScanNumberAt(apexIndex);
        }

        public long retentionTimeWidth() {
            return peak.getRetentionTimeAt(endIndex)-peak.getRetentionTimeAt(startIndex);
        }

        public Range<Long> retentionTimeSpan() {
            return Range.of(peak.getRetentionTimeAt(startIndex), peak.getRetentionTimeAt(endIndex));
        }

        public boolean samePeak(Segment other) {
            return peak.samePeak(other.peak) && getApexScanNumber()==other.getApexScanNumber();
        }

        public long getApexRt() {
            return peak.getRetentionTimeAt(apexIndex);
        }

        public double getApexMass() {
            return peak.getMzAt(apexIndex);
        }
    }

}
