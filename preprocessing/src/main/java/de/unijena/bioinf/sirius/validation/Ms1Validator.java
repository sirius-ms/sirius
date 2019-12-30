package de.unijena.bioinf.sirius.validation;

import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.inputValidators.InvalidException;
import de.unijena.bioinf.ChemistryBase.ms.inputValidators.Ms2ExperimentValidator;
import de.unijena.bioinf.ChemistryBase.ms.inputValidators.Warning;

public class Ms1Validator implements Ms2ExperimentValidator {

    @Override
    public boolean validate(MutableMs2Experiment input, Warning warning, boolean repair) throws InvalidException {
        if (input.getMs1Spectra().size()==0 && input.getMergedMs1Spectrum()==null && input.getMs2Spectra().size()==0)
            throw new InvalidException("Empty input.");

        return true;

    }
}
