package de.unijena.bioinf.spectraldb;

import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.OrderedSpectrumDelegate;
import de.unijena.bioinf.spectraldb.entities.Ms2ReferenceSpectrum;
import lombok.Getter;

@Getter
public class Ms2ReferenceSpectrumWrapper extends OrderedSpectrumDelegate<Peak> {

    private final Ms2ReferenceSpectrum ms2ReferenceSpectrum;

    public Ms2ReferenceSpectrumWrapper(Ms2ReferenceSpectrum ms2ReferenceSpectrum) {
        super(ms2ReferenceSpectrum.getSpectrum());
        this.ms2ReferenceSpectrum = ms2ReferenceSpectrum;
    }
}
