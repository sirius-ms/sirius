
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ftalign;

import de.unijena.bioinf.ftalign.analyse.Pearson;

import java.io.*;
import java.net.URL;
import java.text.NumberFormat;
import java.util.*;

/**
 * @author Kai Dührkop
 */
public class CSVMatrix {

    public enum FLATTEN {UNSYMETRIC, SYMETRIC, SYMETRIC_WITHOUT_DIAGONAL}

    public final String[] rows;
    public final String[] cols;
    public final double[][] matrix;

    public CSVMatrix(String[] rows, String[] cols, double[][] matrix) {
        this.rows = rows;
        this.cols = cols;
        this.matrix = matrix;
    }

    public double correlation(CSVMatrix other, FLATTEN mode) {
        return Pearson.pearson(flatten(mode), other.flatten(mode));
    }

    public double[] flatten(FLATTEN mode) {
        final int n;
        if (mode == FLATTEN.UNSYMETRIC) {
            n = rows.length * cols.length;
        } else {
            final int msize = ((rows.length*rows.length)-rows.length)/2;
            n = (mode == FLATTEN.SYMETRIC) ? msize + rows.length : msize;
        }
        final double flatten[] = new double[n];
        int k = 0;
        for (int row=0; row < rows.length; ++row) {
            final int startCol = (mode == FLATTEN.UNSYMETRIC ? 0 : (mode == FLATTEN.SYMETRIC ? row : row+1));
            for (int col = startCol; col < cols.length; ++col) {
                flatten[k++] = matrix[row][col];
            }
        }
        return flatten;

    }

    public CSVMatrix reorder(CSVMatrix M) {
        final String[] Mrows = Arrays.copyOf(rows, rows.length);
        final String[] Mcols = Arrays.copyOf(cols, cols.length);
        final double[][] Mmatrix = new double[rows.length][cols.length];
        // rowIndizes[i] = j => row i is in matrix on index j
        final int[] rowIndizes = new int[rows.length];
        final int[] colIndizes = new int[cols.length];
        boolean needReorder = false;
        {
            final HashMap<String, Integer> rowMap = new HashMap<String, Integer>();
            for (int k=0; k < M.rows.length; ++k) {
                rowMap.put(M.rows[k], k);
            }
            for (int k=0; k < rows.length; ++k) {
                final Integer l = rowMap.get(rows[k]);
                if (l == null) return null;
                if (l != k) needReorder = true;
                rowIndizes[k] = l;
            }
        }
        {
            final HashMap<String, Integer> colMap = new HashMap<String, Integer>();
            for (int k=0; k < M.cols.length; ++k) {
                colMap.put(M.cols[k], k);
            }
            for (int k=0; k < cols.length; ++k) {
                final Integer l = colMap.get(cols[k]);
                if (l == null) return null;
                if (l != k) needReorder = true;
                colIndizes[k] = l;
            }
        }
        // reorder cells
        for (int row=0; row < rows.length; ++row) {
            int rowIndex = rowIndizes[row];
            for (int col=0; col < cols.length; ++col) {
                Mmatrix[row][col] = M.matrix[rowIndex][colIndizes[col]];
            }
        }
        return new CSVMatrix(Mrows, Mcols, Mmatrix);
    }
    
    public void mapNames(Map<String, String> aliases) {
        for (int i=0; i < rows.length; ++i) {
            if (!aliases.containsKey(rows[i])) {
                throw new RuntimeException("no alias for '" + rows[i] + "'");
            }
            rows[i] = aliases.get(rows[i]);
        }
        for (int i=0; i < cols.length; ++i) {
            if (!aliases.containsKey(cols[i])) {
                throw new RuntimeException("no alias for '" + cols[i] + "'");
            }
            cols[i] = aliases.get(cols[i]);
        }
    }

    public void write(File file, String dummyHeader) throws IOException {
        final FileWriter writer = new FileWriter(file);
        try {
            write(new BufferedWriter(writer), dummyHeader);
        } catch (IOException exc) {
            throw exc;
        } finally {
            writer.close();
        }
    }
    
    public void write(BufferedWriter writer, String dummyHeader) throws IOException {
    	NumberFormat formatter = NumberFormat.getNumberInstance(Locale.ENGLISH);
    	formatter.setGroupingUsed(false);
        write(writer, dummyHeader, formatter);
    }
    
    public void write(BufferedWriter writer, String dummyHeader, NumberFormat formater) throws IOException {
        // write header
        {
            int i = 0;
            if (dummyHeader != null) {
                writer.write('"');
                writer.write(dummyHeader);
                writer.write('"');
                if (cols.length > 0) writer.write(',');
            }
            for (String colName : cols) {
                writer.write('"');writer.write(colName);writer.write('"');
                writer.write(++i == cols.length ? '\n' : ',');
            }
        }
        // write rows
        for (int row=0; row < matrix.length; ++row) {
            final String rowName = rows[row];
            writer.write('"'); writer.write(rowName); writer.write('"');
            if (cols.length > 0) writer.write(',');
            int i=0;
            for (double cell : matrix[row]) {
                writer.write(formater.format(cell));
                writer.write(++i == cols.length ? '\n' : ',');
            }
        }
    }

    public static CSVMatrix read(InputStream in) throws IOException{
        final List<String> rowNames = new ArrayList<String>();
        final List<String> colNames = new ArrayList<String>();
        final List<double[]> rows = new ArrayList<double[]>();
        CSVReader.read(in, new CSVHandler() {
            private double[] vector;
            private int colSize;
            @Override
            public void entry(int row, int col, String entry) {
                if (row == 0) {
                    colNames.add(entry);    
                } else if (col == 0) {
                    rowNames.add(entry);
                } else {
                    vector[col-1] = Double.parseDouble(entry);
                }
            }

            @Override
            public void endOfRow(int row) {
                if (row == 0) {
                    colSize = colNames.size();
                    vector = new double[colSize];
                    Arrays.fill(vector, Double.NaN);
                    return;
                } else if (row == 1 && Double.isNaN(vector[colSize-1])) {
                    // csv table is matrix like
                    --colSize;
                    colNames.remove(0);
                    vector = Arrays.copyOf(vector, colSize);
                }
                rows.add(vector);
                vector = new double[colSize];
            }
        });
        final double[][] matrix = new double[rows.size()][];
        for (int i=0; i < matrix.length; ++i) {
            matrix[i] = rows.get(i);
        }
        return new CSVMatrix(rowNames.toArray(new String[0]), colNames.toArray(new String[0]), matrix);

    }

    public static CSVMatrix read(URL url) throws IOException {
        final InputStream stream = url.openStream();
        CSVMatrix matrix = null;
        try {
            matrix = read(stream);
        } finally {
            stream.close();
        }
        return matrix;
    }

}
