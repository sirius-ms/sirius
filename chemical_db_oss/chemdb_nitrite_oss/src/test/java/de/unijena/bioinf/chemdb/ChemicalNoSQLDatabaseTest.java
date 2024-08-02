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
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.chemdb.nitrite.ChemicalNitriteDatabase;
import de.unijena.bioinf.chemdb.nitrite.wrappers.FingerprintCandidateWrapper;
import de.unijena.bioinf.spectraldb.entities.Ms2ReferenceSpectrum;
import de.unijena.bioinf.storage.blob.file.FileBlobStorage;
import de.unijena.bioinf.storage.db.nosql.Filter;
import de.unijena.bioinf.storage.db.nosql.nitrite.NitriteDatabase;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.Assert.*;

public class ChemicalNoSQLDatabaseTest {

    static ChemicalNitriteDatabase chemDb;
    static List<MolecularFormula> formulas;
    static int[] sizePerFormula;

    static List<String> inchis2d;
    static List<String> names;
    static List<CompoundCandidate> compoundCandidates;

    @BeforeClass
    public static void importData() throws IOException {

        ChemicalBlobDatabase<?> source = new ChemicalBlobDatabase<>(new FileBlobStorage(Path.of("src/test/resources/test-blob-db").toAbsolutePath()), null);
        Map<MolecularFormula, List<FingerprintCandidate>> candidates = new HashMap<>();
        for (MolecularFormula formula : source.index.getFormulas())
            candidates.put(formula, source.lookupStructuresAndFingerprintsByFormula(formula));

        formulas = source.index.getFormulas();
        sizePerFormula = new int[formulas.size()];

        inchis2d = new ArrayList<>();
        names = new ArrayList<>();
        compoundCandidates = new ArrayList<>();

        for (int i = 0; i < formulas.size(); i++) {
            List<CompoundCandidate> cs = source.lookupStructuresByFormula(formulas.get(i));
            sizePerFormula[i] = cs.size();

            cs.forEach(c -> {
                inchis2d.add(c.getInchiKey2D());
                names.add(c.getName());
                compoundCandidates.add(c);
            });
        }
        Path tempDB = Files.createTempFile("chemDB-nitrite_", "_unitTest");
        chemDb = new ChemicalNitriteDatabase(tempDB);
        ChemicalNoSQLDBs.importCandidatesAndSpectra(chemDb, candidates, null, "2099-12-24", null, 5, 100);
    }

    @Test
    public void rawTestTags() throws IOException {
        List<ChemicalNoSQLDatabase.Tag> tags = chemDb.getStorage().findAllStr(ChemicalNoSQLDatabase.Tag.class).toList();
        assertEquals(2, tags.size());
        assertTrue(tags.contains(ChemicalNoSQLDatabase.Tag.of(ChemDbTags.TAG_DATE, "2099-12-24")));
        assertTrue(tags.contains(ChemicalNoSQLDatabase.Tag.of(ChemDbTags.TAG_FP_ID, String.valueOf(5))));
    }

    @Test
    public void rawTestCompounds() throws IOException {
        List<FingerprintCandidateWrapper> fcs = chemDb.getStorage().findAllStr(FingerprintCandidateWrapper.class).toList();
        assertEquals(21, fcs.size());
        fcs.forEach(fc -> assertNotNull(fc.getCandidate(null, null)));
        fcs.forEach(fc -> assertNull(fc.getFingerprint()));

        fcs = chemDb.getStorage().findAllStr(FingerprintCandidateWrapper.class, "fingerprint").toList();
        assertEquals(21, fcs.size());
        fcs.forEach(fc -> assertNotNull(fc.getFingerprint()));
    }

    @Test
    public void rawTestReferenceSpectra() throws IOException {
        List<Ms2ReferenceSpectrum> reference = chemDb.getStorage().findAllStr(Ms2ReferenceSpectrum.class).toList();
        assertEquals(0, reference.size());
    }

    @Test
    public void lookUpStructureAndFingerprintByFormulaTest() throws IOException {
        for (int i = 0; i < formulas.size(); i++) {
            List<FingerprintCandidate> candidates = chemDb.lookupStructuresAndFingerprintsByFormula(formulas.get(i));
            assertFalse(candidates.isEmpty());
            assertEquals(sizePerFormula[i], candidates.size());
            candidates.forEach(fc ->
                    assertNotNull(fc.getFingerprint()));
        }
    }

    @Test
    public void lookUpCompoundsByFormulaTest() throws IOException {
        for (int i = 0; i < formulas.size(); i++) {
            List<CompoundCandidate> candidates = chemDb.lookupStructuresByFormula(formulas.get(i));
            assertFalse(candidates.isEmpty());
            assertEquals(sizePerFormula[i], candidates.size());
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
    public void lookupMolecularFormulas() throws ChemicalDatabaseException {
        Deviation ppm = new Deviation(10d);
        List<PrecursorIonType> ionTypes = List.of(PrecursorIonType.fromString("M+"),PrecursorIonType.fromString("[M+H]+"), PrecursorIonType.fromString("[M+Na]+"));
        for (PrecursorIonType ionType : ionTypes) {
            for (int i = 0; i < formulas.size(); i++) {
                MolecularFormula formula = formulas.get(i);
                double precursormass = ionType.neutralMassToPrecursorMass(formula.getMass());
                List<FormulaCandidate> compounds = chemDb.lookupMolecularFormulas(precursormass, ppm, ionType);
                assertTrue(compounds.size() >= sizePerFormula[i]);
                assertTrue(compounds.stream().anyMatch(c -> c.formula.equals(formula)));
                assertEquals(sizePerFormula[i], compounds.stream().filter(c -> c.formula.equals(formula)).count());
            }
        }
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
    public void getChemDbDateTest() {
        //todo check db date if decided that it must be available or not.
    }

    @Test(expected = UnsupportedOperationException.class)
    public void annotateCompoundsTest() {
        chemDb.annotateCompounds(List.of());
    }

    @Test
    public void testIndexFilter() throws IOException {
        List<FingerprintCandidateWrapper> candidates = chemDb.getStorage().findStr(Filter.where("mass").gt(0.0), FingerprintCandidateWrapper.class).toList();
        assertEquals(21, candidates.size());
        candidates = chemDb.getStorage().findStr(Filter.where("mass").gt(9999.0), FingerprintCandidateWrapper.class).toList();
        assertTrue(candidates.isEmpty());
        candidates = chemDb.getStorage().findStr(Filter.where("mass").gt(500.0), FingerprintCandidateWrapper.class).toList();
        assertEquals(2, candidates.size());
    }

    @Test
    public void testUpsert() throws IOException {
        long fingerprintCountBefore = chemDb.countAllFingerprints();
        NitriteDatabase storage = chemDb.getStorage();
        double avgXLogPBefore = storage.findAllStr(FingerprintCandidateWrapper.class).mapToDouble(w -> w.getCandidate(null, null).xlogp).average().orElse(Double.NaN);

        chemDb.updateAllFingerprints(fpc -> fpc.xlogp += 1);
        double avgXLogPAfter = storage.findAllStr(FingerprintCandidateWrapper.class).mapToDouble(w -> w.getCandidate(null, null).xlogp).average().orElse(Double.NaN);
        assertEquals(fingerprintCountBefore, chemDb.countAllFingerprints());
        assertEquals(avgXLogPBefore + 1, avgXLogPAfter, 10e-9);
    }
}
