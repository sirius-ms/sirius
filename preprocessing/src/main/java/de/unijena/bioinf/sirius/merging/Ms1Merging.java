package de.unijena.bioinf.sirius.merging;

import de.unijena.bioinf.ChemistryBase.ms.MergedMs1Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.ms.annotations.Provides;
import de.unijena.bioinf.sirius.ProcessedInput;

@Provides(MergedMs1Spectrum.class)
public class Ms1Merging {


    public MergedMs1Spectrum getMergedSpectrum(ProcessedInput processedInput) {
        final Ms2Experiment exp = processedInput.getExperimentInformation();
        if (exp.getMergedMs1Spectrum()!=null) {
            return new MergedMs1Spectrum(true, exp.getMergedMs1Spectrum());
        } else if (!exp.getMs1Spectra().isEmpty()){
            final SimpleSpectrum merged = Spectrums.mergeSpectra(exp.getMs1Spectra());
            return new MergedMs1Spectrum(false, merged);
        } else {
            return MergedMs1Spectrum.empty();
        }
    }

    public void merge(ProcessedInput processedInput) {
        processedInput.setAnnotation(MergedMs1Spectrum.class, getMergedSpectrum(processedInput));
    }

}
