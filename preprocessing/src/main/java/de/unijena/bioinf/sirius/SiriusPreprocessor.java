package de.unijena.bioinf.sirius;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;

/**
 * Prepares an experiment for MS and MS/MS analysis
 */
public interface SiriusPreprocessor {

    public ProcessedInput preprocess(Ms2Experiment experiment);


}
