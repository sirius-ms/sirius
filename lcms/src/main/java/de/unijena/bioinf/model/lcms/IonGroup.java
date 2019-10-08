package de.unijena.bioinf.model.lcms;

import de.unijena.bioinf.lcms.quality.Quality;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class IonGroup {

    protected ChromatographicPeak peak;
    protected List<CorrelationGroup> isotopes;
    protected IonAnnotation ionAnnotation;
    protected int chargeState;
    protected final ChromatographicPeak.Segment segment;

    public IonGroup(ChromatographicPeak peak, ChromatographicPeak.Segment segment, List<CorrelationGroup> isotopes) {
        this.peak = peak;
        this.isotopes = isotopes;
        this.segment = segment;
    }

    public boolean chargeStateIsNotDifferent(IonGroup other) {
        return chargeState == 0 || other.chargeState==0 || other.chargeState==chargeState;
    }

    public ChromatographicPeak getPeak() {
        return peak;
    }

    public List<CorrelationGroup> getIsotopes() {
        return isotopes;
    }

    public void addIsotopes(List<CorrelationGroup> correlatedPeaks) {
        this.isotopes.addAll(correlatedPeaks);
    }


    public IonAnnotation getIonAnnotation() {
        return ionAnnotation;
    }

    public void setIonAnnotation(IonAnnotation ionAnnotation) {
        this.ionAnnotation = ionAnnotation;
    }


    public int getChargeState() {
        return chargeState;
    }

    public void setChargeState(int chargeState) {
        this.chargeState = chargeState;
    }

    public ChromatographicPeak.Segment getSegment() {
        return segment;
    }

    public String toString() {
        if (peak.getSegments().isEmpty()) return "";
        int apex = peak.getSegments().first().apex;
        String iso = isotopes.isEmpty() ? "" : (isotopes.size() + " isotopes with correlation " + Arrays.toString(isotopes.stream().mapToDouble(x->x.getCorrelation()).toArray()));
        return String.format(Locale.US, "ScanID: %d .. %d. m/z = %.5f, intensity = %.1f. %s", peak.getScanNumberAt(0), peak.getScanNumberAt(peak.numberOfScans()-1), peak.getMzAt(apex), peak.getIntensityAt(apex), iso);
    }

    public Quality getMsQuality() {
        if (isotopes.size()>=3) return Quality.GOOD;
        if (isotopes.size()>=2) return Quality.DECENT;
        return Quality.BAD;
    }

    public double getMass() {
        return peak.getMzAt(getSegment().apex);
    }
}
