package de.unijena.bioinf.IsotopePatternAnalysis.prediction;

import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;

/**
 * Predicts presence of chemical elements from isotope patterns
 */
@Deprecated
public interface ElementPredictor {

    FormulaConstraints predictConstraints(SimpleSpectrum pickedPattern);

    ChemicalAlphabet getChemicalAlphabet();

    boolean isPredictable(Element element);

}
