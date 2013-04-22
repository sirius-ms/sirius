package de.unijena.bioinf.ChemistryBase.chem;

/**
 * A strict filter which decides if a formula is valid
 */
public interface FormulaFilter {

    public boolean isValid(MolecularFormula formula);

}
