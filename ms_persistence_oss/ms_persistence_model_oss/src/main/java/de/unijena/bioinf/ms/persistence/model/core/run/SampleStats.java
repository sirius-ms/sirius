package de.unijena.bioinf.ms.persistence.model.core.run;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SampleStats {

    /**
     * Assigns a noise level to each spectrum in the sample
     * The noise level is the intensity where we think all peaks below it are noise
     */
    private float[] noiseLevelPerScan;

    /**
     * Assigns a noise level to all MS/MS in the sample
     */
    @With
    private float ms2NoiseLevel;

    @With
    private Deviation ms1MassDeviationWithinTraces;

    @With
    private Deviation minimumMs1MassDeviationBetweenTraces;

    public float noiseLevel(int idx) {
        return noiseLevelPerScan[idx];
    }

    public float ms2NoiseLevel() {
        return ms2NoiseLevel;
    }

}
