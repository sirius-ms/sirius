package de.unijena.bioinf.ms.persistence.model.core.run;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.utils.SimpleSerializers;
import lombok.Getter;

@Getter
public class SampleStatistics {

    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = SimpleSerializers.DeviationDeserializer.class)
    private Deviation ms1MassDeviationsWithinSamples;

    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = SimpleSerializers.DeviationDeserializer.class)
    private Deviation ms1MassDeviationsBetweenSamples;

    private double retentionTimeDeviationsInSeconds, medianPeakWidthInSeconds;

    private double ms2NoiseLevel;

    private int medianNumberOfAlignments;

    private double medianHeightDividedByPeakWidth;

    protected SampleStatistics() {
    }

    public SampleStatistics(Deviation ms1MassDeviationsWithinSamples, Deviation ms1MassDeviationsBetweenSamples, double retentionTimeDeviationsInSeconds, double medianPeakWidthInSeconds, double medianHeightDividedByPeakWidth, int medianNumberOfAlignments, double ms2NoiseLevel) {
        this.ms1MassDeviationsWithinSamples = ms1MassDeviationsWithinSamples;
        this.ms1MassDeviationsBetweenSamples = ms1MassDeviationsBetweenSamples;
        this.retentionTimeDeviationsInSeconds = retentionTimeDeviationsInSeconds;
        this.medianHeightDividedByPeakWidth = medianHeightDividedByPeakWidth;
        this.ms2NoiseLevel = ms2NoiseLevel;
        this.medianPeakWidthInSeconds =medianPeakWidthInSeconds;
        this.medianNumberOfAlignments = medianNumberOfAlignments;
    }
}
