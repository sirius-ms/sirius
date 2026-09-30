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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Locale;

/**
 * Reads the tab separated structure tables of the custom database import. The first non-empty line
 * decides the layout: a header row naming a SMILES or InChI column (case-insensitive; optional "id" and
 * "name" columns, any order) maps the columns by name - e.g. the files written by the custom database
 * TSV export. Without such a header the table is positional (structure, id, name) and the first line is
 * a data row like any other. Rows without a structure are skipped, blank id and name cells are passed on
 * as null.
 */
final class StructureTableReader {

    private StructureTableReader() {
    }

    /** Receives one row of a structure table: the SMILES or InChI plus the optional id and name. */
    @FunctionalInterface
    interface RowConsumer {
        void accept(@NotNull String structure, @Nullable String id, @Nullable String name);
    }

    /** Reads the structure table row by row and passes each row with a structure on to {@code rows}. */
    static void read(@NotNull BufferedReader reader, @NotNull RowConsumer rows) throws IOException {
        Columns columns = null;
        String line;
        while ((line = reader.readLine()) != null) {
            //skip empty lines
            if (line.isBlank())
                continue;
            String[] parts = line.split("\t");
            if (columns == null) {
                columns = Columns.fromHeader(parts);
                if (columns != null)
                    continue; //header row, no data
                columns = Columns.POSITIONAL;
            }
            final String structure = columns.cell(parts, columns.structure());
            if (structure == null)
                continue;
            rows.accept(structure.trim(), columns.cell(parts, columns.id()), columns.cell(parts, columns.name()));
        }
    }

    /** Column indices of a structure table; -1 marks an absent column. */
    record Columns(int structure, int id, int name) {
        static final Columns POSITIONAL = new Columns(0, 1, 2);

        /** The columns named by the given header cells, or null if the cells are not a header row. */
        @Nullable
        static Columns fromHeader(String[] cells) {
            int smiles = -1, inchi = -1, id = -1, name = -1;
            for (int i = 0; i < cells.length; i++) {
                switch (cells[i].trim().toLowerCase(Locale.ROOT)) {
                    case "smiles" -> smiles = i;
                    case "inchi" -> inchi = i;
                    case "id" -> id = i;
                    case "name" -> name = i;
                    default -> { }
                }
            }
            if (smiles < 0 && inchi < 0)
                return null;
            return new Columns(smiles >= 0 ? smiles : inchi, id, name);
        }

        /** The cell at the given index, or null if the column is absent or the cell is blank. */
        @Nullable
        String cell(String[] parts, int index) {
            return index >= 0 && index < parts.length && !parts[index].isBlank() ? parts[index] : null;
        }
    }
}
