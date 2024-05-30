package de.unijena.bioinf.babelms.cef;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static de.unijena.bioinf.babelms.ParserTestUtils.loadExperiment;
import static de.unijena.bioinf.babelms.ParserTestUtils.loadExperiments;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgilentCefExperimentParserTest {

    @Test
    void unknownAdductTest() throws IOException {
        Ms2Experiment experiment = loadExperiment("cef/fbf_unknown_adduct.cef");
        assertTrue(experiment.getPrecursorIonType().isUnknownPositive());
    }

    @Test
    void knownAdductsTest() throws IOException {
        List<Ms2Experiment> experiments = loadExperiments("cef/fbf_known_adducts.cef");
        assertEquals("[M + H]+", experiments.get(0).getPrecursorIonType().toString());
        assertEquals("[M + Na]+", experiments.get(1).getPrecursorIonType().toString());
    }
}