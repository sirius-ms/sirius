package de.unijena.bioinf.ChemistryBase.utils;

import com.google.common.base.Function;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.procedure.TObjectProcedure;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;

public class FileUtils {

    public static <T> List<T> mapLines(File file, Function<String, T> f) throws IOException {
        try (final BufferedReader br = getReader(file)) {
            return mapLines(br, f);
        }
    }
    public static <T> List<T> mapTable(File file, String separator, Function<String[], T> f) throws IOException {
        try (final BufferedReader br = getReader(file)) {
            return mapTable(br, separator, f);
        }
    }
    public static <T> List<T> mapTable(File file, Function<String[], T> f) throws IOException {
        try (final BufferedReader br = getReader(file)) {
            return mapTable(br, f);
        }
    }

    public static <T> List<T> mapLines(BufferedReader reader, Function<String, T> f) throws IOException {
        String line;
        final ArrayList<T> list = new ArrayList<>();
        while ((line=reader.readLine())!=null) {
            list.add(f.apply(line));
        }
        return list;
    }
    public static <T> List<T> mapTable(BufferedReader reader, String separator, Function<String[], T> f) throws IOException {
        String line;
        final ArrayList<T> list = new ArrayList<>();
        while ((line=reader.readLine())!=null) {
            list.add(f.apply(line.split(separator)));
        }
        return list;
    }
    public static <T> List<T> mapTable(BufferedReader reader, Function<String[], T> f) throws IOException {
        return mapTable(reader, "\t", f);
    }

    public static void eachLine(BufferedReader reader, TObjectProcedure<String> proc) throws IOException {
        String line;
        while ((line=reader.readLine())!=null) {
            if (!proc.execute(line))
                    break;
        }
    }
    public static void eachLine(File file, TObjectProcedure<String> proc) throws IOException {
        try (final BufferedReader br = getReader(file)) {
            eachLine(br, proc);
        }
    }

    public static void eachRow(BufferedReader reader, TObjectProcedure<String[]> proc) throws IOException {
        String line;
        while ((line=reader.readLine())!=null) {
            if (!proc.execute(line.split("\t")))
                break;
        }
    }
    public static void eachRow(File file, TObjectProcedure<String[]> proc) throws IOException {
        try (final BufferedReader br = getReader(file)) {
            eachRow(br, proc);
        }
    }

    public static String[][] readTable(File file, String sep) throws IOException {
        try (final BufferedReader br = getReader(file)) {
            return readTable(br, sep);
        }
    }
    public static String[][] readTable(File file, String sep,boolean skipHeader) throws IOException {
        try (final BufferedReader br = getReader(file)) {
            return readTable(br, sep,skipHeader);
        }
    }
    public static String[][] readTable(File file) throws IOException {
        try (final BufferedReader br = getReader(file)) {
            return readTable(br);
        }
    }
    public static String[][] readTable(File file, boolean skipHeader) throws IOException {
        try (final BufferedReader br = getReader(file)) {
            return readTable(br,"\t",skipHeader);
        }
    }

    public static String[] readLines(File file) throws IOException {
        try (final BufferedReader br = getReader(file)) {
            return readLines(br);
        }
    }

    public static String read(File file) throws IOException {
        final StringBuilder buffer = new StringBuilder(512);
        eachLine(file, new TObjectProcedure<String>() {
            @Override
            public boolean execute(String object) {
                buffer.append(object);
                buffer.append('\n');
                return true;
            }
        });
        return buffer.toString();
    }

    public static String[][] readTable(BufferedReader reader) throws IOException {
        return readTable(reader, "\t");
    }

    public static String[][] readTable(BufferedReader reader, String colSeparator, boolean skipHeader) throws IOException {
        String line;
        ArrayList<String[]> table = new ArrayList<>();
        if (skipHeader) reader.readLine();
        while ((line=reader.readLine())!=null) {
            table.add(line.split(colSeparator));
        }
        return table.toArray(new String[table.size()][]);
    }

    public static String[][] readTable(BufferedReader reader, String colSeparator) throws IOException {
       return readTable(reader,colSeparator,false);
    }

