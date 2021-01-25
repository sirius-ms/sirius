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

import com.google.common.collect.Range;
import gnu.trove.list.array.TIntArrayList;

import java.util.*;

import static java.util.Collections.binarySearch;

public class MutableChromatographicPeak implements CorrelatedChromatographicPeak {

    private final ArrayList<ScanPoint> scanPoints;
    private int apex;
    private ChromatographicPeak correlatedChromatographicPeak;
    private int correlationStartPoint, correlationEndPoint;
    private double correlation;
    public TreeMap<Integer, Segment> segments;

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

    public void addSegment(int from, int apex, int to) {
        Segment newSegment = new Segment(this, from, apex, to);
        // segments are not allowed to overlap
        var ceiling = segments.ceilingEntry(newSegment.apex);
        if (ceiling!=null && ceiling.getValue().startIndex < newSegment.endIndex)
            throw new IllegalArgumentException("Segments are not allowed to overlap.");
        var floor = segments.floorEntry(newSegment.apex);
        if (floor != null && floor.getValue().endIndex > newSegment.startIndex)
            throw new IllegalArgumentException("Segments are not allowed to overlap.");

        this.segments.put(newSegment.apex, newSegment);
    }

    public void divideSegment(Segment segment, int minimum, int maximumLeft, int maximumRight) {
        segments.remove(segment.apex);
        addSegment(segment.startIndex, maximumLeft, minimum);
        addSegment(minimum, maximumRight, segment.endIndex);
    }

    public void joinSegments(Segment left, Segment right) {
        int newApex;
        if (getIntensityAt(left.apex) > getIntensityAt(right.apex))
            newApex = left.apex;
        else newApex = right.apex;
        segments.remove(left.apex);
        segments.remove(right.apex);
        addSegment(Math.min(left.startIndex,right.startIndex),  newApex,Math.max(left.endIndex,right.endIndex));
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
        else return Range.closed(getRetentionTimeAt(0), getRetentionTimeAt(scanPoints.size()-1));
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
        int startIndex = segments.firstEntry().getValue().startIndex;
        int endIndex = segments.lastEntry().getValue().endIndex;

        final int shift = startIndex;
        final ArrayList<ScanPoint> copy = new ArrayList<>(scanPoints.subList(startIndex,endIndex+1));
        scanPoints.clear();
        scanPoints.addAll(copy);
        if (shift > 0) {
            final ArrayList<Segment> segs = new ArrayList<>(segments.values());
            segments.clear();
            for (Segment s : segs) {
                final Segment t = new Segment(s.peak, s.startIndex - shift, s.apex - shift, s.endIndex - shift);
                segments.put(t.apex, t);
            }
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
            if ((a >= s.getStartScanNumber() && a <= s.getEndScanNumber()) || (b >= s.getStartScanNumber() && b <= s.getEndScanNumber()) ) {
                segmentsToDelete.add(s.apex);
                minA = Math.min(minA, s.getStartIndex());
                maxB = Math.max(maxB, s.getEndIndex());
                if (s.getApexIntensity()>apexInt) {
                    maxApex = s.apex;
                    apexInt = s.getApexIntensity();
                }
            }
        }
        if (segmentsToDelete.isEmpty()) return Optional.empty();
        segmentsToDelete.forEach(x->{segments.remove(x); return true;});
        final Segment s = new Segment(this, minA, maxApex, maxB);
        segments.put(s.apex, s);
        return Optional.of(s);
    }

}
