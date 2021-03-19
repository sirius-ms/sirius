package de.unijena.bioinf.babelms.ms;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.IsotopeMs2Settings;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MsParserTest{

    @Test
    public void readFile() throws Exception {
        //values example file
        File input = new File(this.getClass().getClassLoader().getResource("Adenosine.ms").getFile());
        Ms2Experiment experiment = new MsExperimentParser().getParser(input).parseFromFile(input).get(0);

        assertEquals("molecule name differs", experiment.getName(), "Adenosine");
        assertEquals("molecular formula differs", experiment.getMolecularFormula(), MolecularFormula.parse("C10H13N5O4"));
        assertEquals("charge differs", (int) experiment.getPrecursorIonType().getCharge(), 1);
        assertEquals("parent mass differs", experiment.getIonMass(), 268.1000061035156, .00000001);

        //test spectra
        List<SimpleSpectrum> ms1spectra = experiment.getMs1Spectra();
        List<? extends Ms2Spectrum<Peak>> ms2spectra = experiment.getMs2Spectra();
        assertEquals("there are 4 ms2 spectra in the file but parsed where " + ms2spectra.size(), 4, ms2spectra.size());
        assertEquals("there is 1 ms1 spectrum in the file but parsed where " + ms1spectra.size(), 1, ms1spectra.size());

        //test parameters from file
        assertTrue("IsotopeMs2Settings not set", experiment.hasAnnotation(IsotopeMs2Settings.class));
        assertEquals("IsotopeMs2Settings Strategy differs", IsotopeMs2Settings.Strategy.SCORE, experiment.getAnnotationOrThrow(IsotopeMs2Settings.class).value);

        //test default parameters
        assertTrue("NumberOfCandidates not set", experiment.hasAnnotation(NumberOfCandidates.class));
        assertEquals("NumberOfCandidates differs from DEFAULT", PropertyManager.DEFAULTS.createInstanceWithDefaults(NumberOfCandidates.class).value, experiment.getAnnotationOrThrow(NumberOfCandidates.class).value);
    }
}
