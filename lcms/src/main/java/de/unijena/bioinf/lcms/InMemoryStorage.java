package de.unijena.bioinf.lcms;

import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.model.lcms.Scan;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.IOException;

/**
 * Stores spectra in memory. Not threadsafe when used for writing!
 */
public class InMemoryStorage implements SpectrumStorage {

    protected final TIntObjectHashMap<SimpleSpectrum> scan2spectrum;

    public InMemoryStorage() {
        this.scan2spectrum = new TIntObjectHashMap<>();
    }

    @Override
    public synchronized void add(Scan scan, SimpleSpectrum spectrum) {
        scan2spectrum.put(scan.getIndex(), spectrum);
    }

    @Override
    public SimpleSpectrum getScan(Scan scan) {
        return scan2spectrum.get(scan.getIndex());
    }

    @Override
    public void close() throws IOException {
        // dummy
    }
}
