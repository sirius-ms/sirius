package de.unijena.bioinf.lcms.trace.segmentation;

import de.unijena.bioinf.lcms.trace.Trace;

public interface ApexDetection {

    public int[] detectMaxima(Trace trace);

}
