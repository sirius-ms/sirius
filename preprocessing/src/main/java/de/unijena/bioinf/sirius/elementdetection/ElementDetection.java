package de.unijena.bioinf.sirius.elementdetection;

import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.ms.ft.Ms1IsotopePattern;
import de.unijena.bioinf.ms.annotations.Provides;
import de.unijena.bioinf.ms.annotations.Requires;
import de.unijena.bioinf.sirius.ProcessedInput;

import java.util.Set;

@Requires(Ms1IsotopePattern.class)
@Provides(FormulaConstraints.class)
public interface ElementDetection {

    /**
     * detect chemical elements from isotope pattern.
     * @return null, if no isotope pattern available, otherwise the formula constraints for the detected elements
     */
    public FormulaConstraints detect(ProcessedInput processedInput);

    public Set<Element> getPredictableElements();

    public default boolean isPredictable(Element e) {
        return getPredictableElements().contains(e);
    }

}
