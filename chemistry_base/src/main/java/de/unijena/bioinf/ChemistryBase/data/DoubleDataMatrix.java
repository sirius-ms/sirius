
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

package de.unijena.bioinf.ChemistryBase.data;

import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.procedure.TObjectIntProcedure;

import java.util.*;

/**
 * A DoubleDataMatrix is a matrix of double values with row and col names. It is considered to be used as distance
 * or similarity matrix and can be parsed from a csv or excel file.
 * This class does not handle the parsing of the table structure itself, but get an Iterator of String arrays (=rows).
 * So each parser that returns collections of string-arrays or iterators of string-arrays can be used in combination
 * with this class.
 *
 * The main idea is, that you have some csv files containing some similarity/distance data. You want to merge
 * this files into a single matrix. Merging can be done in two ways:
 * - if all files have different row and col names you may want to merge them in one big matrix. For this case use {{@link #merge()}}
 * - if all files have same/similar row and col names you may want to overlay them such that you get a 3D matrix. For
 *   this case use {{@link #overlay(java.util.List, java.util.List, java.util.List, java.util.List, double)}}
 */
public class DoubleDataMatrix {

    private final double[][][] values;
    private final String[] rowHeader, colHeader, layerHeader;

    public interface NameNormalizer {
        String normalize(String name);
    }
    public static DoubleDataMatrix merge() {
        throw new RuntimeException("Not implemented yet");
    }

    /**
     * Input: A set of 2D matrices
     * Output: A 3D matrix.
     *
     * If your 2D matrices contains exactly the same row and col names, this task is easy. If they have some different
     * row/col names, you either want to fill the missing values with e.g. 0 or NaN, or to skip this cells completely.
     *
     * Sometimes your row/col names differ only in some consistent detail. In this case you can give a list of
     * name normalizers that map your names into another names that are equal over all matrices.
     *
     * @param templateTables contains matrices that rows/cols have to be inserted into the output matrix. So they are merged
     * @param additionalTables contains matrices that rows/cols are only inserted, if they are also contained in the template tables
     * @param nameNormalizers
     * @return
     */

    public static DoubleDataMatrix overlay(List<Iterator<String[]>> templateTables, List<Iterator<String[]>> additionalTables, List<String> names, List<NameNormalizer> nameNormalizers, double neutralElement) {

        if (templateTables==null) templateTables = Collections.emptyList();
        if (additionalTables==null) additionalTables = Collections.emptyList();
        final NameNormalizer[] norms = new NameNormalizer[templateTables.size()+additionalTables.size()];
        if (nameNormalizers != null) {
            for (int i=0; i < nameNormalizers.size(); ++i) norms[i] = nameNormalizers.get(i);
        }
        final int N = templateTables.size()+additionalTables.size();

        final TObjectIntHashMap<String> rowNames = new TObjectIntHashMap<String>(40, 0.75f, -1);
        final TObjectIntHashMap<String> colNames = new TObjectIntHashMap<String>(40, 0.5f, -1);
        final TreeMap<Integer, double[]>[] rowBuffers = new TreeMap[N];
        for (int i=0; i < N; ++i) rowBuffers[i] = new TreeMap<Integer, double[]>();
        extractTemplateTables(templateTables, neutralElement, norms, rowNames, colNames, rowBuffers);
        final double[][][] matrix = new double[N][rowNames.size()][colNames.size()];
        for (int K = 0; K < templateTables.size(); ++K) {
            fillTable(neutralElement, rowNames, rowBuffers[K], matrix[K]);
        }
        extractAdditionalTables(templateTables, additionalTables, neutralElement, norms, rowNames, colNames, rowBuffers);
        for (int L = 0; L < additionalTables.size(); ++L) {
            fillTable(neutralElement, rowNames, rowBuffers[templateTables.size()+L], matrix[templateTables.size()+L]);
        }
        final String[] rowNameArray = new String[rowNames.size()];
        final String[] colNameArray = new String[colNames.size()];
        rowNames.forEachEntry(new TObjectIntProcedure<String>() {
            @Override
            public boolean execute(String a, int b) {
                rowNameArray[b] = a;
                return true;
            }
        });
        colNames.forEachEntry(new TObjectIntProcedure<String>() {
            @Override
            public boolean execute(String a, int b) {
                colNameArray[b] = a;
                return true;
            }
        });
        return new DoubleDataMatrix(matrix, rowNameArray, colNameArray, names.toArray(new String[N]));
    }

