package de.unijena.bioinf.lcms.trace.segmentation;

import de.unijena.bioinf.ms.persistence.model.core.run.SampleStats;
import de.unijena.bioinf.lcms.trace.Trace;

public interface ApexDetection {

    public int[] detectMaxima(SampleStats stats, Trace trace);

}
