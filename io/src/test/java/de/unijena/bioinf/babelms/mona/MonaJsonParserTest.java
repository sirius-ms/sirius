/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.babelms.mona;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.chem.Smiles;
import de.unijena.bioinf.ChemistryBase.data.Tagging;
import de.unijena.bioinf.ChemistryBase.ms.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

import static de.unijena.bioinf.babelms.ParserTestUtils.loadExperiment;
import static org.junit.jupiter.api.Assertions.*;

class MonaJsonParserTest {

    @Test
    void parseMS1() throws IOException {
        Ms2Experiment experiment = loadExperiment("mona/MoNA010759.json");

        assertTrue(experiment.getMs2Spectra().isEmpty());
        assertEquals(1, experiment.getMs1Spectra().size());

        Spectrum<Peak> spectrum = experiment.getMs1Spectra().get(0);
        assertEquals(4, spectrum.size());
        assertEquals(42.03298, spectrum.getMzAt(0), 1e-9);
        assertEquals(3, spectrum.getIntensityAt(0), 1e-9);
        assertEquals(139.0585, spectrum.getMzAt(3), 1e-9);
        assertEquals(7.7, spectrum.getIntensityAt(3), 1e-9);

        assertEquals(MsInstrumentation.Instrument.QTOF, experiment.getAnnotation(MsInstrumentation.class).orElseThrow());
        assertEquals("m-Aminobenzoic acid", experiment.getName());
        assertEquals("C7H7NO2", experiment.getMolecularFormula().toString());

        assertEquals("InChI=1S/C7H7NO2/c8-6-3-1-2-5(4-6)7(9)10/h1-4H,8H2,(H,9,10)", experiment.getAnnotation(InChI.class).orElseThrow().in3D);
        assertEquals("XFDUHJPVQKIXHO-UHFFFAOYSA-N", experiment.getAnnotation(InChI.class).orElseThrow().key);

        assertEquals("O=C(O)C=1C=CC=C(N)C1", experiment.getAnnotation(Smiles.class).orElseThrow().smiles);
        assertEquals("splash10-000i-0900000000-efdee90e857bd8d0327c", experiment.getAnnotation(Splash.class).orElseThrow().getSplash());

        assertEquals(2.087279 * 60, experiment.getAnnotation(RetentionTime.class).orElseThrow().getMiddleTime(), 1e-9);

        Tagging tags = experiment.getAnnotation(Tagging.class).orElseThrow();
        assertEquals(Set.of("Agilent_6550_Q-TOF_AIF", "KI-GIAR_zicHILIC_POS", "LC-MS"), tags.stream().collect(Collectors.toSet()));
    }

    @Test
    void parseMS2() throws IOException {
        Ms2Experiment experiment = loadExperiment("mona/MoNA020112.json");

        assertTrue(experiment.getMs1Spectra().isEmpty());
        assertEquals(1, experiment.getMs2Spectra().size());

        Ms2Spectrum<Peak> spectrum = experiment.getMs2Spectra().get(0);
        assertEquals(25, spectrum.size());
        assertEquals(474.173187255859, spectrum.getPrecursorMz(), 1e-12);
        assertEquals(474.173187255859, experiment.getIonMass(), 1e-12);
        assertEquals(40, spectrum.getCollisionEnergy().getMinEnergy(), 1e-9);
        assertEquals(40, spectrum.getCollisionEnergy().getMaxEnergy(), 1e-9);
        assertEquals("[M + H]+", spectrum.getIonization().toString());
        assertEquals("[M + H]+", experiment.getPrecursorIonType().toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"mona/missing_fields.json", "mona/invalid_fields.json"})
    void cannotParse(String file) throws IOException {
        Ms2Experiment experiment = loadExperiment(file);
        Ms2Spectrum<Peak> spectrum = experiment.getMs2Spectra().get(0);

        assertEquals(0, experiment.getIonMass());
        assertNull(spectrum.getCollisionEnergy());
        assertNull(experiment.getPrecursorIonType());
        assertTrue(experiment.getAnnotation(MsInstrumentation.class).isEmpty());
        assertTrue(experiment.getName().isEmpty());
//        assertNull(experiment.getMolecularFormula());  // todo fails because of sirius-libs#51
        assertTrue(experiment.getAnnotation(InChI.class).isEmpty());
        assertTrue(experiment.getAnnotation(Smiles.class).isEmpty());
        assertTrue(experiment.getAnnotation(Splash.class).isEmpty());
        assertTrue(experiment.getAnnotation(RetentionTime.class).isEmpty());

        assertTrue(experiment.getAnnotation(Tagging.class).isEmpty());
    }
}