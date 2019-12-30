package de.unijena.bioinf.sirius.peakprocessor;

import de.unijena.bioinf.ms.annotations.Requires;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;
import de.unijena.bioinf.sirius.annotations.DecompositionList;

/**
 * Is applied on spectra which are already decomposed
 */
@Requires(value=DecompositionList.class, in=ProcessedPeak.class)
public interface DecomposedSpectrumProcessor {

    public void process(ProcessedInput input);
}
