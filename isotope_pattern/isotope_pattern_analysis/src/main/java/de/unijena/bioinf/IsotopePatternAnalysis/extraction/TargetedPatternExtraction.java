package de.unijena.bioinf.IsotopePatternAnalysis.extraction;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;

public interface TargetedPatternExtraction {

    public SimpleSpectrum extractSpectrum(Ms2Experiment experiment);

}
