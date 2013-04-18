package de.unijena.bioinf.FragmentationTreeConstruction.computation.inputValidator;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.FragmentationTreeConstruction.model.MSInput;

/**
 * Checks if input formula is charged. In this case, remove the modification mass from formular
 */
public class ChargedInputFormulaValidator implements InputValidator {


    @Override
    public MSInput validate(MSInput input, boolean repair) throws InvalidException {
        final MSInput newInput = input; // TODO: Implement deep copy
        final MolecularFormula formula = input.getFormula();
        if (formula != null && formula.maybeCharged()) {
            if (repair) {
                input.setFormulaChargedMass(formula.getMass());
            }
        }
        return newInput;
    }
}
