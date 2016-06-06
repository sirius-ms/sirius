package de.unijena.bioinf.ChemistryBase.fp;

import de.unijena.bioinf.ChemistryBase.chem.Element;

public class FormulaProperty extends MolecularProperty {

    private Element element;
    private int minCount;

    public FormulaProperty(Element element, int minCount) {
        this.element = element;
        this.minCount = minCount;
    }

    public String toString() {
        return element.getSymbol() + " >= " + minCount;
    }

    @Override
    public String getDescription() {
        return "contains at least " + minCount + " " + element.getName();
    }
}
