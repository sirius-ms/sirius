package de.unijena.bioinf.IsotopePatternAnalysis.extraction;

import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePattern;

import java.util.Collections;
import java.util.List;

public class AlreadyExtracted implements PatternExtractor {
    @Override
    public List<IsotopePattern> extractPattern(Spectrum<Peak> spectrum) {
        return Collections.singletonList(new IsotopePattern(spectrum));
    }
}