    public static String[] readLines(BufferedReader reader) throws IOException {
        String line;
        ArrayList<String> lines = new ArrayList<>();
        while ((line=reader.readLine())!=null) {
            lines.add(line);
        }
        return lines.toArray(new String[lines.size()]);
    }

    public static String read(BufferedReader reader) throws IOException {
        final StringBuilder buffer = new StringBuilder(512);
        eachLine(reader, new TObjectProcedure<String>() {
            @Override
            public boolean execute(String object) {
                buffer.append(object);
                buffer.append('\n');
                return true;
            }
        });
        return buffer.toString();
    }

    public static BufferedReader resource(Class<?> klass, String path) throws IOException {
        if (path.toLowerCase().endsWith(".gz")) {
            return new BufferedReader(new InputStreamReader(new GZIPInputStream(klass.getResourceAsStream(path), getRecommendetBufferSize())));
        } else {
            return new BufferedReader(new InputStreamReader(new BufferedInputStream(klass.getResourceAsStream(path), getRecommendetBufferSize())));
        }
    }

    public static float[][] readAsFloatMatrix(File file) throws IOException {
        try (final BufferedReader br = getReader(file)) {
            return readAsFloatMatrix(br);
        }
    }
    public static float[] readAsFloatVector(File file) throws IOException {
        try (final BufferedReader br = getReader(file)) {
            return readAsFloatVector(br);
        }
    }
    public static double[][] readAsDoubleMatrix(File file) throws IOException {
        try (final BufferedReader br = getReader(file)) {
            return readAsDoubleMatrix(br);
        }
    }
    public static double[] readAsDoubleVector(File file) throws IOException {
        try (final BufferedReader br = getReader(file)) {
            return readAsDoubleVector(br);
        }
    }

    public static void writeDoubleMatrix(File file, double[][] matrix) throws IOException {
        try (final BufferedWriter bw = getWriter(file)) {
            writeDoubleMatrix(bw, matrix);
        }
    }
    public static void writeDoubleVector(File file, double[] vector) throws IOException {
        try (final BufferedWriter bw = getWriter(file)) {
            writeDoubleVector(bw, vector);
        }
    }
    public static void writeFloatMatrix(File file, float[][] matrix) throws IOException {
        try (final BufferedWriter bw = getWriter(file)) {
            writeFloatMatrix(bw, matrix);
        }
    }
    public static void writeFloatVector(File file, float[] vector) throws IOException {
        try (final BufferedWriter bw = getWriter(file)) {
            writeFloatVector(bw, vector);
        }
    }
    public static void writeIntMatrix(File file, int[][] matrix) throws IOException {
        try (final BufferedWriter bw = getWriter(file)) {
            writeIntMatrix(bw, matrix);
        }
    }
    public static void writeIntVector(File file, int[] vector) throws IOException {
        try (final BufferedWriter bw = getWriter(file)) {
            writeIntVector(bw, vector);
        }
    }

