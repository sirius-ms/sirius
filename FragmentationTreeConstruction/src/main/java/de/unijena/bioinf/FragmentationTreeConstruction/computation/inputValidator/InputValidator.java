package de.unijena.bioinf.FragmentationTreeConstruction.computation.inputValidator;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.FragmentationTreeConstruction.model.MSInput;

public interface InputValidator {

    public Ms2Experiment validate(Ms2Experiment input, Warning warning, boolean repair) throws InvalidException;

}
