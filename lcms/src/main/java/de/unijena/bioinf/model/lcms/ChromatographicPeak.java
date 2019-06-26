package de.unijena.bioinf.model.lcms;

import com.google.common.collect.Range;

import java.util.NavigableSet;
import java.util.Optional;

public interface ChromatographicPeak {

    public int numberOfScans();

    public double getMzAt(int k);
    public double getIntensityAt(int k);
    public long getRetentionTimeAt(int k);
    public int getScanNumberAt(int k);
    public Range<Long> getRetentionTime();
    public NavigableSet<Segment> getSegments();
    public ScanPoint getScanPointAt(int k);

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

        public double getIntensity() {
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
            if (r.lowerEndpoint().equals(r.upperEndpoint()))
                return (peak.getRetentionTimeAt(Math.min(endIndex, r.upperEndpoint()+1) - Math.max(startIndex,r.lowerEndpoint()-1)));
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
    }

}
