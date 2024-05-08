package de.unijena.bioinf.lcms.align;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import lombok.Data;

import java.io.Serializable;

@Data
public class AlignmentStatistics implements Serializable {

    protected Deviation expectedMassDeviationBetweenSamples;
    protected double expectedRetentionTimeDeviation;
    protected double minRt, maxRt, minMz, maxMz;
    protected IntArrayList mappingLengths;
    protected FloatArrayList stepSizes;
    protected float averageNumberOfAlignments, medianNumberOfAlignments;

}
