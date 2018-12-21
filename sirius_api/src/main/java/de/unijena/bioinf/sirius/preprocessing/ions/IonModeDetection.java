package de.unijena.bioinf.sirius.preprocessing.ions;

import de.unijena.bioinf.ChemistryBase.chem.IonMode;
import de.unijena.bioinf.ChemistryBase.ms.PossibleIonModes;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;

import java.util.Set;

/**
 * Detects the ion mode given the MS spectrum
 */
public interface IonModeDetection {

    PossibleIonModes detect(ProcessedInput processedInput, Set<IonMode> candidates);

}
