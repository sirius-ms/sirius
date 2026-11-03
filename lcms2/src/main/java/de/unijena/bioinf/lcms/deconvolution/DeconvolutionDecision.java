package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.lcms.trace.ProcessedSample;

public interface DeconvolutionDecision {

    public boolean performDeconvolution(ProcessedSample sample);

}
