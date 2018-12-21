package de.unijena.bioinf.sirius.preprocessing;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;

/**
 * Prepares an experiment for MS and MS/MS analysis
 */
public interface SiriusPreprocessor {

    public ProcessedInput preprocess(Ms2Experiment experiment);


}
