package de.unijena.bioinf.ChemistryBase.ms.inputValidators;

import de.unijena.bioinf.ChemistryBase.exceptions.InvalidInputData;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Dataset;

/**
 * Created by ge28quv on 05/07/17.
 */
public interface Ms2DatasetValidator {

    Ms2Dataset validate(Ms2Dataset input, Warning warning, boolean repair) throws InvalidException;

}
