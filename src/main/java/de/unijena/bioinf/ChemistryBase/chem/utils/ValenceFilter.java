package de.unijena.bioinf.ChemistryBase.chem.utils;

import de.unijena.bioinf.ChemistryBase.algorithm.HasParameters;
import de.unijena.bioinf.ChemistryBase.algorithm.Parameter;
import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.FormulaFilter;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;

/**
 * A formula passes this filter, if its RDBE value is greater or equal to the given limit
 */
@HasParameters
public class ValenceFilter implements FormulaFilter {

    private final int minValenceInt;
    private final double minValence;

    public ValenceFilter() {
        this(-0.5d);
    }

    public ValenceFilter(@Parameter("minValence") double minValence) {
        this.minValenceInt = (int)(2*minValence);
        this.minValence = minValence;

    }

    @Override
    public boolean isValid(MolecularFormula formula) {
        return formula.doubledRDBE() >= minValenceInt;
    }

    public double getMinValence() {
        return minValence;
    }
}
