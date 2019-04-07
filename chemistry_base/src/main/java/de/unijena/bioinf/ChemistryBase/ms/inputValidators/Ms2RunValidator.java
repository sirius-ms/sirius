package de.unijena.bioinf.ChemistryBase.ms.inputValidators;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Run;

/**
 * Created by ge28quv on 05/07/17.
 */
public interface Ms2RunValidator {

    Ms2Run validate(Ms2Run input, Warning warning, boolean repair) throws InvalidException;

}
