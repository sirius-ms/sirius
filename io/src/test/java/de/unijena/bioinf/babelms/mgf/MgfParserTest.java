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

package de.unijena.bioinf.babelms.mgf;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.babelms.ParserTestUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The feature id of an {@link Ms2Experiment} becomes the external feature id of the aligned feature
 * (StorageUtils#toFeature), which is what users map results back onto. MS-DIAL writes no TITLE and no
 * FEATURE_ID; its identifier is the SCANS value, so SCANS has to reach the feature id as well as the name.
 */
public class MgfParserTest {

    @Test
    public void scansBecomesFeatureIdWhenNoFeatureIdIsGiven() throws IOException {
        List<Ms2Experiment> experiments = ParserTestUtils.loadExperiments("mgf/msdial_scans_only.mgf");

        assertEquals(2, experiments.size());
        assertEquals("1084", experiments.get(0).getFeatureId());
        assertEquals("1207", experiments.get(1).getFeatureId());
    }

    @Test
    public void scansIsStillUsedAsNameWhenNoTitleIsGiven() throws IOException {
        Ms2Experiment exp = ParserTestUtils.loadExperiment("mgf/msdial_scans_only.mgf");
        assertEquals("1084", exp.getName());
    }

    @Test
    public void explicitFeatureIdWinsOverScans() throws IOException {
        Ms2Experiment exp = ParserTestUtils.loadExperiment("mgf/scans_and_feature_id.mgf");
        assertEquals("e_42", exp.getFeatureId());
    }

    /**
     * TITLE is a name, SCANS is an identifier - a file carrying both should end up with both, rather than
     * losing the identifier because a name happened to be present.
     */
    @Test
    public void titleBecomesNameAndScansBecomesFeatureId() throws IOException {
        Ms2Experiment exp = ParserTestUtils.loadExperiment("mgf/title_and_scans.mgf");
        assertEquals("Quercetin", exp.getName());
        assertEquals("2311", exp.getFeatureId());
    }

    /**
     * The name assignment is order dependent (SCANS only fills a still-empty name, TITLE always overwrites).
     * The feature id must not inherit that dependency.
     */
    @Test
    public void featureIdFromScansDoesNotDependOnKeywordOrder() throws IOException {
        Ms2Experiment scansFirst = ParserTestUtils.loadExperiment("mgf/scans_before_title.mgf");
        Ms2Experiment titleFirst = ParserTestUtils.loadExperiment("mgf/title_and_scans.mgf");

        assertEquals("2311", scansFirst.getFeatureId());
        assertEquals(titleFirst.getFeatureId(), scansFirst.getFeatureId());
        assertEquals("Quercetin", scansFirst.getName());
    }

    @Test
    public void spectraAreStillParsed() throws IOException {
        Ms2Experiment exp = ParserTestUtils.loadExperiment("mgf/msdial_scans_only.mgf");
        assertNotNull(exp.getMs2Spectra());
        assertEquals(1, exp.getMs2Spectra().size());
        assertEquals(6, exp.getMs2Spectra().get(0).size());
        assertEquals(271.0601, exp.getIonMass(), 1e-6);
    }

    /**
     * ION_SOURCE names the ionisation technique (ESI, APCI, ...), not the adduct. It starts with ION like
     * IONMODE does, and used to be parsed as an ion type - failing the whole file on "Unknown ion 'ESI'"
     * (GitHub issue #324).
     */
    @Test
    public void ionSourceIsNotReadAsAdduct() throws IOException {
        Ms2Experiment exp = ParserTestUtils.loadExperiment("mgf/ion_source_before_adduct.mgf");
        assertEquals(PrecursorIonType.fromString("[M+H]+"), exp.getPrecursorIonType());
        assertEquals(1, exp.getMs2Spectra().size());
    }
}
