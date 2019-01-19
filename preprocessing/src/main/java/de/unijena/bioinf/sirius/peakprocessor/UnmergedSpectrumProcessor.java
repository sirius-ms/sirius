package de.unijena.bioinf.sirius.peakprocessor;

import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;

/**
 * Applies changes on the spectrum before merging
 */
public interface UnmergedSpectrumProcessor {

    public void process(MutableMs2Experiment experiment);

}
