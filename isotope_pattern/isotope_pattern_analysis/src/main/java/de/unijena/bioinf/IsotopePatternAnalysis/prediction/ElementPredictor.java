package de.unijena.bioinf.IsotopePatternAnalysis.prediction;

import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;

/**
 * Predicts presence of chemical elements from isotope patterns
 */
public interface ElementPredictor {

    public FormulaConstraints predictConstraints(SimpleSpectrum pickedPattern);

}
