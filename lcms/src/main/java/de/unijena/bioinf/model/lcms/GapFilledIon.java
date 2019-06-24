package de.unijena.bioinf.model.lcms;

public class GapFilledIon extends FragmentedIon {

    FragmentedIon original;

    public GapFilledIon(ChromatographicPeak chromatographicPeak, ChromatographicPeak.Segment segment, FragmentedIon original) {
        super(null, chromatographicPeak, segment);
        this.original = original;
    }
}
