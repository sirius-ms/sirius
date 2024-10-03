package de.unijena.bioinf.lcms.msms;

import de.unijena.bioinf.lcms.spectrum.Ms2SpectrumHeader;
import de.unijena.bioinf.lcms.trace.ProcessedSample;

import java.util.Optional;

public interface Ms2TraceStrategy {

    public Optional<MsMsTraceReference> getTraceFor(ProcessedSample sample, Ms2SpectrumHeader ms2);

}
