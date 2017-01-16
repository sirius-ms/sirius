package de.unijena.bioinf.IsotopePatternAnalysis.prediction;

import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;

/**
 * Predicts presence of chemical elements from isotope patterns
 */
public interface ElementPredictor {

    public FormulaConstraints predictConstraints(SimpleSpectrum pickedPattern);

    public ChemicalAlphabet getChemicalAlphabet();

    public boolean isPredictable(Element element);

}
