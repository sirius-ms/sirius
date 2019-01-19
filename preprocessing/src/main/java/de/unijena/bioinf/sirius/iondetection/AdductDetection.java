package de.unijena.bioinf.sirius.iondetection;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.PossibleAdducts;
import de.unijena.bioinf.sirius.ProcessedInput;

import java.util.Set;

/**
 * Detects the ion mode given the MS spectrum
 */
public interface AdductDetection {

    PossibleAdducts detect(ProcessedInput processedInput, Set<PrecursorIonType> candidates);

}
