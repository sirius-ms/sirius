package de.unijena.bioinf.model.lcms;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class IonGroup {

    protected ChromatographicPeak peak;
    protected List<CorrelationGroup> isotopes;
    protected IonAnnotation ionAnnotation;

    public IonGroup(ChromatographicPeak peak, List<CorrelationGroup> isotopes) {
        this.peak = peak;
        this.isotopes = isotopes;
    }

    public ChromatographicPeak getPeak() {
        return peak;
    }

    public List<CorrelationGroup> getIsotopes() {
        return isotopes;
    }

    public IonAnnotation getIonAnnotation() {
        return ionAnnotation;
    }

    public void setIonAnnotation(IonAnnotation ionAnnotation) {
        this.ionAnnotation = ionAnnotation;
    }

    public String toString() {
        if (peak.getSegments().isEmpty()) return "";
        int apex = peak.getSegments().first().apex;
        String iso = isotopes.isEmpty() ? "" : (isotopes.size() + " isotopes with correlation " + Arrays.toString(isotopes.stream().mapToDouble(x->x.getCorrelation()).toArray()));
        return String.format(Locale.US, "ScanID: %d .. %d. m/z = %.5f, intensity = %.1f. %s", peak.getScanNumberAt(0), peak.getScanNumberAt(peak.numberOfScans()-1), peak.getMzAt(apex), peak.getIntensityAt(apex), iso);
    }
}
