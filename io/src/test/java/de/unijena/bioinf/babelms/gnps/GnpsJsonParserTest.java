package de.unijena.bioinf.babelms.gnps;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.Smiles;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.babelms.ParserTestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static de.unijena.bioinf.babelms.ParserTestUtils.loadExperiment;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GnpsJsonParserTest {

    @ParameterizedTest
    @ValueSource(strings = {"gnps/collection_format.json", "gnps/single_format.json"})
    void testParse(String file) throws IOException {
        Ms2Experiment experiment = loadExperiment(file);
        assertTrue(experiment.getMs1Spectra().isEmpty());
        assertEquals(1, experiment.getMs2Spectra().size());
        Ms2Spectrum<Peak> spectrum = experiment.getMs2Spectra().get(0);

        assertTestSpectrum(spectrum);
        assertEquals(940.25, experiment.getIonMass(), 1e-9);
        assertEquals("[M + H]+", experiment.getPrecursorIonType().toString());
        assertEquals("[M + H]+", spectrum.getIonization().toString());

        assertEquals(MsInstrumentation.Instrument.QTOF, experiment.getAnnotation(MsInstrumentation.class).orElseThrow());
        assertEquals("Hoiamide B", experiment.getName());

        assertEquals("InChI=1S/C45H73N5O10S3/c1-14-17-24(6)34(52)26(8)37-25(7)30(58-13)18-31-46-29(19-61-31)39-49-45(12,21-62-39)43-50-44(11,20-63-43)42(57)48-32(22(4)15-2)35(53)27(9)40(55)59-36(23(5)16-3)38(54)47-33(28(10)51)41(56)60-37/h19,22-28,30,32-37,51-53H,14-18,20-21H2,1-13H3,(H,47,54)(H,48,57)/t22-,23-,24+,25-,26-,27+,28+,30-,32-,33-,34-,35-,36-,37-,44+,45+/m0/s1", experiment.getAnnotation(InChI.class).orElseThrow().in3D);

        if (file.contains("collection")) {
            assertEquals("KNGPFNUOXXLKCN-ZNCJFREWSA-N", experiment.getAnnotation(InChI.class).orElseThrow().key);
            assertEquals("C45H73N5O10S3", experiment.getMolecularFormula().toString());

            assertEquals("splash10-00dl-0000011189-0000011189", experiment.getAnnotation(Splash.class).orElseThrow().getSplash());
        } else if (file.contains("single")) {
            // for some reason different splash of the same spectrum
            assertEquals("splash10-00dl-0000011189-2af4a5ce365756c5c803", experiment.getAnnotation(Splash.class).orElseThrow().getSplash());
        }

        assertEquals("CCC[C@@H](C)[C@@H]([C@H](C)[C@@H]1[C@H]([C@H](Cc2nc(cs2)C3=N[C@](CS3)(C4=N[C@](CS4)(C(=O)N[C@H]([C@H]([C@H](C(=O)O[C@H](C(=O)N[C@H](C(=O)O1)[C@@H](C)O)[C@@H](C)CC)C)O)[C@@H](C)CC)C)C)OC)C)O", experiment.getAnnotation(Smiles.class).orElseThrow().smiles);
    }

    private void assertTestSpectrum(Ms2Spectrum<Peak> spectrum) {
        assertEquals(336, spectrum.size());

        assertEquals(278.049927, spectrum.getMzAt(0), 1e-6);
        assertEquals(35793.0, spectrum.getIntensityAt(0), 1e-6);
        assertEquals(940.883240, spectrum.getMzAt(335), 1e-6);
        assertEquals(2.0, spectrum.getIntensityAt(335), 1e-6);

        assertEquals(940.25, spectrum.getPrecursorMz(), 1e-9);
    }

    @Test
    public void testParseSpectrum() throws IOException {
        Ms2Experiment experiment = loadExperiment("gnps/only_spectrum.json");

        assertTrue(experiment.getMs1Spectra().isEmpty());
        assertEquals(1, experiment.getMs2Spectra().size());
        Ms2Spectrum<Peak> spectrum = experiment.getMs2Spectra().get(0);

        assertTestSpectrum(spectrum);
        assertEquals(940.25, experiment.getIonMass(), 1e-9);
        assertEquals("splash10-00dl-0000011189-7d4b9e412ca92d989b2d", experiment.getAnnotation(Splash.class).orElseThrow().getSplash());
    }

    @Test
    public void testRootArray() throws IOException {
        File input = ParserTestUtils.getTestFile("gnps/spectrum_array.json");
        List<Ms2Experiment> experiments = new MsExperimentParser().getParser(input).parseFromFile(input);

        assertEquals(3, experiments.size());
        assertEquals(1.0, experiments.get(0).getIonMass());
        assertEquals(2.0, experiments.get(1).getIonMass());
        assertEquals(3.0, experiments.get(2).getIonMass());
    }
}
