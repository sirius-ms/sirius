/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
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

package de.unijena.bioinf.chemdb.custom;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Column resolution of structure tables: the headerless positional layout (structure, id, name) and
 * tables with a header row, e.g. the files written by the custom database TSV export - the current
 * layout (SMILES, id, name, ...) as well as the one of older versions (name, SMILES, ...).
 */
public class StructureTableParsingTest {

    private static List<List<String>> parse(String table) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        CustomDatabaseImporter.readStructureTable(new BufferedReader(new StringReader(table)),
                (structure, id, name) -> rows.add(Arrays.asList(structure, id, name)));
        return rows;
    }

    @Test
    public void headerlessTableIsPositional() throws IOException {
        assertEquals(List.of(
                        Arrays.asList("CCO", "ID1", "Ethanol"),
                        Arrays.asList("CO", "ID2", null),
                        Arrays.asList("C", null, null)),
                parse("CCO\tID1\tEthanol\n\nCO\tID2\nC\n"));
    }

    @Test
    public void currentExportLayoutIsReimportable() throws IOException {
        String exported = "SMILES\tid\tname\tInChIkey2D\tInChI\tformula\tmass\n"
                + "CCO\tMY-ID-1\tEthanol\tLFQSCWFLJHTTHZ\tInChI=1S/C2H6O/c1-2-3/h3H,2H2,1H3\tC2H6O\t46.04\n"
                + "CO\t\t\tOKKJLVBELUTLKV\tInChI=1S/CH4O/c1-2/h2H,1H3\tCH4O\t32.03\n";
        assertEquals(List.of(
                        Arrays.asList("CCO", "MY-ID-1", "Ethanol"),
                        Arrays.asList("CO", null, null)),
                parse(exported));
    }

    @Test
    public void unidentifiableHeaderKeepsPositionalBehaviour() throws IOException {
        // no SMILES/InChI column named: the first line is a data row like any other (as before)
        assertEquals(List.of(
                        Arrays.asList("structure", "identifier", "compound"),
                        Arrays.asList("CCO", "ID1", "Ethanol")),
                parse("structure\tidentifier\tcompound\nCCO\tID1\tEthanol\n"));
    }

    @Test
    public void legacyExportLayoutIsReimportable() throws IOException {
        String exported = "name\tSMILES\tInChIkey2D\tInChI\tformula\tmass\n"
                + "Ethanol\tCCO\tLFQSCWFLJHTTHZ\tInChI=1S/C2H6O/c1-2-3/h3H,2H2,1H3\tC2H6O\t46.04\n"
                + "\tCO\tOKKJLVBELUTLKV\tInChI=1S/CH4O/c1-2/h2H,1H3\tCH4O\t32.03\n";
        // structure from the SMILES column, name from the name column, no id column
        assertEquals(List.of(
                        Arrays.asList("CCO", null, "Ethanol"),
                        Arrays.asList("CO", null, null)),
                parse(exported));
    }

    @Test
    public void headerColumnsAreCaseInsensitiveAndMayBeReordered() throws IOException {
        assertEquals(List.of(Arrays.asList("CCO", "ID1", "Ethanol")),
                parse("Id\tName\tsmiles\nID1\tEthanol\tCCO\n"));
    }

    @Test
    public void inchiColumnIsUsedWithoutSmilesColumn() throws IOException {
        assertEquals(List.of(Arrays.asList("InChI=1S/CH4O/c1-2/h2H,1H3", null, "Methanol")),
                parse("name\tInChI\nMethanol\tInChI=1S/CH4O/c1-2/h2H,1H3\n"));
    }

    @Test
    public void rowsWithoutStructureAreSkipped() throws IOException {
        assertEquals(List.of(Arrays.asList("CCO", null, "Ethanol")),
                parse("name\tSMILES\nNoStructure\t\nEthanol\tCCO\nShortRow\n"));
    }
}
