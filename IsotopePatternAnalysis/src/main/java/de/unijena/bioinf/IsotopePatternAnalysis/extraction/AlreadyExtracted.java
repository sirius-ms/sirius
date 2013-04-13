package de.unijena.bioinf.IsotopePatternAnalysis.extraction;

import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;

import java.util.Collections;
import java.util.List;

public class AlreadyExtracted implements PatternExtractor {
    @Override
    public List<Spectrum<Peak>> extractPattern(Spectrum<Peak> spectrum) {
        return Collections.singletonList(spectrum);
    }
}
