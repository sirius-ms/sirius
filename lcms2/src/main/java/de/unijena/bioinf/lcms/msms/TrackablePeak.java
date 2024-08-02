package de.unijena.bioinf.lcms.msms;

import de.unijena.bioinf.ChemistryBase.ms.Peak;
import lombok.Value;

@Value
public class TrackablePeak implements Peak {

    private final double mass;
    private final float intensity;
    private final int sampleid, scanId, peakIdx;
    private final float weight;


    @Override
    public double getMass() {
        return mass;
    }

    @Override
    public double getIntensity() {
        return intensity;
    }

    public double getWeightedIntensity() {
        return (double)intensity*(double)weight;
    }
}
