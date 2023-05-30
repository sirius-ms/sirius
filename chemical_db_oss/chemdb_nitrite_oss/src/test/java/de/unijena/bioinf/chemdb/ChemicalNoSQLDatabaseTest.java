/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.chemdb;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.chemdb.nitrite.ChemicalNitriteDatabase;
import de.unijena.bioinf.chemdb.nitrite.wrappers.FingerprintWrapper;
import de.unijena.bioinf.spectraldb.SpectralNoSQLDatabase;
import de.unijena.bioinf.spectraldb.entities.Ms2SpectralData;
import de.unijena.bioinf.spectraldb.entities.Ms2SpectralMetadata;
import de.unijena.bioinf.storage.blob.file.FileBlobStorage;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class ChemicalNoSQLDatabaseTest {

    static ChemicalNitriteDatabase chemDb;
    static MolecularFormula[] formulas;

    static List<String> inchis2d;
    static List<String> names;
    static List<CompoundCandidate> compoundCandidates;

    @BeforeClass
    public static void importData() throws IOException {

        ChemicalBlobDatabase<?> source = new ChemicalBlobDatabase<>(new FileBlobStorage(Path.of("src/test/resources/test-blob-db").toAbsolutePath()));
        List<FingerprintCandidate> candidates = new ArrayList<>();
        for (MolecularFormula formula : source.formulas)
            candidates.addAll(source.lookupStructuresAndFingerprintsByFormula(formula));

        formulas = source.formulas;

        inchis2d = new ArrayList<>();
        names = new ArrayList<>();
        compoundCandidates = new ArrayList<>();

        for (MolecularFormula formula : formulas)
            source.lookupStructuresByFormula(formula).forEach(c -> {
                inchis2d.add(c.getInchiKey2D());
                names.add(c.getName());
                compoundCandidates.add(c);
            });

        Path tempDB = Files.createTempFile("chemDB-nitrite_", "_unitTest");
        chemDb = new ChemicalNitriteDatabase(tempDB);
        ChemicalNoSQLDBs.importCompoundsAndFingerprintsLazy(chemDb.getStorage(), candidates, null, "2099-12-24", null, 5, 100);
    }

    @Test
    public void rawTestTags() throws IOException {
        List<SpectralNoSQLDatabase.Tag> tags = chemDb.getStorage().findAllStr(SpectralNoSQLDatabase.Tag.class).toList();
        assertEquals(2, tags.size());
        assertTrue(tags.contains(SpectralNoSQLDatabase.Tag.of(ChemDbTags.TAG_DATE, "2099-12-24")));
        assertTrue(tags.contains(SpectralNoSQLDatabase.Tag.of(ChemDbTags.TAG_FP_ID, String.valueOf(5))));
    }

    @Test
    public void rawTestCompounds() throws IOException {
        List<FingerprintCandidate> fcs = chemDb.getStorage().findAllStr(FingerprintCandidate.class).toList();
        assertEquals(21, fcs.size());
        fcs.forEach(fc -> assertNull(fc.getFingerprint()));
    }

    @Test
    public void rawTestFps() throws IOException {
        List<FingerprintWrapper> fps = chemDb.getStorage().findAllStr(FingerprintWrapper.class).toList();
        assertEquals(21, fps.size());
        fps.forEach(fc -> assertNotNull(fc.getFingerprint()));
    }

    @Test
    public void rawTestSpectraMeta() throws IOException {
        List<Ms2SpectralMetadata> specMeta = chemDb.getStorage().findAllStr(Ms2SpectralMetadata.class).toList();
        assertEquals(0, specMeta.size());
    }

    @Test
    public void rawTestSpectraData() throws IOException {
        Stream<Ms2SpectralData> spectData = chemDb.getStorage().findAllStr(Ms2SpectralData.class);
        assertEquals(0, spectData.count());
    }

    @Test
    public void lookUpStructureAndFingerprintByFormulaTest() throws IOException {
        for (MolecularFormula formula : formulas) {
            List<FingerprintCandidate> candidates = chemDb.lookupStructuresAndFingerprintsByFormula(formula);
            assertFalse(candidates.isEmpty());
            candidates.forEach(fc ->
                    assertNotNull(fc.getFingerprint()));
        }
    }

    @Test
    public void lookUpCompoundsByFormulaTest() throws IOException {
        for (MolecularFormula formula : formulas) {
            List<CompoundCandidate> candidates = chemDb.lookupStructuresByFormula(formula);
            assertFalse(candidates.isEmpty());
        }
    }

    @Test
    public void lookupFingerprintsByInchiTest() throws IOException {
        List<FingerprintCandidate> candidates = chemDb.lookupFingerprintsByInchis(inchis2d);
        assertFalse(candidates.isEmpty());
        assertEquals(21, candidates.size());
        candidates.forEach(fc ->
                assertNotNull(fc.getFingerprint()));
    }

    @Test
    public void lookupFingerprintsByInchiInPlaceTest() throws IOException {
        List<FingerprintCandidate> candidates = chemDb.lookupFingerprintsByInchi(compoundCandidates);
        assertEquals(compoundCandidates.size(), candidates.size());
        //todo if correctly implemented find solution to test for object identity.
        candidates.forEach(fc ->
                assertNotNull(fc.getFingerprint()));
    }

    @Test
    public void findInchiByNamesTest() throws ChemicalDatabaseException {
        Iterator<String> it = inchis2d.iterator();
        for (String name : names) {
            String inchi = it.next();
            List<InChI> res = chemDb.findInchiByNames(List.of(name));
            assertTrue(res.stream().anyMatch(c -> c.key2D().equals(inchi)));
        }
    }

    @Test
    public void  lookupMolecularFormulas() throws ChemicalDatabaseException{
//        chemDb.lookupMolecularFormulas()
        throw new NotImplementedException("Implement this test!");
    }
    @Test
    public void containsFormula() throws ChemicalDatabaseException {
        for (MolecularFormula formula : formulas)
            assertTrue(chemDb.containsFormula(formula));
    }

    @Test
    public void notContainsFormula() throws ChemicalDatabaseException {
        assertFalse(chemDb.containsFormula(MolecularFormula.parseOrThrow("C6H12O6")));
    }


    //    @Test
    public void getChemDbDateTest() throws ChemicalDatabaseException {
        //todo check db date if decided that it must be available or not.
    }

    @Test(expected = UnsupportedOperationException.class)
    public void annotateCompoundsTest() throws ChemicalDatabaseException {
        chemDb.annotateCompounds(List.of());
    }


}
