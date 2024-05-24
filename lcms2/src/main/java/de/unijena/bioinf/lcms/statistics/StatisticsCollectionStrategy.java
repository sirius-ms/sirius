package de.unijena.bioinf.lcms.statistics;

import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.lcms.spectrum.Ms1SpectrumHeader;
import de.unijena.bioinf.lcms.spectrum.Ms2SpectrumHeader;

/**
 * Step 1 of LC/MS Processing: collecting statistics about noise peak distribution
 */
public interface StatisticsCollectionStrategy {

    public interface Calculation {
        public void processMs1(Ms1SpectrumHeader header, SimpleSpectrum ms1Spectrum);

        public void processMs2(Ms2SpectrumHeader header, SimpleSpectrum ms2Spectrum);

        public SampleStats done();
    }

    public Calculation collectStatistics();

}
