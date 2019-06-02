package de.unijena.bioinf.model.lcms;

import java.util.ArrayList;
import java.util.List;

public class FragmentedIon extends IonGroup {

    protected int chargeState;
    protected final MergedSpectrum msms;
    protected final ChromatographicPeak.Segment segment;

    protected final List<CorrelatedIon> adducts, inSourceFragments;

    public FragmentedIon(MergedSpectrum msms, ChromatographicPeak chromatographicPeak, ChromatographicPeak.Segment segment) {
        super(chromatographicPeak, new ArrayList<>());
        this.msms = msms;
        this.segment = segment;
        this.adducts = new ArrayList<>();
        this.inSourceFragments = new ArrayList<>();
    }

    public List<CorrelatedIon> getAdducts() {
        return adducts;
    }

    public List<CorrelatedIon> getInSourceFragments() {
        return inSourceFragments;
    }

    public void addIsotopes(List<CorrelationGroup> correlatedPeaks) {
        this.isotopes.addAll(correlatedPeaks);
    }

    public int getChargeState() {
        return chargeState;
    }

    public void setChargeState(int chargeState) {
        this.chargeState = chargeState;
    }

    public MergedSpectrum getMsMs() {
        return msms;
    }

    public ChromatographicPeak.Segment getSegment() {
        return segment;
    }

    public String toString() {
        return "MS/MS("+chargeState+") m/z = " + msms.getPrecursor().getMass() + ", apex = " + peak.getRetentionTimeAt(segment.getApexIndex())/60000d + " min";
    }
}
