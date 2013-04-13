package de.unijena.bioinf.ChemistryBase.chem.utils;

import de.unijena.bioinf.ChemistryBase.chem.Element;

/**
 * @see de.unijena.bioinf.ChemistryBase.chem.PeriodicTable#parse(String, FormulaVisitor)
 */
public interface FormulaVisitor<T> {
	/**
	 * @param element chemical element
	 * @param amount number of atoms with this element
	 * @return
	 */
	public T visit(Element element, int amount);

}
