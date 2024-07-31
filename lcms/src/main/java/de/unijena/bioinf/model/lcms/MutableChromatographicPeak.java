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

import gnu.trove.list.array.TIntArrayList;
import org.apache.commons.lang3.Range;
import org.slf4j.LoggerFactory;

import java.util.*;

import static java.util.Collections.binarySearch;

public class MutableChromatographicPeak implements CorrelatedChromatographicPeak {

    private final ArrayList<ScanPoint> scanPoints;
    private int apex;
    private ChromatographicPeak correlatedChromatographicPeak;
    private int correlationStartPoint, correlationEndPoint;
    private double correlation;
    public TreeMap<Integer, Segment> segments;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MutableChromatographicPeak that = (MutableChromatographicPeak) o;
        final ScanPoint u = scanPoints.get(apex), v = that.scanPoints.get(that.apex);
        return u.equals(v);
    }

    @Override
    public int hashCode() {
        return scanPoints.get(apex).hashCode();
    }

    public MutableChromatographicPeak(ChromatographicPeak peak) {
        this.scanPoints = new ArrayList<>();
        for (int i=0; i < peak.numberOfScans(); ++i) scanPoints.add(peak.getScanPointAt(i));
        this.segments = new TreeMap<Integer,Segment>(peak.getSegments());
        if (peak instanceof CorrelatedChromatographicPeak) {
            CorrelatedChromatographicPeak cpeak = (CorrelatedChromatographicPeak)peak;
            correlationEndPoint = cpeak.getCorrelationEndPoint();
            correlationStartPoint = cpeak.getCorrelationStartPoint();
            correlation = cpeak.getCorrelation();
        }
        validate();
    }

    public MutableChromatographicPeak() {
        this.scanPoints = new ArrayList<>();
        this.segments = new TreeMap<>();
    }
    public void extendRight(ScanPoint p) {
        scanPoints.add(p);
    }
    public void extendLeft(ScanPoint p) {
        scanPoints.add(0, p);
    }

    public Segment addSegment(int from, int apex, int to, boolean noise) {
        Segment newSegment = new Segment(this, from, apex, to, noise);
        // segments are not allowed to overlap
        var ceiling = segments.ceilingEntry(newSegment.apexIndex);
        if (ceiling!=null && ceiling.getValue().startIndex < newSegment.endIndex)
            throw new IllegalArgumentException("Segments are not allowed to overlap: " + newSegment + " overlaps with " + ceiling.getValue() );
        var floor = segments.floorEntry(newSegment.apexIndex);
        if (floor != null && floor.getValue().endIndex > newSegment.startIndex)
            throw new IllegalArgumentException("Segments are not allowed to overlap: " + newSegment + " overlaps with " + floor.getValue() );

        this.segments.put(newSegment.apexIndex, newSegment);
        if (maxtensity(from, to) > getIntensityAt(apex)) {
            LoggerFactory.getLogger(MutableChromatographicPeak.class).warn("Apex is not peak with highest intensity!");
        }
        validate();
        return newSegment;
    }

    private double maxtensity(int from, int to) {
        double f = getIntensityAt(from);
        for (int k=from+1; k <= to; ++k) {
            f = Math.max(f, getIntensityAt(k));
        }
        return f;
    }
    private double mintensity(int from, int to) {
        double f = getIntensityAt(from);
        for (int k=from+1; k <= to; ++k) {
            f = Math.min(f, getIntensityAt(k));
        }
        return f;
    }

    /**
     * Split the segment such that left is on the left side and right is on the right side. Return the two segments if this is possible.
     */
    public Optional<Segment[]> tryToDivideSegment(Segment segment, int leftIndex, int rightIndex) {
        if (segment.peak!=this) throw new IllegalArgumentException("Can only split segments that are owned by this peak");
        final double a = getIntensityAt(leftIndex);
        final double b = getIntensityAt(rightIndex);
        // find minimum in between
        double minimum = Math.min(a,b);
        int mindex = -1;
        for (int k=leftIndex+1; k < rightIndex; ++k) {
            if (getIntensityAt(k) < minimum) {
                minimum = getIntensityAt(k);
                mindex = k;
            }
        }
        if (mindex<0) {
            return Optional.empty();
        }
        // find maximum left and right
        double maxLeft = 0d, maxRight = 0d;
        int mxL=-1,mxR=-1;
        for (int k=segment.getStartIndex(); k < mindex; ++k) {
            if (maxLeft < getIntensityAt(k)) {
                maxLeft = getIntensityAt(k);
                mxL = k;
            }
        }
        for (int k=mindex+1; k < segment.getEndIndex(); ++k) {
            if (maxRight < getIntensityAt(k)) {
                maxRight = getIntensityAt(k);
                mxR = k;
            }
        }
        if (mxL<0 || mxR<0) {
            return Optional.empty();
        }
        divideSegment(segment, mindex, mxL, mxR);
        return Optional.of(new Segment[]{
           getSegmentWithApexId(mxL).get(), getSegmentWithApexId(mxR).get()
        });
    }

    public void divideSegment(Segment segment, int minimum, int maximumLeft, int maximumRight) {
        try {
            final TreeMap<Integer,Segment> clone = (TreeMap<Integer, Segment>) segments.clone();
            segments.remove(segment.apexIndex);
            Segment s = addSegment(segment.startIndex, maximumLeft, minimum, segment.noise);
            s = addSegment(minimum, maximumRight, segment.endIndex, segment.noise);
            validate();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            LoggerFactory.getLogger(MutableChromatographicPeak.class).error("Error while splitting segment " + segment + " into two parts: From " + maximumLeft + " to " + minimum + ", and from " + minimum + " to " + maximumRight + ".");
            throw e;
        }
    }

    public Segment joinSegments(Segment left, Segment right) {
        int newApex;
        if (getIntensityAt(left.apexIndex) > getIntensityAt(right.apexIndex))
            newApex = left.apexIndex;
        else newApex = right.apexIndex;
        segments.remove(left.apexIndex);
        segments.remove(right.apexIndex);
        Segment segment = addSegment(Math.min(left.startIndex,right.startIndex),  newApex,Math.max(left.endIndex,right.endIndex), false);
        validate();
        return segment;
    }

    @Override
    public int numberOfScans() {
        return scanPoints.size();
    }

    @Override
    public double getMzAt(int k) {
        return scanPoints.get(k).getMass();
    }

    @Override
    public double getIntensityAt(int k) {
        return scanPoints.get(k).getIntensity();
    }

    @Override
    public long getRetentionTimeAt(int k) {
        return scanPoints.get(k).getRetentionTime();
    }

    @Override
    public int getScanNumberAt(int k) {
        return scanPoints.get(k).getScanNumber();
    }

    @Override
    public Range<Long> getRetentionTime() {
        if (scanPoints.isEmpty())
            return null;
        else return Range.of(getRetentionTimeAt(0), getRetentionTimeAt(scanPoints.size()-1));
    }

    @Override
    public NavigableMap<Integer, Segment> getSegments() {
        return segments;
    }

    @Override
    public ScanPoint getScanPointAt(int k) {
        return scanPoints.get(k);
    }

    @Override
    public int findClosestIndexByRt(long rt) {
        int insertionPoint = binarySearch(scanPoints, new ScanPoint(0, rt, 0, 0), Comparator.comparingLong(ScanPoint::getRetentionTime));
        if (insertionPoint>=0) return insertionPoint;
        final int indexA = -(insertionPoint+1);
        final int indexB = indexA-1;
        long delta = Long.MAX_VALUE;
        if (indexA < scanPoints.size()) {
            delta = Math.abs(scanPoints.get(indexA).getRetentionTime() - rt);
        }
        if (indexB >= 0) {
            long d2=Math.abs(scanPoints.get(indexB).getRetentionTime() - rt);
            if (d2 < delta) return indexB;
        }
        return indexA;
    }

    @Override
    public Optional<Segment> getSegmentWithApexId(int apexId) {
        return Optional.ofNullable(segments.get(apexId));
    }

    @Override
    public ScanPoint getScanPointForScanId(int scanId) {
        int scanNumber = findScanNumber(scanId);
        if (scanNumber>=0) return getScanPointAt(scanNumber);
        else return null;
    }

    @Override
    public int findScanNumber(int scanNumber) {
        return binarySearch(scanPoints, new ScanPoint(scanNumber,0,0,0), Comparator.comparingInt(ScanPoint::getScanNumber));
    }

    public static MutableChromatographicPeak concat(ChromatographicPeak leftTrace, ChromatographicPeak rightTrace) {
        if (!leftTrace.getLeftEdge().equals(rightTrace.getLeftEdge())) {
            throw new IllegalArgumentException("Traces have no shared connection point");
        }
        final MutableChromatographicPeak peaks = new MutableChromatographicPeak();
        for (int k=leftTrace.numberOfScans()-1; k > 0; --k)
            peaks.extendRight(leftTrace.getScanPointAt(k));
        for (int k=0; k < rightTrace.numberOfScans(); ++k)
            peaks.extendRight(rightTrace.getScanPointAt(k));
        return peaks;

    }

    @Override
    public String toString() {
        return "m/z = " + getMzAt(segments.firstKey()) + ", retention time = " + String.format(Locale.US,"%.2f - %.2f min", getLeftEdge().getRetentionTime()/60000d, getRightEdge().getRetentionTime()/60000d) + " scans = " + getLeftEdge().getScanNumber() + " ... " + getRightEdge().getScanNumber() + ", " + numberOfScans() + " scans in total with " + segments.size() + " segments.";
    }

    public void setCorrelationToOtherPeak(ChromatographicPeak other, double correlation, int start, int end) {
        correlatedChromatographicPeak = other;
        this.correlation = correlation;
        this.correlationStartPoint = start;
        this.correlationEndPoint = end;
    }

    @Override
    public ChromatographicPeak getCorrelatedPeak() {
        return correlatedChromatographicPeak;
    }

    @Override
    public double getCorrelation() {
        return correlation;
    }

    @Override
    public int getCorrelationStartPoint() {
        return correlationStartPoint;
    }

    @Override
    public int getCorrelationEndPoint() {
        return correlationEndPoint;
    }

    @Override
    public MutableChromatographicPeak mutate() {
        return this;
    }

    public void trimEdges() {
        extendFirstSegmentToLeft();
        extendLastSegmentToRight();

        int startIndex = segments.firstEntry().getValue().startIndex;
        int endIndex = segments.lastEntry().getValue().endIndex;

        // artificially add noise segments to start and end, such that all scanpoints are covered by a segment
        if (startIndex > 0) {
            addNoiseSegment(0, startIndex-1 );
        }
        if (endIndex < this.numberOfScans()-1) {
            addNoiseSegment(endIndex+1, this.numberOfScans()-1 );
        }
        /*
        final int shift = startIndex;
        final ArrayList<ScanPoint> copy = new ArrayList<>(scanPoints.subList(startIndex,endIndex+1));
        scanPoints.clear();
        scanPoints.addAll(copy);
        if (shift > 0) {
            final ArrayList<Segment> segs = new ArrayList<>(segments.values());
            segments.clear();
            for (Segment s : segs) {
                final Segment t = new Segment(s.peak, s.startIndex - shift, s.apex - shift, s.endIndex - shift, s.noise);
                segments.put(t.apex, t);
            }
        }
        validate();
         */
    }

    private void addNoiseSegment(int from, int to) {
        // find apex
        int mxi=from;
        for (int i=from+1; i <= to; ++i) {
            if (getIntensityAt(i)>getIntensityAt(mxi)) {
                mxi=i;
            }
        }
        segments.put(mxi, new Segment(this, from, mxi, to, true));
    }

    private void extendFirstSegmentToLeft() {
        Segment seg = segments.firstEntry().getValue();
        int k= seg.startIndex;;
        int mindex=k;
        for (; k >= 0; --k) {
            if (getIntensityAt(k) < getIntensityAt(mindex)) {
                mindex=k;
            }
        }
        if (mindex!=seg.startIndex) {
            Segment newSeg = new Segment(this, mindex, seg.apexIndex, seg.endIndex, seg.noise);
            segments.remove(seg.apexIndex);
            segments.put(newSeg.apexIndex, newSeg);
        }
    }
    private void extendLastSegmentToRight() {
        Segment seg = segments.lastEntry().getValue();
        int k= seg.endIndex;;
        double minIntensity = getIntensityAt(k);
        int mindex=k;
        for (; k < numberOfScans(); ++k) {
            if (getIntensityAt(k) < getIntensityAt(mindex)) {
                mindex=k;
            }
        }
        if (mindex!=seg.endIndex) {
            Segment newSeg = new Segment(this, seg.startIndex, seg.apexIndex, mindex, seg.noise);
            segments.remove(seg.apexIndex);
            segments.put(newSeg.apexIndex, newSeg);
        }
    }

    public Optional<Segment> joinAllSegmentsWithinScanIds(int a, int b) {
        if (a>b){
            int z = a;
            a = b;
            b = z;
        }
        final TIntArrayList segmentsToDelete = new TIntArrayList(Math.min(3,segments.size()));
        int minA=Integer.MAX_VALUE, maxB=0,maxApex=-1;
        double apexInt = 0f;
        for (Segment s : segments.values()) {
            if ((a <= s.getEndScanNumber() && b >= s.getEndScanNumber())) {
                segmentsToDelete.add(s.apexIndex);
                minA = Math.min(minA, s.getStartIndex());
                maxB = Math.max(maxB, s.getEndIndex());
                if (s.getApexIntensity()>apexInt) {
                    maxApex = s.apexIndex;
                    apexInt = s.getApexIntensity();
                }
            }
        }
        if (segmentsToDelete.isEmpty()) return Optional.empty();
        segmentsToDelete.forEach(x->{segments.remove(x); return true;});
        final Segment s = new Segment(this, minA, maxApex, maxB, false);
        segments.put(s.apexIndex, s);
        validate();
        return Optional.of(s);
    }

    public void validate() {
        /*
        for (Segment s : segments.values()) {
            for (Segment t : segments.values()) {
                if (s!=t && s.startIndex < t.endIndex && s.endIndex > t.startIndex) {
                    throw new IllegalArgumentException("Segments are not allowed to overlap: " + s + " overlaps with " + t );
                }
            }
        }
         */
    }

}
