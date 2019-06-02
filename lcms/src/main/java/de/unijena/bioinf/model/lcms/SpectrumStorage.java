package de.unijena.bioinf.model.lcms;

import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;

/**
 * Stores a large number of spectra and retrieves them.
 */
public interface SpectrumStorage {

    public void add(Scan scan, SimpleSpectrum spectrum);

    public SimpleSpectrum getScan(Scan scan);

}
