package de.unijena.bioinf.lcms.chromatogram;

import com.google.common.collect.Range;
import de.unijena.bioinf.ChemistryBase.algorithm.BinarySearch;
import de.unijena.bioinf.model.lcms.ScanPoint;

import java.util.Collections;
import java.util.List;

public final class MassTrace {

    private final static MassTrace EMPTY = new MassTrace(Collections.emptyList());

    protected Range<Double> mzRange;
    protected Range<Long> rtRange;
    protected List<Segment> segments;

    public static MassTrace empty() {
        return EMPTY;
    }

    protected List<ScanPoint> trace;

    MassTrace(List<ScanPoint> trace) {
        this.trace = trace;
        calcRanges();
        calcSegments();
    }

    private void calcSegments() {
        // first find all maxima

        // 
    }

    private void calcRanges() {
        if (trace.isEmpty()) {
            mzRange = Range.closed(0d,0d);
            rtRange = Range.closed(0L,0L);
        } else {
            double min=Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
            long minR = Long.MAX_VALUE, maxR = Long.MIN_VALUE;
            for (int k=0; k < trace.size(); ++k) {
                final ScanPoint p = trace.get(k);
                minR = Math.min(minR, p.getRetentionTime());
                maxR = Math.max(maxR, p.getRetentionTime());
                min = Math.min(min, p.getMass());
                max = Math.max(max, p.getMass());
            }
            mzRange = Range.closed(min,max);
            rtRange = Range.closed(minR,maxR);
        }
    }

    public int findScanNumber(int scanNumber) {
        return BinarySearch.searchForInt(trace, ScanPoint::getScanNumber, scanNumber);
    }

    public ScanPoint getScanPointAtIndex(int index) {
        return trace.get(index);
    }
    public double getMzAtIndex(int index) {
        return trace.get(index).getMass();
    }
    public long getRetentionTimeAtIndex(int index) {
        return trace.get(index).getRetentionTime();
    }
    public double getIntensityAtIndex(int index) {
        return trace.get(index).getIntensity();
    }

    public Range<Double> getMzRange() {
        return mzRange;
    }

    public Range<Long> getRtRange() {
        return rtRange;
    }

    public boolean isEmpty() {
        return trace.isEmpty();
    }

    public int size() {
        return trace.size();
    }

    public static class Segment {

        protected final int apex;
        protected final int start,end;

        public Segment(int apex, int start, int end) {
            this.apex = apex;
            this.start = start;
            this.end = end;
        }
    }
}
