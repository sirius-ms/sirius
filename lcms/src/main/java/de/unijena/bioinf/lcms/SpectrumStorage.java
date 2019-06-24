package de.unijena.bioinf.lcms;

import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.model.lcms.Scan;

/**
 * Stores a large number of spectra and retrieves them.
 */
public interface SpectrumStorage {

    public void add(Scan scan, SimpleSpectrum spectrum);

    public SimpleSpectrum getScan(Scan scan);

}
