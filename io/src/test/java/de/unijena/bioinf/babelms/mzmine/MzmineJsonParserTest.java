/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */


package de.unijena.bioinf.babelms.mzmine;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.chem.Smiles;
import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MsInstrumentation;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.babelms.json.JsonExperimentParserDispatcher;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.util.List;

import static de.unijena.bioinf.babelms.ParserTestUtils.loadExperiment;
import static de.unijena.bioinf.babelms.ParserTestUtils.loadExperiments;
import static org.junit.jupiter.api.Assertions.*;

/**
 * mzmine writes spectral libraries (and its feature exports) as one flat json object per spectrum, one after another -
 * not as a json array. The fixtures are synthetic but use exactly the keys and value types of real mzmine files.
 */
public class MzmineJsonParserTest {

    @Test
    void everyRecordOfALibraryIsParsed() throws IOException {
        List<Ms2Experiment> experiments = loadExperiments("mzmine/mzmine_library.json");
        assertEquals(List.of("Quercetin", "Quercetin", "Caffeine"), experiments.stream().map(Ms2Experiment::getName).toList());
    }

    @Test
    void recordFieldsAreParsed() throws IOException {
        Ms2Experiment exp = loadExperiment("mzmine/mzmine_library.json");

        assertEquals(303.049929, exp.getIonMass(), 1e-9);
        assertEquals(PrecursorIonType.fromString("[M+H]+"), exp.getPrecursorIonType());
        assertEquals(MolecularFormula.parseOrThrow("C15H10O7"), exp.getMolecularFormula());
        assertEquals("O=C1C(O)=C(Oc2cc(O)cc(O)c12)c1ccc(O)c(O)c1", exp.getAnnotation(Smiles.class).orElseThrow().smiles);
        assertEquals("REFJWTPEDVJJIY-UHFFFAOYSA-N", exp.getAnnotation(InChI.class).orElseThrow().key);
        assertEquals(MsInstrumentation.Instrument.ORBI, exp.getAnnotation(MsInstrumentation.class).orElseThrow());
        // mzmine writes the retention time in minutes
        assertEquals(5.2 * 60, exp.getAnnotation(RetentionTime.class).orElseThrow().getRetentionTimeInSeconds(), 1e-9);

        assertTrue(exp.getMs1Spectra().isEmpty());
        assertEquals(1, exp.getMs2Spectra().size());
        Ms2Spectrum<?> ms2 = exp.getMs2Spectra().get(0);
        assertEquals(2, ms2.getMsLevel());
        assertEquals(303.049929, ms2.getPrecursorMz(), 1e-9);
        assertEquals(7, ms2.size());
        assertEquals(137.0233, ms2.getMzAt(0), 1e-9);
        assertEquals(9120.0, ms2.getIntensityAt(0), 1e-9);
        assertEquals(60.0, ms2.getCollisionEnergy().minEnergySource(), 1e-9);
        assertEquals(60.0, ms2.getCollisionEnergy().maxEnergySource(), 1e-9);
    }

    /**
     * A spectrum merged across several collision energies lists all of them; it is represented as the range they span.
     */
    @Test
    void mergedCollisionEnergiesBecomeARange() throws IOException {
        CollisionEnergy ce = loadExperiments("mzmine/mzmine_library.json").get(1).getMs2Spectra().get(0).getCollisionEnergy();
        assertEquals(20.0, ce.minEnergySource(), 1e-9);
        assertEquals(60.0, ce.maxEnergySource(), 1e-9);
    }

    @Test
    void adductOtherThanProtonation() throws IOException {
        Ms2Experiment exp = loadExperiments("mzmine/mzmine_library.json").get(2);
        assertEquals(PrecursorIonType.fromString("[M+Na]+"), exp.getPrecursorIonType());
        assertEquals(30.0, exp.getMs2Spectra().get(0).getCollisionEnergy().minEnergySource(), 1e-9);
    }

    @Test
    void layoutOfTheRecordsDoesNotMatter() throws IOException {
        List<Ms2Experiment> oneLine = loadExperiments("mzmine/mzmine_library.json");
        assertEquals(3, oneLine.size());
        for (String file : List.of("mzmine/mzmine_library_pretty.json", "mzmine/mzmine_library_array.json")) {
            List<Ms2Experiment> other = loadExperiments(file);
            assertEquals(oneLine.stream().map(Ms2Experiment::getName).toList(), other.stream().map(Ms2Experiment::getName).toList(), file);
            assertEquals(oneLine.stream().map(Ms2Experiment::getIonMass).toList(), other.stream().map(Ms2Experiment::getIonMass).toList(), file);
        }
    }

    /**
     * The json export of an mzmine feature list (mzmine 4.10) - no compound name, but otherwise the library keys.
     */
    @Test
    void featureListExportIsParsed() throws IOException {
        Ms2Experiment exp = loadExperiment("mzmine/mzmine_feature_export.json");
        assertEquals(PrecursorIonType.fromString("[M+H]+"), exp.getPrecursorIonType());
        assertEquals("COC1=CC=C(C=C1O)C2CC(C=3C(=CC(=CC3O2)O)O)=O", exp.getAnnotation(Smiles.class).orElseThrow().smiles);
        assertEquals(MsInstrumentation.Instrument.QTOF, exp.getAnnotation(MsInstrumentation.class).orElseThrow());
        assertEquals(1, exp.getMs2Spectra().size());
        assertEquals(4, exp.getMs2Spectra().get(0).size());
        assertEquals(40.0, exp.getMs2Spectra().get(0).getCollisionEnergy().minEnergySource(), 1e-9);
    }

    /**
     * Other tools may adopt the format without writing mzmine's bookkeeping keys, so a record is recognised by its
     * content alone.
     */
    @Test
    void recordWithoutMzmineSpecificKeysIsParsed() throws IOException {
        Ms2Experiment exp = loadExperiment("mzmine/mzmine_format_from_other_tool.json");
        assertEquals("Quercetin", exp.getName());
        assertEquals(PrecursorIonType.fromString("[M+H]+"), exp.getPrecursorIonType());
        assertEquals(7, exp.getMs2Spectra().get(0).size());
    }

    /**
     * Libraries can be far larger than memory, so records must be read one at a time. The input here never ends -
     * reading it completely before returning the first record would never finish.
     */
    @Test
    void recordsAreStreamed() throws IOException {
        String record = new String(readResource("mzmine/mzmine_library.json")).lines().findFirst().orElseThrow() + "\n";
        try (BufferedReader endless = new BufferedReader(new EndlessReader(record))) {
            JsonExperimentParserDispatcher parser = new JsonExperimentParserDispatcher();
            for (int i = 0; i < 3; i++)
                assertEquals("Quercetin", parser.parse(endless, URI.create("file:/endless.json")).getName());
        }
    }

    private static byte[] readResource(String resource) throws IOException {
        try (var in = MzmineJsonParserTest.class.getClassLoader().getResourceAsStream(resource)) {
            return in.readAllBytes();
        }
    }

    /** Repeats the given text forever. */
    private static class EndlessReader extends Reader {
        private final String text;
        private int pos = 0;

        EndlessReader(String text) {
            this.text = text;
        }

        @Override
        public int read(char[] cbuf, int off, int len) {
            for (int i = 0; i < len; i++) {
                cbuf[off + i] = text.charAt(pos);
                pos = (pos + 1) % text.length();
            }
            return len;
        }

        @Override
        public void close() {
        }
    }
}
