package de.unijena.bioinf.fteval;

import au.com.bytecode.opencsv.CSVReader;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.TreeSet;

/**
 * Created by kaidu on 17.09.14.
 */
public class Standardize {

    private TreeSet<String> rowNames;
    private TreeSet<String> colNames;


    public Standardize() {
        this.rowNames = new TreeSet<String>();
        this.colNames = new TreeSet<String>();
    }

    public void merge(File f) throws IOException {
        if (rowNames.isEmpty()) mergeByUnion(f);
        else mergeByIntersection(f);
    }

    public void mergeByUnion(File f) throws IOException {
        final Iterator<String[]> iter = parseMatrix(f);
        final String[] header = iter.next();
        String[] line = iter.next();
        for (int k = (line.length == header.length ? 1 : 0); k < header.length; ++k) colNames.add(header[k]);
        while (true) {
            rowNames.add(line[0]);
            if (iter.hasNext()) line = iter.next();
            else break;
        }
    }

    public void mergeByIntersection(File f) throws IOException {
        TreeSet<String> rowNames2 = new TreeSet<String>();
        TreeSet<String> colNames2 = new TreeSet<String>();
        final Iterator<String[]> iter = parseMatrix(f);
        final String[] header = iter.next();
        String[] line = iter.next();
        for (int k = (line.length == header.length ? 1 : 0); k < header.length; ++k) colNames2.add(header[k]);
        while (true) {
            rowNames2.add(line[0]);
            if (iter.hasNext()) line = iter.next();
            else break;
        }
        rowNames.retainAll(rowNames2);
        colNames.retainAll(colNames2);
    }

    public String[][] reorderFile(File f) throws IOException {
        final TObjectIntHashMap<String> rowIndizes = new TObjectIntHashMap<String>(rowNames.size(), 0.75f, -1);
        {
            int i = 1;
            for (String s : rowNames) rowIndizes.put(s, i++);
        }
        final TObjectIntHashMap<String> colIndizes = new TObjectIntHashMap<String>(colNames.size(), 0.75f, -1);
        {
            int i = 1;
            for (String s : colNames) colIndizes.put(s, i++);
        }
        String[][] matrix = parseMatrixComplete(f);
        final int[] rowMapping = new int[matrix.length];
        final int[] colMapping = new int[matrix[0].length];
        final String[] header = matrix[0];
        int k = (matrix[1].length == matrix[0].length) ? 1 : 0;
        for (int i = k; i < header.length; ++i) {
            colMapping[i] = colIndizes.get(header[i]);
        }
        for (int i = 1; i < matrix.length; ++i) {
            rowMapping[i] = rowIndizes.get(matrix[i][0]);
        }
        final String[][] newMatrix = new String[rowNames.size()][colNames.size()];
        // reorder rows
        for (int i = 0; i < matrix.length; ++i) {
            if (rowMapping[i] < 0) continue;
            newMatrix[rowMapping[i]] = matrix[i];
        }
        matrix = null;
        // reorder cols
        final String[] buffer = new String[newMatrix[1].length];
        for (int i = 1; i < newMatrix.length; ++i) {
            final String[] row = newMatrix[i];
            for (int j = 0; j < colMapping.length; ++j) {
                if (colMapping[i] < 0) continue;
                buffer[colMapping[j]] = row[j];
            }
            newMatrix[i] = buffer.clone();
        }
        return newMatrix;
    }

    private static String[][] parseMatrixComplete(File file) throws IOException {
        final Iterator<String[]> iter = parseMatrix(file);
        final ArrayList<String[]> lines = new ArrayList<String[]>();
        while (iter.hasNext()) lines.add(iter.next());
        return lines.toArray(new String[lines.size()][]);
    }

    private static Iterator<String[]> parseMatrix(File file) throws IOException {
        final BufferedReader r = new BufferedReader(new FileReader(file));
        final CSVReader reader = new CSVReader(r);
        return new Iterator<String[]>() {
            String[] row = reader.readNext();

            @Override
            public boolean hasNext() {
                return row != null;
            }

            @Override
            public String[] next() {
                if (hasNext()) {
                    final String[] r = row;
                    readNext();
                    return r;
                } else throw new NoSuchElementException();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            private void readNext() {
                try {
                    row = reader.readNext();
                    if (row == null) reader.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };

    }

}
