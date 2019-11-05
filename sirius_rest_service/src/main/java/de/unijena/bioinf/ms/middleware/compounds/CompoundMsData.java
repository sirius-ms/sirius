package de.unijena.bioinf.ms.middleware.compounds;

import de.unijena.bioinf.ms.middleware.spectrum.AnnotatedSpectrum;

import java.util.List;
import java.util.Optional;

/**
 * The MsData wraps all spectral input data belonging to a compound.
 *
 * Each compound has:
 * - One merged MS/MS spectrum (optional)
 * - One merged MS spectrum (optional)
 * - many MS/MS spectra
 * - many MS spectra
 *
 * Each non-merged spectrum has an index which can be used to access the spectrum.
 *
 * In future we might add some additional information like chromatographic peak or something similar
 */
public class CompoundMsData {

    protected Optional<AnnotatedSpectrum> mergedMs1;
    protected Optional<AnnotatedSpectrum> mergedMs2;
    protected List<AnnotatedSpectrum> ms2Spectra;
    protected List<AnnotatedSpectrum> ms1Spectra;

    public CompoundMsData(Optional<AnnotatedSpectrum> mergedMs1, Optional<AnnotatedSpectrum> mergedMs2, List<AnnotatedSpectrum> ms2Spectra, List<AnnotatedSpectrum> ms1Spectra) {
        this.mergedMs1 = mergedMs1;
        this.mergedMs2 = mergedMs2;
        this.ms2Spectra = ms2Spectra;
        this.ms1Spectra = ms1Spectra;
    }

    public AnnotatedSpectrum getMergedMs1() {
        return mergedMs1.orElse(null);
    }

    public AnnotatedSpectrum getMergedMs2() {
        return mergedMs2.orElse(null);
    }

    public List<AnnotatedSpectrum> getMs2Spectra() {
        return ms2Spectra;
    }

    public List<AnnotatedSpectrum> getMs1Spectra() {
        return ms1Spectra;
    }
}
