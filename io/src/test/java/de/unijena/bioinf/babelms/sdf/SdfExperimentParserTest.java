package de.unijena.bioinf.babelms.sdf;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.Smiles;
import de.unijena.bioinf.ChemistryBase.ms.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static de.unijena.bioinf.babelms.ParserTestUtils.loadExperiment;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SdfExperimentParserTest {

    @Test
    void parseTest() throws IOException {
        Ms2Experiment experiment = loadExperiment("sdf/UP000538.sdf");

        assertTrue(experiment.getMs1Spectra().isEmpty());
        assertEquals(1, experiment.getMs2Spectra().size());

        Ms2Spectrum<Peak> spectrum = experiment.getMs2Spectra().get(0);
        assertEquals(1, spectrum.size());
        assertEquals(349.1844, spectrum.getMzAt(0), 1e-9);
        assertEquals(100, spectrum.getIntensityAt(0), 1e-9);
        assertEquals(393.2095, spectrum.getPrecursorMz(), 1e-9);

        assertEquals("Octaethylene glycol", experiment.getName());
        assertEquals("C16H34O9", experiment.getMolecularFormula().toString());

        assertEquals(MsInstrumentation.Instrument.ORBI, experiment.getAnnotation(MsInstrumentation.class).orElseThrow());

        assertEquals("InChI=1S/C16H34O9/c17-1-3-19-5-7-21-9-11-23-13-15-25-16-14-24-12-10-22-8-6-20-4-2-18/h17-18H,1-16H2", experiment.getAnnotation(InChI.class).orElseThrow().in3D);
        assertEquals("GLZWNFNQMJAZGY-UHFFFAOYSA-N", experiment.getAnnotation(InChI.class).orElseThrow().key);

        assertEquals("OCCOCCOCCOCCOCCOCCOCCOCCO", experiment.getAnnotation(Smiles.class).orElseThrow().smiles);
        assertEquals("splash10-0002-0009000000-d849df50ad373f4c881c", experiment.getAnnotation(Splash.class).orElseThrow().getSplash());

        assertEquals("[M + Na]+", experiment.getPrecursorIonType().toString());
    }
}