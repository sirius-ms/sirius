package de.unijena.bioinf.sirius.deisotope;

import de.unijena.bioinf.ChemistryBase.ms.MergedMs1Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.ft.Ms1IsotopePattern;
import de.unijena.bioinf.ms.annotations.Provides;
import de.unijena.bioinf.ms.annotations.Requires;
import de.unijena.bioinf.sirius.ProcessedInput;

@Requires(MergedMs1Spectrum.class)
@Provides(Ms1IsotopePattern.class)
public interface IsotopePatternDetection {

    public void detectIsotopePattern(ProcessedInput processedInput);

}