    /*
        Shortcuts for getting a Stream or Reader/Writer. Automatically Compress/Decompress
     */
    public static BufferedOutputStream getOut(File file) throws IOException {
        if (file.getName().endsWith(".gz")) {
            return new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(file), getRecommendetBufferSize()), getRecommendetBufferSize());
        } else {
            return new BufferedOutputStream(new FileOutputStream(file), getRecommendetBufferSize());
        }
    }
    public static BufferedInputStream getIn(File file) throws IOException {
        if (file.getName().endsWith(".gz")) {
            return new BufferedInputStream(new GZIPInputStream(new FileInputStream(file), getRecommendetBufferSize()));
        } else {
            return new BufferedInputStream(new FileInputStream(file), getRecommendetBufferSize());
        }
    }

    public static BufferedWriter getWriter(File file) throws IOException {
        if (file.getName().endsWith(".gz")) {
            return new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(file),  getRecommendetBufferSize()), Charset.forName("UTF-8")));
        } else {
            return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF-8")), getRecommendetBufferSize());
        }
    }
    public static BufferedReader getReader(File file) throws IOException {
        if (file.getName().endsWith(".gz")) {
            return new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(file), getRecommendetBufferSize()), Charset.forName("UTF-8")));
        } else {
            return new BufferedReader(new InputStreamReader(new FileInputStream(file), Charset.forName("UTF-8")), getRecommendetBufferSize());
        }
    }

    public static BufferedReader ensureBuffering(Reader r) {
        if (r instanceof BufferedReader) return (BufferedReader)r;
        else return new BufferedReader(r, getRecommendetBufferSize());
    }
    public static InputStream ensureBuffering(InputStream r) {
        if (r instanceof BufferedInputStream || r instanceof GZIPInputStream || r instanceof InflaterInputStream)
            return r;
        else return new BufferedInputStream(r, getRecommendetBufferSize());
    }

    private static int getRecommendetBufferSize() {
        return 1024*1024*8;
    }

    /*
    Write numbers into files
     */

    public static float[][] readAsFloatMatrix(BufferedReader reader) throws IOException {
        String line;
        final TFloatArrayList values = new TFloatArrayList();
        ArrayList<float[]> rows = new ArrayList<float[]>();
        while ((line=reader.readLine())!=null) {
            int i = 0;
            while (i < line.length() && Character.isWhitespace(line.charAt(i)))
                ++i;
            if (i >= line.length() || line.charAt(i) == '#')
                continue;
            int n = i;
            for (; i < line.length(); ++i) {
                if (Character.isWhitespace(line.charAt(i))) {
                    final String token = line.substring(n, i);
                    values.add(Float.parseFloat(token));
                    n=i+1;
                }
            }
            if (n < line.length()) values.add(Float.parseFloat(line.substring(n,line.length())));
            if (values.size()==0) continue;
            rows.add(values.toArray());
            values.reset();
        }
        return rows.toArray(new float[rows.size()][]);
    }

    public static float[] readAsFloatVector(BufferedReader reader) throws IOException {
        String line;
        // skip comments
        while ((line=reader.readLine())!=null) {
            if (!line.isEmpty() && line.charAt(0)!='#') break;
        }
        if (line!= null && !line.isEmpty()) {
            String[] tabs = line.split("\\s+");
            if (tabs.length>1) {
                // we have a row vector
                final float[] vec = new float[tabs.length];
                for (int i=0; i < tabs.length; ++i)
                    vec[i] = Float.parseFloat(tabs[i]);
                return vec;
            } else {
                final TFloatArrayList buffer = new TFloatArrayList(128);
                buffer.add(Float.parseFloat(tabs[0]));
                // we have a col vector
                while ((line=reader.readLine())!=null) {
                    if (!line.isEmpty() && line.charAt(0)!='#') {
                        buffer.add(Float.parseFloat(line));
                    }
                }
                return buffer.toArray();
            }
        } else return new float[0];
    }

    public static double[][] readAsDoubleMatrix(BufferedReader reader) throws IOException {
        String line;
        final TDoubleArrayList values = new TDoubleArrayList();
        ArrayList<double[]> rows = new ArrayList<double[]>();
        while ((line=reader.readLine())!=null) {
            int i = 0;
            while (i < line.length() && Character.isWhitespace(line.charAt(i)))
                ++i;
            if (i >= line.length() || line.charAt(i) == '#')
                continue;
            int n = i;
            for (; i < line.length(); ++i) {
                if (Character.isWhitespace(line.charAt(i))) {
                    final String token = line.substring(n, i);
                    values.add(Double.parseDouble(token));
                    n=i+1;
                }
            }
            if (n < line.length()) values.add(Double.parseDouble(line.substring(n,line.length())));
            if (values.size()==0) continue;
            rows.add(values.toArray());
            values.reset();
        }
        return rows.toArray(new double[rows.size()][]);
    }

    public static double[] readAsDoubleVector(BufferedReader reader) throws IOException {
        String line;
        // skip comments
        while ((line=reader.readLine())!=null) {
            if (!line.isEmpty() && line.charAt(0)!='#') break;
        }
        if (line!= null && !line.isEmpty()) {
            String[] tabs = line.split("\\s+");
            if (tabs.length>1) {
                // we have a row vector
                final double[] vec = new double[tabs.length];
                for (int i=0; i < tabs.length; ++i)
                    vec[i] = Double.parseDouble(tabs[i]);
                return vec;
            } else {
                final TDoubleArrayList buffer = new TDoubleArrayList(128);
                buffer.add(Double.parseDouble(tabs[0]));
                // we have a col vector
                while ((line=reader.readLine())!=null) {
                    if (!line.isEmpty() && line.charAt(0)!='#') {
                        buffer.add(Double.parseDouble(line));
                    }
                }
                return buffer.toArray();
            }
        } else return new double[0];
    }

    public static int[][] readAsIntMatrix(BufferedReader reader) throws IOException {
        String line;
        final TIntArrayList values = new TIntArrayList();
        ArrayList<int[]> rows = new ArrayList<int[]>();
        while ((line=reader.readLine())!=null) {
            int i = 0;
            while (i < line.length() && Character.isWhitespace(line.charAt(i)))
                ++i;
            if (i >= line.length() || line.charAt(i) == '#')
                continue;
            int n = i;
            for (; i < line.length(); ++i) {
                if (Character.isWhitespace(line.charAt(i))) {
                    final String token = line.substring(n, i);
                    values.add(Integer.parseInt(token));
                    n=i+1;
                }
            }
            if (n < line.length()) values.add(Integer.parseInt(line.substring(n,line.length())));
            if (values.size()==0) continue;
            rows.add(values.toArray());
            values.reset();
        }
        return rows.toArray(new int[rows.size()][]);
    }

    public static int[] readAsIntVector(BufferedReader reader) throws IOException {
        String line;
        // skip comments
        while ((line=reader.readLine())!=null) {
            if (!line.isEmpty() && line.charAt(0)!='#') break;
        }
        if (line!= null && !line.isEmpty()) {
            String[] tabs = line.split("\\s+");
            if (tabs.length>1) {
                // we have a row vector
                final int[] vec = new int[tabs.length];
                for (int i=0; i < tabs.length; ++i)
                    vec[i] = Integer.parseInt(tabs[i]);
                return vec;
            } else {
                final TIntArrayList buffer = new TIntArrayList(128);
                buffer.add(Integer.parseInt(tabs[0]));
                // we have a col vector
                while ((line=reader.readLine())!=null) {
                    if (!line.isEmpty() && line.charAt(0)!='#') {
                        buffer.add(Integer.parseInt(line));
                    }
                }
                return buffer.toArray();
            }
        } else return new int[0];
    }


    public static void writeDoubleMatrix(Writer writer, double[][] matrix) throws IOException {
        for (double[] row : matrix) {
            writer.write(String.valueOf(row[0]));
            for (int k=1; k < row.length; ++k) {
                writer.write(' ');
                writer.write(String.valueOf(row[k]));
            }
            writer.write('\n');
        }
    }
    public static void writeDoubleVector(Writer writer, double[] vector) throws IOException {
        for (double value : vector) {
            writer.write(String.valueOf(value));
            writer.write('\n');
        }
    }

    public static void writeFloatMatrix(Writer writer, float[][] matrix) throws IOException {
        for (float[] row : matrix) {
            writer.write(String.valueOf(row[0]));
            for (int k=1; k < row.length; ++k) {
                writer.write(' ');
                writer.write(String.valueOf(row[k]));
            }
            writer.write('\n');
        }
    }
    public static void writeFloatVector(Writer writer, float[] vector) throws IOException {
        for (float value : vector) {
            writer.write(String.valueOf(value));
            writer.write('\n');
        }
    }

    public static void writeIntMatrix(Writer writer, int[][] matrix) throws IOException {
        for (int[] row : matrix) {
            writer.write(String.valueOf(row[0]));
            for (int k=1; k < row.length; ++k) {
                writer.write(' ');
                writer.write(String.valueOf(row[k]));
            }
            writer.write('\n');
        }
    }

    public static void writeIntVector(Writer writer, int[] vector) throws IOException {
        for (int value : vector) {
            writer.write(String.valueOf(value));
            writer.write('\n');
        }
    }

    public static Path newTempFile(@NotNull String directory, @NotNull String prefix, @NotNull String suffix) {
        return Paths.get(directory, MessageFormat.format("{0}{1}{2}", prefix, UUID.randomUUID(), suffix));
    }

    public static Path newTempFile(@NotNull String prefix, @NotNull String suffix) {
        return newTempFile(System.getProperty("java.io.tmpdir"), prefix, suffix);
    }

}
