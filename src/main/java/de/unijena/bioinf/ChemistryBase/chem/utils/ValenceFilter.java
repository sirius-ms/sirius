package de.unijena.bioinf.ChemistryBase.chem.utils;

import de.unijena.bioinf.ChemistryBase.chem.FormulaFilter;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;

/**
 * Created with IntelliJ IDEA.
 * User: kai
 * Date: 4/22/13
 * Time: 3:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class ValenceFilter implements FormulaFilter{

    private final int minValence;

    public ValenceFilter() {
        this(-0.5d);
    }

    public ValenceFilter(double minValence) {
        this.minValence = (int)(2*minValence);
    }

    @Override
    public boolean isValid(MolecularFormula formula) {
        return formula.doubledRDBE() >= minValence;
    }
}
