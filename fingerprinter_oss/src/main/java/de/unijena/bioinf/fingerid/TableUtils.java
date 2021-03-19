
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

package de.unijena.bioinf.fingerid;

import java.io.*;
import java.util.Arrays;
import java.util.BitSet;

/**
 * Read and Write to tab separated tables
 */
public class TableUtils {

    private final BufferedReader reader;
    private final BufferedWriter writer;

    private String[] currentRow;
    private String[] appended;
    private int appends;

    private int inchiColumn=-1;
    private int fpColumn=-1;

    public TableUtils(Reader reader, Writer writer) {
        this.reader = new BufferedReader(reader);
        this.writer = writer != null ? new BufferedWriter(writer) : null;
        appended = new String[5];
        appends=0;
    }

    public void addColumn(String col) {
        if (appends>=appended.length) appended = Arrays.copyOf(appended, appends*2);
        appended[appends++] = col;
    }

    public void replaceColumn(int index, String col) {
        currentRow[index] = col;
    }

    public void replaceFingerprint(String newFingerprint) {
        replaceColumn(fpColumn, newFingerprint);
    }

    public void replaceInchi(String newInchi) {
        replaceColumn(inchiColumn, newInchi);
    }

    public void writeColumn() throws IOException {
        writer.append(currentRow[0]);
        for (int i=1; i < currentRow.length; ++i) {
            writer.append('\t');
            writer.append(currentRow[i]);
        }
        for (int i=0; i < appends; ++i) {
            writer.append('\t');
            writer.append(appended[i]);
        }
        writer.append('\n');
    }

    public String getInchi() {
        return currentRow[inchiColumn];
    }

    public String getFingerprint() {
        return currentRow[fpColumn];
    }

    public BitSet getFingerprintAsBitset() {
        final String col = currentRow[fpColumn];
        final BitSet fp = new BitSet(col.length());
        for (int i=0; i < col.length(); ++i) {
            if (col.charAt(i)=='1') fp.set(i);
        }
        return fp;
    }

    public boolean[] getFingerprintAsBoolean() {
        final String col = currentRow[fpColumn];
        final boolean[] fp = new boolean[col.length()];
        for (int i = 0; i < col.length(); ++i) {
            if (col.charAt(i) == '1') fp[i] = true;
        }
        return fp;
    }

    public int findInchiColumn() {
        for (int i=0; i < currentRow.length; ++i) {
            if (currentRow[i].startsWith("InChI=")) {
                inchiColumn = i;
                return i;
            }
        }
        return -1;
    }

    public void flush() throws IOException {
        writer.flush();
    }

    public void close() throws IOException {
        if (writer != null) closeWrite();
        closeRead();
    }

    public void closeWrite() throws IOException {
        writer.close();
    }

    public void closeRead() throws IOException {
        reader.close();
    }

    public int findFingerprintColumn() {
        eachColumn:
        for (int i=0; i < currentRow.length; ++i) {
            final String col = currentRow[i];
            for (int j=0; j < col.length(); ++j)
                if (col.charAt(j) != '0' && col.charAt(j)!='1') continue eachColumn;
            fpColumn = i;
            return i;
        }
        return -1;
    }

    public boolean nextRow() throws IOException {
        final String line = reader.readLine();
        if (line==null) {
            currentRow=null;
            return false;
        }
        currentRow = line.split("\t");
        appends=0;
        return true;
    }

}
