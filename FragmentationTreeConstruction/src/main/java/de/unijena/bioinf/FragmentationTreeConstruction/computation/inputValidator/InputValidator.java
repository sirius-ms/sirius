package de.unijena.bioinf.FragmentationTreeConstruction.computation.inputValidator;

import de.unijena.bioinf.FragmentationTreeConstruction.model.MSInput;

public interface InputValidator {

    public MSInput validate(MSInput input, boolean repair) throws InvalidException;

}
