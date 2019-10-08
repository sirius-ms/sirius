package de.unijena.bioinf.sirius.peakprocessor;

import de.unijena.bioinf.sirius.ProcessedInput;

/**
 * Apply changes on merged spectrum
 */
public interface MergedSpectrumProcessor {

    public void process(ProcessedInput input);

}
