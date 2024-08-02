package de.unijena.bioinf.chemdb;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import org.junit.After;
import org.junit.Before;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

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
public class ChemicalDatabaseTest {

    private ChemicalDatabase db;

    private static long makeCompleteFlag() {
        long bioflag = 0L;
        for (int i = 2; i < 64; ++i) {
            bioflag |= (1L << i);
        }
        return bioflag;
    }

    @Before
    public void open() {
        try {
            //this might need to be adjusted according to the user's connection
            db = new ChemicalDatabase();
            if (!db.hasConnection()) fail("Cannot create database connection");
        } catch (Exception e) {
            fail("Cannot create database connection");
        }
    }

    @After
    public void close() {
        try {
            if (db != null && db.hasConnection(10))
                db.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //@Test
    public void retrieveStructureAndAnnotate() {
        String inchiKey2D = "IYGYMKDQCDOMRE";
        String inChI = "InChI=1S/C20H17NO6/c1-21-5-4-10-6-14-15(25-8-24-14)7-12(10)17(21)18-11-2-3-13-19(26-9-23-13)16(11)20(22)27-18/h2-3,6-7,17-18H,4-5,8-9H2,1H3";
        String smiles = "CN1CCC2=CC3=C(C=C2C1C4C5=C(C6=C(C=C5)OCO6)C(=O)O4)OCO3";
        String name = "Bicculine";

        try {
            List<FingerprintCandidate> candidateList = db.lookupFingerprintsByInchis(Collections.singletonList(inchiKey2D));
            assertEquals("Expected exactly one candidates for the inchi key.", 1, candidateList.size());

            final FingerprintCandidate candidate = candidateList.get(0);

            //these values might change in the future.
            assertEquals("InChI does not match. Standardization changed?", inChI, candidate.getInchi().in2D);
            assertEquals("SMILES does not match. Standardization changed?", smiles, candidate.getSmiles());
            assertEquals("Different name expected", name, candidate.getName());
            assertTrue("Structure should be contained in Chebi.", (candidate.getBitset() & DataSource.CHEBI.searchFlag) > 0);
            assertTrue("Structure should be contained in PubChem.", (candidate.getBitset() & DataSource.PUBCHEM.searchFlag) > 0);

            //test annotation
            candidate.setBitset(makeCompleteFlag()); //modify to test all available reference tables
            db.annotateCompounds(candidateList);

            //simple fingerprint check
            assertTrue("no positive molecular properties in fingerprint", candidate.getFingerprint().cardinality() > 0);


        } catch (IOException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    //@Test
    public void findFormula() {
        MolecularFormula mf = MolecularFormula.parseOrThrow("C6H12O6");
        PrecursorIonType ionTypeH = PrecursorIonType.getPrecursorIonType("[M+H]+");
        PrecursorIonType ionTypeIntr = PrecursorIonType.getPrecursorIonType("[M]+");
        PrecursorIonType ionTypeHNeg = PrecursorIonType.getPrecursorIonType("[M-H]-");
        PrecursorIonType ionTypeIntrNeg = PrecursorIonType.getPrecursorIonType("[M]-");

        findFormula(mf, ionTypeH);
        findFormula(mf, ionTypeIntr);
        findFormula(mf, ionTypeHNeg);
        findFormula(mf, ionTypeIntrNeg);
    }

    public void findFormula(MolecularFormula mf, PrecursorIonType ionType) {
        double mass = mf.getMass();
        Deviation deviation = new Deviation(5);
        double mz = ionType.neutralMassToPrecursorMass(mass);

        try {
            List<FormulaCandidate> candidates = db.lookupMolecularFormulas(mz, deviation, ionType);
            //contained?
            assertTrue(candidates.stream().anyMatch(c -> c.getFormula().equals(mf)));

        } catch (ChemicalDatabaseException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
}