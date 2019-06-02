package de.unijena.bioinf.model.lcms;

import com.google.common.collect.Range;

import java.util.*;

import static java.util.Collections.binarySearch;

public class MutableChromatographicPeak implements CorrelatedChromatographicPeak {

    private final ArrayList<ScanPoint> scanPoints;
    private int apex;
    private ChromatographicPeak correlatedChromatographicPeak;
    private int correlationStartPoint, correlationEndPoint;
    private double correlation;
    public TreeSet<Segment> segments;

    public MutableChromatographicPeak(ChromatographicPeak peak) {
        this.scanPoints = new ArrayList<>();
        for (int i=0; i < peak.numberOfScans(); ++i) scanPoints.add(peak.getScanPointAt(i));
        this.segments = new TreeSet<>(peak.getSegments());
        if (peak instanceof CorrelatedChromatographicPeak) {
            CorrelatedChromatographicPeak cpeak = (CorrelatedChromatographicPeak)peak;
            correlationEndPoint = cpeak.getCorrelationEndPoint();
            correlationStartPoint = cpeak.getCorrelationStartPoint();
            correlation = cpeak.getCorrelation();
        }
    }

    public MutableChromatographicPeak() {
        this.scanPoints = new ArrayList<>();
        this.segments = new TreeSet<>((u,v)->Integer.compare(u.startIndex, v.startIndex));
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
        Segment ceiling = segments.ceiling(newSegment);
        if (ceiling!=null && ceiling.endIndex < newSegment.endIndex)
            throw new IllegalArgumentException("Segments are not allowed to overlap.");
        Segment floor = segments.floor(newSegment);
        if (floor != null && floor.endIndex > newSegment.startIndex)
            throw new IllegalArgumentException("Segments are not allowed to overlap.");

        this.segments.add(newSegment);
    }

    public void divideSegment(Segment segment, int minimum, int maximumLeft, int maximumRight) {
        segments.remove(segment);
        addSegment(segment.startIndex, maximumLeft, minimum);
        addSegment(minimum, maximumRight, segment.endIndex);
    }

    public void joinSegments(Segment left, Segment right) {
        int newApex;
        if (getIntensityAt(left.apex) > getIntensityAt(right.apex))
            newApex = left.apex;
        else newApex = right.apex;
        segments.remove(left);
        segments.remove(right);
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
    public NavigableSet<Segment> getSegments() {
        return segments;
    }

    @Override
    public ScanPoint getScanPointAt(int k) {
        return scanPoints.get(k);
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
        return "m/z = " + getMzAt(segments.first().apex) + ", retention time = " + getLeftEdge().getRetentionTime() + " ... " + getRightEdge().getRetentionTime() + " ms, scans = " + getLeftEdge().getScanNumber() + " ... " + getRightEdge().getScanNumber() + ", " + numberOfScans() + " scans in total with " + segments.size() + " segments.";
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
        int startIndex = segments.first().startIndex;
        int endIndex = segments.last().endIndex;

        final int shift = startIndex;
        final ArrayList<ScanPoint> copy = new ArrayList<>(scanPoints.subList(startIndex,endIndex+1));
        scanPoints.clear();
        scanPoints.addAll(copy);
        if (shift > 0) {
            final ArrayList<Segment> segs = new ArrayList<>(segments);
            segments.clear();
            for (Segment s : segs) {
                segments.add(new Segment(s.peak, s.startIndex-shift,s.apex-shift,s.endIndex-shift));
            }
        }
    }

    public boolean joinAllSegmentsWithinScanIds(int a, int b) {
        final List<Segment> segmentsToDelete = new ArrayList<>();
        for (Segment s : segments) {
            if (s.getStartScanNumber() >= a && s.getEndScanNumber() <= b)
                segmentsToDelete.add(s);
        }
        if (segmentsToDelete.isEmpty()) return false;
        final int minA = segmentsToDelete.stream().mapToInt(s->s.getStartIndex()).min().getAsInt();
        final int maxB = segmentsToDelete.stream().mapToInt(s->s.getEndIndex()).max().getAsInt();
        segments.removeAll(segmentsToDelete);
        final int apex = segmentsToDelete.stream().max(Comparator.comparingDouble(u -> getIntensityAt(u.apex))).get().apex;
        segments.add(new Segment(this, minA, apex, maxB));
        return true;
    }
}
