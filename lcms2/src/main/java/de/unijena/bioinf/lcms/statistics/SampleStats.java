package de.unijena.bioinf.lcms.statistics;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.utils.SimpleSerializers;
import de.unijena.bioinf.ms.persistence.model.core.run.SampleStatistics;
import lombok.*;

import java.util.Arrays;
import java.util.Optional;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SampleStats {

    /**
     * Assigns a noise level to each spectrum in the sample
     * The noise level is the intensity where we think all peaks below it are noise
     */
    @With private float[] noiseLevelPerScan;

    /**
     * Assigns a noise level to all MS/MS in the sample
     */
    @With
    private float ms2NoiseLevel;

    @With
    private float minimumIntensity;

    @With
    @Getter @Setter
    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = SimpleSerializers.DeviationDeserializer.class)
    private Deviation ms1MassDeviationWithinTraces;

    @With
    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = SimpleSerializers.DeviationDeserializer.class)
    private Deviation minimumMs1MassDeviationBetweenTraces;

    public float noiseLevel(int idx) {
        return noiseLevelPerScan[idx];
    }

    public float ms2NoiseLevel() {
        return ms2NoiseLevel;
    }

    private double expectedPeakWidth = -1;

    public Optional<Double> getExpectedPeakWidth() {
        return expectedPeakWidth<0 ? Optional.empty() : Optional.of(expectedPeakWidth);
    }

    public void setExpectedPeakWidth(double expectedPeakWidth) {
        this.expectedPeakWidth = expectedPeakWidth;
    }

    public void setNoiseLevel(double noiseLevel) {
        Arrays.fill(noiseLevelPerScan, (float)noiseLevel);
    }
}
