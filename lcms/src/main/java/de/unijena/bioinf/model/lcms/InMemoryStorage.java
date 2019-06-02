package de.unijena.bioinf.model.lcms;

import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import gnu.trove.map.hash.TIntObjectHashMap;

/**
 * Stores spectra in memory. Not threadsafe when used for writing!
 */
public class InMemoryStorage implements SpectrumStorage {

    private final TIntObjectHashMap<SimpleSpectrum> scan2spectrum;

    public InMemoryStorage() {
        this.scan2spectrum = new TIntObjectHashMap<>();
    }

    @Override
    public void add(Scan scan, SimpleSpectrum spectrum) {
        scan2spectrum.put(scan.getScanNumber(), spectrum);
    }

    @Override
    public SimpleSpectrum getScan(Scan scan) {
        return scan2spectrum.get(scan.getScanNumber());
    }
}
