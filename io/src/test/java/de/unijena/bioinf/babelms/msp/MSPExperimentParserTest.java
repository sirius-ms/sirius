/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2024 Bright Giant GmbH
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
package de.unijena.bioinf.babelms.msp;

import de.unijena.bioinf.ChemistryBase.chem.Smiles;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static de.unijena.bioinf.babelms.ParserTestUtils.loadExperiment;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MSPExperimentParserTest {
    @Test
    void parseMspComments() throws IOException {
        Ms2Experiment experiment = loadExperiment("mona/MoNA010759.msp");
        assertEquals("c1cc(cc(c1)N)C(=O)O", experiment.getAnnotation(Smiles.class).orElseThrow().smiles);
    }

}