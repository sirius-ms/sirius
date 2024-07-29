package de.unijena.bioinf.lcms.msms;

import de.unijena.bioinf.lcms.spectrum.Ms2SpectrumHeader;
import de.unijena.bioinf.lcms.trace.ProcessedSample;

public interface Ms2TraceStrategy {

    public int getTraceFor(ProcessedSample sample, Ms2SpectrumHeader ms2);

}
