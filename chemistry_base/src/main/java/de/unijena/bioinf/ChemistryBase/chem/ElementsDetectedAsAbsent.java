package de.unijena.bioinf.ChemistryBase.chem;

import de.unijena.bioinf.ChemistryBase.chem.utils.FormulaVisitor;
import de.unijena.bioinf.ms.annotations.ProcessedInputAnnotation;

import java.util.HashSet;
import java.util.Set;

/**
 * This annotation works similar to FormulaConstraints, but while formula constraints is inclusive (everything not
 * specified in FormulaConstraints in forbidden) this annotation is exclusive (only things in this set are forbidden)
 */
public class ElementsDetectedAsAbsent implements ProcessedInputAnnotation {
    private final static ElementsDetectedAsAbsent EMPTY = new ElementsDetectedAsAbsent(new HashSet());
    public static ElementsDetectedAsAbsent empty() {
        return EMPTY;
    }

    private final Set<Element> absentElements;

    public ElementsDetectedAsAbsent(Set<Element> absentElements) {
        this.absentElements = absentElements;
    }

    public boolean isSatisfied(MolecularFormula formula) {
        return !isViolated(formula);
    }

    public boolean isViolated(MolecularFormula formula) {
        for (Element e : formula) {
            if (isElementForbidden(e)) return true;
        }
        return false;
    }

    public boolean isElementForbidden(Element e) {
        return absentElements.contains(e);
    }

    public boolean isElementAllowed(Element e) {
        return !isElementForbidden(e);
    }
}
