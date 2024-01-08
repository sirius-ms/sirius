package de.unijena.bioinf.lcms.align2;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import lombok.Data;

@Data
public class AlignmentStatistics {

    protected Deviation expectedMassDeviationBetweenSamples;
    protected double expectedRetentionTimeDeviation;
    protected double minRt, maxRt, minMz, maxMz;
    protected int maxMappingLen;

}
