package de.unijena.bioinf.model.lcms;

import com.google.common.collect.Range;

import java.util.HashSet;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;

public interface ChromatographicPeak {

    public int numberOfScans();

    public double getMzAt(int k);
    public double getIntensityAt(int k);
    public long getRetentionTimeAt(int k);
    public int getScanNumberAt(int k);
    public Range<Long> getRetentionTime();
    public NavigableSet<Segment> getSegments();
    public ScanPoint getScanPointAt(int k);

    default public boolean samePeak(ChromatographicPeak other) {
        if (this==other) return true;
        if (!getRetentionTime().isConnected(other.getRetentionTime())) return false;
        ScanPoint u = getApexPeak();
        ScanPoint v = other.getApexPeak();
        return (u.getScanNumber()==v.getScanNumber() && Math.abs(u.getMass()-v.getMass())<0.001 && Math.abs(u.getIntensity()-v.getIntensity())< (u.getIntensity()*0.001) );
    }

    default public ScanPoint getApexPeak() {
        ScanPoint apex = null;
        for (Segment s : getSegments()) {
            ScanPoint p = getScanPointAt(s.apex);
            if (apex==null || p.getIntensity()>apex.getIntensity()) {
                apex = p;
            }
        }
        return apex;
    }

    public ScanPoint getScanPointForScanId(int scanId);
    public default ScanPoint getRightEdge() {
        return getScanPointAt(numberOfScans()-1);
    }
    public default ScanPoint getLeftEdge() {
        return getScanPointAt(0);
    }

    /**
     * searches for two minima and a maximum such that the given scanNumber is between the minima and closeby the maximum
     * @return index of the maximum
     */
    public default Optional<Segment> getSegmentForScanId(int scanId) {
        for (Segment s : getSegments()) {
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

    default Set<Integer> scanNumbers() {
        final HashSet<Integer> set = new HashSet<>();
        for (int k=0; k <  numberOfScans(); ++k) {
            set.add(getScanNumberAt(k));
        }
        return set;
    }

    public static class Segment {

        protected final ChromatographicPeak peak;
        protected final int startIndex, endIndex, apex;
        protected final int fwhmStart, fwhmEnd;

        Segment(ChromatographicPeak peak, int startIndex, int apex, int endIndex) {
            this.peak = peak;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.apex = apex;
            final Range<Integer> fwhm = calculateFWHM(0.5);
            this.fwhmEnd = fwhm.upperEndpoint();
            this.fwhmStart = fwhm.lowerEndpoint();
        }

        void setMinMaxScanIndex(int[] scanIndex, int surrounding) {
            int k=Math.max(0,startIndex-surrounding);
            scanIndex[0] = Math.min(scanIndex[0],peak.getScanNumberAt(k));
            k=Math.min(peak.numberOfScans()-1,endIndex+surrounding);
            scanIndex[1] = Math.max(scanIndex[1],peak.getScanNumberAt(k));
        }

        public double getApexIntensity() {
            return peak.getIntensityAt(apex);
        }

        public ChromatographicPeak getPeak() {
            return peak;
        }

        public String toString() {
            return "Segment(" + peak.getScanNumberAt(startIndex) + " ... " + peak.getScanNumberAt(endIndex) + "), " + (endIndex-startIndex+1) + " spans from " + (retentiomTimeSpan().lowerEndpoint()/60000d) + " .. " + (retentiomTimeSpan().upperEndpoint()/60000d)  +  " min over " + retentionTimeWidth()/1000d + " seconds.";
        }

        public long fwhm() {
            return peak.getRetentionTimeAt(fwhmEnd)-peak.getRetentionTimeAt(fwhmStart);
        }
        public long fwhm(double percentile) {
            Range<Integer> r = calculateFWHM(percentile);
            if (r.lowerEndpoint().equals(r.upperEndpoint())) {
                int a = Math.min(endIndex, r.upperEndpoint() + 1);
                int b = Math.max(startIndex, r.lowerEndpoint() - 1);
                return Math.min(peak.getRetentionTimeAt(r.upperEndpoint())-peak.getRetentionTimeAt(a),
                        peak.getRetentionTimeAt(b)-peak.getRetentionTimeAt(r.lowerEndpoint()));
            }
            return peak.getRetentionTimeAt(r.upperEndpoint())-peak.getRetentionTimeAt(r.lowerEndpoint());
        }

        public int getFwhmStartIndex() {
            return fwhmStart;
        }

        public int getFwhmEndIndex() {
            return fwhmEnd;
        }

        public Range<Integer> calculateFWHM(double threshold) {
            double intApex = peak.getIntensityAt(apex);
            double halveMaximum = intApex*threshold;
            int i,j;
            for (i=apex; i >= startIndex; --i) {
                if (peak.getIntensityAt(i) < halveMaximum)
                    break;
            }
            ++i;
            for (j=apex; j <= endIndex; ++j) {
                if (peak.getIntensityAt(j) < halveMaximum)
                    break;
            }
            --j;
            if (i>j) return Range.closed(apex,apex);
            return Range.closed(i,j);
        }

        public int getStartScanNumber() {
            return peak.getScanNumberAt(startIndex);
        }

        public int getEndScanNumber() {
            return peak.getScanNumberAt(endIndex);
        }

        public int getApexScanNumber() {
            return peak.getScanNumberAt(apex);
        }

        public int getStartIndex() {
            return startIndex;
        }

        public int getEndIndex() {
            return endIndex;
        }

        public int getApexIndex() {
            return apex;
        }

        public long retentionTimeWidth() {
            return peak.getRetentionTimeAt(endIndex)-peak.getRetentionTimeAt(startIndex);
        }

        public Range<Long> retentiomTimeSpan() {
            return Range.closed(peak.getRetentionTimeAt(startIndex), peak.getRetentionTimeAt(endIndex));
        }

        public boolean samePeak(Segment other) {
            return peak.samePeak(other.peak) && getApexScanNumber()==other.getApexScanNumber();
        }

        public long getApexRt() {
            return peak.getRetentionTimeAt(apex);
        }

        public double getApexMass() {
            return peak.getMzAt(apex);
        }
    }

}
