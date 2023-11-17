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

import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.babelms.MsExperimentParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class MonaJsonParserTest {

    private Ms2Experiment loadExperiment(String file) throws IOException {
        File input = new File(this.getClass().getClassLoader().getResource(file).getFile());
        return new MsExperimentParser().getParser(input).parseFromFile(input).get(0);
    }

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
    }
}