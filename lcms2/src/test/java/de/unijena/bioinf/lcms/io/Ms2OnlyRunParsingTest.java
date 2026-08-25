package de.unijena.bioinf.lcms.io;

import de.unijena.bioinf.lcms.LCMSStorageFactory;
import de.unijena.bioinf.lcms.centroiding.CentroidIndividualSpectraStrategy;
import de.unijena.bioinf.lcms.projectspace.CollectingDatabaseAdapter;
import de.unijena.bioinf.lcms.spectrum.Ms2SpectrumHeader;
import de.unijena.bioinf.lcms.trace.LCMSStorage;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.ms.persistence.model.core.run.Chromatography;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A run that holds nothing but MS/MS scans. Instruments produce them - a targeted list acquired alongside a
 * separate survey run, or an export that kept only the fragmentation - and the reader used to refuse the whole
 * file, so nothing behind it ever got a chance to look at the spectra.
 */
public class Ms2OnlyRunParsingTest {

    @TempDir
    Path tmp;

    @Test
    public void aRunWithoutAnyMs1ScanIsStillRead() throws IOException {
        final Path file = tmp.resolve("ms2only.mzML");
        final SyntheticMzML mzml = SyntheticMzML.builder();
        mzml.ms2(120d, 180.0812, 1.0, 20d, null,
                new double[]{53.0386, 110.0713, 163.0603}, new double[]{500, 1200, 900});
        mzml.ms2(126d, 305.1401, 1.0, 20d, null,
                new double[]{72.0808, 145.0491, 287.1295}, new double[]{800, 400, 1500});
        mzml.writeTo(file);

        final ProcessedSample sample = parse(file);
        assertEquals(0, sample.getMapping().length(), "an MS2 only run has no scan point axis");
        final List<Ms2SpectrumHeader> headers = new ArrayList<>();
        sample.getStorage().getSpectrumStorage().ms2SpectraHeader().forEach(headers::add);
        assertEquals(2, headers.size(), "both MS/MS scans are read");
        assertEquals(180.0812, headers.get(0).getPrecursorMz(), 1e-6);
        assertEquals(120d, headers.get(0).getRetentionTime(), 1e-6);
    }

    @Test
    public void aRunWithoutAnySpectrumAtAllIsStillRefused() throws IOException {
        final Path file = tmp.resolve("empty.mzML");
        SyntheticMzML.builder().writeTo(file);
        assertThrows(IOException.class, () -> parse(file));
    }

    @Test
    public void anOrdinaryRunKeepsItsScanPointAxis() throws IOException {
        final Path file = tmp.resolve("dda.mzML");
        final SyntheticMzML mzml = SyntheticMzML.builder();
        final int parent = mzml.ms1(120d, new double[]{180.0812, 305.1401}, new double[]{9000, 4000});
        mzml.ms2(121d, 180.0812, 1.0, 20d, parent,
                new double[]{53.0386, 110.0713, 163.0603}, new double[]{500, 1200, 900});
        mzml.ms1(124d, new double[]{180.0812, 305.1401}, new double[]{7000, 4200});
        mzml.writeTo(file);

        final ProcessedSample sample = parse(file);
        assertEquals(2, sample.getMapping().length());
        assertEquals(120d, sample.getMapping().getRetentionTimeAt(0), 1e-6);
    }

    private ProcessedSample parse(Path file) throws IOException {
        final LCMSStorageFactory factory = LCMSStorage.temporaryStorage(null, false);
        return LCMSImporter.importToProject(file, null, factory, new CollectingDatabaseAdapter(),
                new CentroidIndividualSpectraStrategy(), false, Chromatography.LC);
    }
}
