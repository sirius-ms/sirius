package de.unijena.bioinf.spectraldb;

import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.OrderedSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.OrderedSpectrumDelegate;
import lombok.Getter;

@Getter
public class IndexedQuerySpectrumWrapper extends OrderedSpectrumDelegate<Peak> {

    private final int queryIndex;

    public IndexedQuerySpectrumWrapper(OrderedSpectrum<Peak> querySpectrum, int queryIndex) {
        super(querySpectrum);
        this.queryIndex = queryIndex;
    }
}
