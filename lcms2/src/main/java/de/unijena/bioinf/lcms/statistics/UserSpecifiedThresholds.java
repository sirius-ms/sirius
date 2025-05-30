package de.unijena.bioinf.lcms.statistics;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;

public class UserSpecifiedThresholds extends MedianNoiseCollectionStrategy {
    @Setter @Getter
    private Double ms1NoiseLevel;
    @Setter @Getter
    private Deviation allowedMassDeviationInMs1;

    private double maximalAllowedRetentionTimeDeviation;

    public UserSpecifiedThresholds() {
    }


    @Override
    public Calculation collectStatistics() {
        return new CalculateMedians();
    }

    public boolean hasUserInput() {
        return ms1NoiseLevel!=null || allowedMassDeviationInMs1!=null;
    }

    protected class CalculateMedians extends MedianNoiseCollectionStrategy.CalculateMedians {

        @Override
        public SampleStats done() {
            SampleStats done = super.done();
            if (ms1NoiseLevel!=null) done.setNoiseLevel(ms1NoiseLevel.floatValue());
            if (allowedMassDeviationInMs1!=null) done.setMs1MassDeviationWithinTraces(allowedMassDeviationInMs1);
            return done;
        }
    }
}
