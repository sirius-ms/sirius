package de.unijena.bioinf.lcms.spectrum;

import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.lcms.datatypes.SpectrumDatatype;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class SpectrumStorage {


    public abstract void addSpectrum(Ms1SpectrumHeader header, SimpleSpectrum spectrum);
    public abstract SimpleSpectrum getSpectrum(int id);
    public abstract Ms2SpectrumHeader addMs2Spectrum(Ms2SpectrumHeader header, SimpleSpectrum spectrum);
    public abstract Iterable<Ms2SpectrumHeader> ms2SpectraHeader();
    public abstract Ms2SpectrumHeader ms2SpectrumHeader(int id);
    public abstract Ms1SpectrumHeader ms1SpectrumHeader(int id);
    public abstract SimpleSpectrum getMs2Spectrum(int id);


    public static class MvSpectrumStorage extends SpectrumStorage {
        private MVMap<Integer, SimpleSpectrum> spectraMap, ms2SpectraMap;
        private MVMap<Integer, Ms2SpectrumHeader> ms2headers;
        private MVMap<Integer, Ms1SpectrumHeader> ms1Headers;

        private AtomicInteger ms2spectraIds;

        public MvSpectrumStorage(MVStore storage) {
            this.spectraMap = storage.openMap("spectra",
                    new MVMap.Builder<Integer,SimpleSpectrum>().valueType(new SpectrumDatatype()));
            this.ms2headers = storage.openMap("ms2headers", new MVMap.Builder<Integer,Ms2SpectrumHeader>().valueType(new MsSpectrumHeaderDatatype()));
            this.ms2SpectraMap = storage.openMap("ms2spectraMap",
                    new MVMap.Builder<Integer,SimpleSpectrum>().valueType(new SpectrumDatatype()));
            this.ms1Headers = storage.openMap("ms1headerMap", new MVMap.Builder<Integer,Ms1SpectrumHeader>().valueType(new MsSpectrumHeaderDatatype()));
            this.ms2spectraIds = new AtomicInteger(0);
        }


        @Override
        public void addSpectrum(Ms1SpectrumHeader header, SimpleSpectrum spectrum) {
            spectraMap.put(header.getUid(), spectrum);
            ms1Headers.put(header.getUid(), header);
        }

        @Override
        public SimpleSpectrum getSpectrum(int id) {
            return spectraMap.get(id);
        }

        @Override
        public Ms2SpectrumHeader addMs2Spectrum(Ms2SpectrumHeader header, SimpleSpectrum spectrum) {
            int id = ms2spectraIds.incrementAndGet();
            Ms2SpectrumHeader ms2SpectrumHeader = header.withUid(id);

            // TODO: workaround. Validation should be done somewhere else @martin_engler
            if (Double.isNaN(header.getTargetedMz())) {

            }

            ms2SpectraMap.put(id, spectrum);
            ms2headers.put(id, ms2SpectrumHeader);
            return ms2SpectrumHeader;
        }

        @Override
        public Iterable<Ms2SpectrumHeader> ms2SpectraHeader() {
            return this.ms2headers.values();
        }

        @Override
        public Ms2SpectrumHeader ms2SpectrumHeader(int id) {
            return this.ms2headers.get(id);
        }

        @Override
        public Ms1SpectrumHeader ms1SpectrumHeader(int id) {
            return this.ms1Headers.get(id);
        }

        @Override
        public SimpleSpectrum getMs2Spectrum(int id) {
            return this.ms2SpectraMap.get(id);
        }

    }

}
