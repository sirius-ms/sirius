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

package de.unijena.bioinf.spectraldb;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.chemdb.nitrite.ChemicalNitriteDatabase;
import de.unijena.bioinf.spectraldb.entities.MergedReferenceSpectrum;
import de.unijena.bioinf.spectraldb.entities.Ms2ReferenceSpectrum;
import de.unijena.bioinf.spectraldb.entities.ReferenceSpectrum;
import de.unijena.bionf.fastcosine.FastCosine;
import de.unijena.bionf.fastcosine.SearchPreparedSpectrum;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The {@code searchPreparedSpectrum} of a reference spectrum roughly doubles the cost of reading a row, so it
 * is an optional field that a read has to ask for. Optional-field projection matches raw document keys and
 * silently ignores a name that matches nothing - no error, no log - so a wrong or missing declaration is
 * invisible until someone measures it (#931). These tests pin both halves down.
 */
public class SpectralNoSQLOptionalFieldsTest {

    private static final String INCHI_KEY = "GZCGUPFRVQAUEE";
    private static final PrecursorIonType ION_TYPE = PrecursorIonType.fromString("[M+H]+");

    private static ChemicalNitriteDatabase db;

    @BeforeAll
    public static void setup() throws IOException {
        db = new ChemicalNitriteDatabase(Files.createTempFile("spectralDB-nitrite_", "_unitTest"));

        FastCosine fastCosine = new FastCosine();
        SearchPreparedSpectrum prepared = fastCosine.prepareQuery(271.0601, glucosePlusH());

        db.getStorage().insert(Ms2ReferenceSpectrum.builder()
                .candidateInChiKey(INCHI_KEY)
                .precursorIonType(ION_TYPE)
                .precursorMz(271.0601)
                .exactMass(270.0528)
                .formula(MolecularFormula.parseOrThrow("C15H10O5"))
                .name("Apigenin")
                .libraryName("test-library")
                .msLevel(2)
                .spectrum(glucosePlusH())
                .searchPreparedSpectrum(prepared)
                .build());

        MergedReferenceSpectrum merged = new MergedReferenceSpectrum();
        merged.setCandidateInChiKey(INCHI_KEY);
        merged.setPrecursorIonType(ION_TYPE);
        merged.setPrecursorMz(271.0601);
        merged.setExactMass(270.0528);
        merged.setName("Apigenin");
        merged.setLibraryName("test-library");
        merged.setSearchPreparedSpectrum(fastCosine.prepareMergedQuery(List.of(prepared)));
        db.getStorage().insert(merged);
    }

    /** a realistic small MS2, apigenin-like fragments with a ~1.1% 13C satellite on the base peak */
    private static SimpleSpectrum glucosePlusH() {
        SimpleMutableSpectrum spec = new SimpleMutableSpectrum();
        spec.addPeak(153.0182, 41003);
        spec.addPeak(154.0215, 451); // 13C of 153
        spec.addPeak(163.0390, 4821);
        spec.addPeak(229.0495, 6188);
        spec.addPeak(271.0601, 98431);
        return new SimpleSpectrum(spec);
    }

    @Test
    public void ms2DefaultReadOmitsThePreparedSpectrum() throws IOException {
        List<Ms2ReferenceSpectrum> read = db.getStorage().findAllStr(Ms2ReferenceSpectrum.class).toList();

        assertEquals(1, read.size());
        assertNull(read.getFirst().getSearchPreparedSpectrum(),
                "a default read must not pay for the prepared spectrum");
        assertNull(read.getFirst().getSpectrum(), "the raw spectrum is optional as well");
    }

    @Test
    public void ms2ReadCanRequestThePreparedSpectrum() throws IOException {
        List<Ms2ReferenceSpectrum> read = db.getStorage()
                .findAllStr(Ms2ReferenceSpectrum.class, "searchPreparedSpectrum").toList();

        assertEquals(1, read.size());
        assertNotNull(read.getFirst().getSearchPreparedSpectrum(),
                "the declared name must match the stored key, otherwise projection is a silent no-op");
        //not asserting the peak count: FastCosine strips the parent peak and applies a noise threshold,
        //which is its business, not this test's
        assertTrue(read.getFirst().getSearchPreparedSpectrum().size() > 0);
    }

    /**
     * {@code withSpectrum} had no declaration behind it at all, so it never took effect for merged spectra.
     */
    @Test
    public void mergedReadWithoutSpectrumOmitsIt() throws IOException {
        MergedReferenceSpectrum merged = db.getMergedReferenceSpectrum(INCHI_KEY, ION_TYPE, false);

        assertNotNull(merged);
        assertNull(merged.getSearchPreparedSpectrum(), "withSpectrum=false must project the spectrum out");
    }

    @Test
    public void mergedReadWithSpectrumLoadsIt() throws IOException {
        MergedReferenceSpectrum merged = db.getMergedReferenceSpectrum(INCHI_KEY, ION_TYPE, true);

        assertNotNull(merged);
        assertNotNull(merged.getSearchPreparedSpectrum(), "withSpectrum=true must load the spectrum");
    }

    /**
     * The analogue search scores every merged spectrum it gets from here, so it needs the prepared spectrum.
     */
    @Test
    public void mergedIterationLoadsTheSpectrum() throws IOException {
        List<MergedReferenceSpectrum> read = new ArrayList<>();
        db.forEachMergedSpectrum(read::add);

        assertEquals(1, read.size());
        assertNotNull(read.getFirst().getSearchPreparedSpectrum(), "the analogue search scores against it");
    }

    @Test
    public void mergedReadByUuidLoadsTheSpectrum() throws IOException {
        long uuid = db.getMergedReferenceSpectrum(INCHI_KEY, ION_TYPE, false).getUuid();

        ReferenceSpectrum merged = db.getReferenceSpectrum(uuid, SpectrumType.MERGED_SPECTRUM);

        assertNotNull(merged.getSearchPreparedSpectrum(), "the match view shows it as the reference spectrum");
    }
}
