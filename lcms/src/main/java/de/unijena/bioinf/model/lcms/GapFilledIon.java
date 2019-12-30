package de.unijena.bioinf.model.lcms;

import de.unijena.bioinf.lcms.quality.Quality;

public class GapFilledIon extends FragmentedIon {

    FragmentedIon original;

    public GapFilledIon(Polarity polarity, ChromatographicPeak chromatographicPeak, ChromatographicPeak.Segment segment, FragmentedIon original) {
        super(polarity, null,null, Quality.UNUSABLE, chromatographicPeak, segment);
        this.original = original;
    }
}