    private static class Tab {
        TreeMap<String, double[]> rows;
        String[] colNames;
        Tab(Iterator<String[]> iter, NameNormalizer norm) {
            String[] header = iter.next();
            if (!iter.hasNext()) {
                // empty table
                colNames = new String[0];
                rows = new TreeMap<String, double[]>();
            } else {
                rows = new TreeMap<String, double[]>();
                String[] row = iter.next();
                // parse HeaderBuilder
                if (header.length == row.length) {
                    // first element in header is placeholder
                    colNames = new String[row.length-1];
                    if (norm != null)
                        for (int i=1; i < header.length; ++i) colNames[i] = norm.normalize(header[i]);
                    else System.arraycopy(header, 1, colNames, 0, colNames.length);
                } else if (header.length == row.length-1) {
                    // header contains only col names
                    if (norm != null) {
                        colNames = new String[header.length];
                        for (int i=0; i < header.length; ++i) colNames[i] = norm.normalize(header[i]);
                    } else {
                        colNames = Arrays.copyOf(header, header.length);
                    }
                } else {
                    throw new IllegalArgumentException("Row size differs");
                }
                // parse rows
                while (row != null) {
                    String rowName = norm != null ? norm.normalize(row[0]) : row[0];
                    double[] rowValues = new double[row.length-1];
                    for (int i=1; i < row.length; ++i) rowValues[i-1] = Double.parseDouble(row[i]);
                    rows.put(rowName, rowValues);
                    row = iter.hasNext() ? iter.next() : null;
                }
            }
        }
    }

    public static DoubleDataMatrix overlayIntersection(List<Iterator<String[]>> tables, List<String> names, List<NameNormalizer> norms) {
        final TObjectIntHashMap<String> rowNames = new TObjectIntHashMap<String>(40, 0.75f, -1);
        final TObjectIntHashMap<String> colNames = new TObjectIntHashMap<String>(40, 0.5f, -1);

        final List<Tab> matrices = new ArrayList<Tab>();
        for (int i=0; i < tables.size(); ++i) {
            final NameNormalizer n = (norms == null || norms.size() <= i) ? null : norms.get(i);
            matrices.add(new Tab(tables.get(i), n));
        }

        // intersect matrices
        final TreeSet<String> colIntersections = new TreeSet<String>();
        final TreeSet<String> rowIntersections = new TreeSet<String>();

        final Iterator<Tab> matrixIter = matrices.iterator();

        Tab t = matrixIter.next();
        colIntersections.addAll(Arrays.asList(t.colNames));
        rowIntersections.addAll(t.rows.keySet());

        while (matrixIter.hasNext()) {
            t = matrixIter.next();
            colIntersections.retainAll(new TreeSet<String>(Arrays.asList(t.colNames))); // stupid java collection api -_-
            rowIntersections.retainAll(t.rows.keySet());
        }

        // rearrange tables

        final double[][][] matrix = new double[tables.size()][rowIntersections.size()][colIntersections.size()];

        int k1=0;
        for (String s : rowIntersections) rowNames.put(s, k1++);
        int k2=0;
        for (String s : colIntersections) colNames.put(s, k2++);

        final int[] from = new int[colNames.size()];
        final int[] to = new int[colNames.size()];

        for (int T = 0; T < tables.size(); ++T) {
            final Tab table = matrices.get(T);
            int K=0;
            for (int col=0; col < table.colNames.length; ++col) {
                if (colIntersections.contains(table.colNames[col])) {
                    from[K] = col;
                    to[K] = colNames.get(table.colNames[col]);
                    ++K;
                }
            }
            final double[][] M = matrix[T];
            rowNames.forEachEntry(new TObjectIntProcedure<String>() {
                @Override
                public boolean execute(String a, int b) {
                    final double[] source = table.rows.get(a);
                    final double[] target = M[b];
                    for (int K=0; K < from.length; ++K) {
                        target[to[K]] = source[from[K]];
                    }
                    return true;
                }
            });
        }

        final String[] finalRowNames = new String[rowNames.size()];
        final String[] finalColNames = new String[colNames.size()];
        rowNames.forEachEntry(new TObjectIntProcedure<String>() {
            @Override
            public boolean execute(String a, int b) {
                finalRowNames[b] = a;
                return true;
            }
        });
        colNames.forEachEntry(new TObjectIntProcedure<String>() {
            @Override
            public boolean execute(String a, int b) {
                finalColNames[b] = a;
                return true;
            }
        });



        return new DoubleDataMatrix(matrix, finalRowNames, finalColNames, names.toArray(new String[tables.size()]));
    }

