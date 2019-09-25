package de.unijena.bioinf.sirius.merging;

import de.unijena.bioinf.sirius.ProcessedInput;

public interface Ms2Merger {
    /**
     * Takes an experiment, merge all MS/MS spectra into one.
     * @param processedInput
     */
    public void merge(ProcessedInput processedInput);

}