    private static void extractAdditionalTables(List<Iterator<String[]>> templateTables, List<Iterator<String[]>> additionalTables, double neutralElement, NameNormalizer[] norms, TObjectIntHashMap<String> rowNames, TObjectIntHashMap<String> colNames, TreeMap<Integer, double[]>[] rowBuffers) {
        for (int L = 0; L < additionalTables.size(); ++L) {
            final int K = templateTables.size()+L;
            final Iterator<String[]> table = additionalTables.get(L);
            if (!table.hasNext()) continue;
            final String[] header = table.next();
            if (!table.hasNext()) continue;
            String[] row = table.next();
            final int start;
            if (row.length == header.length) {
                // first col in header row is a placeholder
                start = 1;
            } else {
                start = 0;
            }
            int[] indexMapper = new int[header.length];
            for (int i=start; i < header.length; ++i) {
                final String name = norms[K]==null ? header[i] : norms[K].normalize(header[i]);
                final Integer j = colNames.get(name);
                indexMapper[i-start] = (j==null) ? -1 : j;
            }
            while(true) {
                final String rowName = row[0];
                final Integer rowIndex = rowNames.get(rowName);
                if (rowIndex == null) continue;
                final double[] currentRow = new double[colNames.size()];
                if (neutralElement != 0d) Arrays.fill(currentRow, neutralElement);
                for (int i=1; i < row.length; ++i) {
                    if (indexMapper[i-1] >= 0)
                        currentRow[indexMapper[i-1]] = Double.parseDouble(row[i]);
                }
                rowBuffers[K].put(rowIndex, currentRow);
                if (table.hasNext()) row = table.next();
                else break;
            }
        }
    }

    private static void extractTemplateTables(List<Iterator<String[]>> templateTables, double neutralElement, NameNormalizer[] norms, TObjectIntHashMap<String> rowNames, TObjectIntHashMap<String> colNames, TreeMap<Integer, double[]>[] rowBuffers) {
        int rowIndizes = 0;
        int colIndizes = 0;
        for (int K = 0; K < templateTables.size(); ++K) {
            final Iterator<String[]> table = templateTables.get(K);
            if (!table.hasNext()) continue;
            final String[] header = table.next();
            if (!table.hasNext()) continue;
            String[] row = table.next();
            final int start;
            if (row.length == header.length) {
                // first col in header row is a placeholder
                start = 1;
            } else {
                start = 0;
            }
            int[] indexMapper = new int[header.length];
            for (int i=start; i < header.length; ++i) {
                final String name = norms[K]==null ? header[i] : norms[K].normalize(header[i]);
                int j = colNames.putIfAbsent(name, colIndizes);
                if (j < 0) j = colIndizes++;
                indexMapper[i-start] = j;
            }
            while(true) {
                final String rowName = row[0];
                int rowIndex = rowNames.putIfAbsent(rowName, rowIndizes);
                if (rowIndex < 0) rowIndex = rowIndizes++;
                final double[] currentRow = new double[colNames.size()];
                if (neutralElement != 0d) Arrays.fill(currentRow, neutralElement);
                for (int i=1; i < row.length; ++i) {
                    currentRow[indexMapper[i-1]] = Double.parseDouble(row[i]);
                }
                rowBuffers[K].put(rowIndex, currentRow);
                if (table.hasNext()) row = table.next();
                else break;
            }
        }
    }

    private static void fillTable(double neutralElement, TObjectIntHashMap<String> rowNames, TreeMap<Integer, double[]> rowBuffer, double[][] doubles) {
        final double[] EMPTY = new double[0];
        final double[][] table = doubles;
        for (int row=0; row < rowNames.size(); ++row) {
            double[] buf = rowBuffer.get(row);
            if (buf == null) buf = EMPTY;
            final double[] dst = table[row];
            System.arraycopy(buf, 0, dst, 0, buf.length);
            if (neutralElement != 0d) Arrays.fill(dst, buf.length, dst.length, neutralElement);
        }
    }


    protected DoubleDataMatrix(double[][][] values, String[] rowHeader, String[] colHeader, String[] layerHeader) {
        this.values = values;
        this.rowHeader = rowHeader;
        this.colHeader = colHeader;
        this.layerHeader = layerHeader;
    }

    public double[] getXY(int row, int col) {
        return getXY(row, col, new double[values.length]);
    }

    public double[] getXY(int row, int col, double[] buf) {
        for (int i=0; i < values.length; ++i) {
            buf[i] = values[i][row][col];
        }
        return buf;
    }

    public double[][] getLayer(int layer) {
        return values[layer];
    }

    public double[][] getLayer(String name) {
        for (int l=0; l < layerHeader.length; ++l)
            if (layerHeader[l].equals(name)) return getLayer(l);
        return null;
    }

    public double[][][] getValues() {
        return values;
    }

    public String[] getRowHeader() {
        return rowHeader;
    }

    public String[] getColHeader() {
        return colHeader;
    }

    public String[] getLayerHeader() {
        return layerHeader;
    }
}
