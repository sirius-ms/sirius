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

package de.unijena.bioinf.ChemistryBase.utils;

import com.github.f4b6a3.tsid.TsidCreator;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.procedure.TObjectProcedure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.*;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class FileUtils {

    public static String sanitizeFilename(String inputName) {
        return inputName.replaceAll("[^a-zA-Z0-9-_.]", "_");
    }


    public static String getFileName(URI path){
        if (path == null)
            return null;
        return getFileName(path.getPath());
    }
    public static String getFileName(String path){
        return path.replaceAll("\\\\","/").substring(path.lastIndexOf("/") + 1);
    }
    public static long getFolderSizeOrThrow(Path startPath) {
        try {
            return getFolderSize(startPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static long getFolderSize(Path startPath) throws IOException {
        final AtomicLong size = new AtomicLong(0);

        Files.walkFileTree(startPath, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                size.addAndGet(attrs.size());
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                // Skip folders that can't be traversed
                LoggerFactory.getLogger(FileUtils.class).warn("skipped: " + file, exc);
                return FileVisitResult.CONTINUE;
            }
        });

        return size.get();
    }

    public static void closeIfNotDefaultFS(Path zipFS) throws IOException {
        final FileSystem fs = zipFS.getFileSystem();
        if (!fs.equals(FileSystems.getDefault()) && fs.isOpen())
            fs.close();
    }

    public static boolean isZipArchive(Path f) throws IOException {
        if (!Files.isRegularFile(f))
            return false;
        int fileSignature;

        try (DataInputStream raf = new DataInputStream(Files.newInputStream(f, StandardOpenOption.READ))) {
            if (raf.available() < 4)
                return false;
            fileSignature = raf.readInt();
        }
        return fileSignature == 0x504B0304 || fileSignature == 0x504B0506 || fileSignature == 0x504B0708;
    }


    public static FileSystem asZipFS(Path zipFile, boolean createNew, boolean useTempFile, @Nullable ZipCompressionMethod method) throws IOException {
        final Map<String, Object> option = new HashMap<>();
        option.put("useTempFile", useTempFile);
        option.put("forceZIP64End", "true");
        option.put("compressionMethod", method == null ? ZipCompressionMethod.DEFLATED.name() : method.name());
        if (createNew){
            if (zipFile.getParent() != null)
                Files.createDirectories(zipFile.getParent());
            option.put("create", "true");
        }
        return FileSystems.newFileSystem(zipFile, option);
    }

    public static Path asZipFSPath(Path zipFile, boolean createNew, boolean useTempFile, @Nullable ZipCompressionMethod method) throws IOException {
        FileSystem zipFS = asZipFS(zipFile, createNew, useTempFile, method);
        return zipFS.getPath(zipFS.getSeparator());
    }

    /**
     * @param folder
     * @param zipFilePath
     * @return new Created zip Files
     * @throws IOException if zip file compression fails
     */
    public static Path zipDir(final Path folder, final Path zipFilePath) throws IOException {
        try (
                FileOutputStream fos = new FileOutputStream(zipFilePath.toFile());
                ZipOutputStream zos = new ZipOutputStream(fos)
        ) {
            Files.walkFileTree(folder, new SimpleFileVisitor<>() {
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    zos.putNextEntry(new ZipEntry(folder.relativize(file).toString()));
                    Files.copy(file, zos);
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }

                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    zos.putNextEntry(new ZipEntry(folder.relativize(dir).toString() + "/"));
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
            });
            return zipFilePath;
        }
    }

    /**
     * @param zipFile
     * @param target
     * @return Target directory with unzipped data
     * @throws IOException if extraction fails
     */
    public static Path unZipDir(final Path zipFile, final Path target) throws IOException {
        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                final Path toPath = target.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectory(toPath);
                } else {
                    Files.copy(zipInputStream, toPath);
                }
            }
        }
        return target;
    }

    /**
     * Copies a File Tree recursively to another location.
     * Src and dest might be different Filesystems (e.g. mounted ZipFile)
     * <p>
     * Note: The target directory must already exist.
     *
     * @param src  Source location
     * @param dest Target location
     * @throws IOException if I/O Error occurs
     */


    public static void copyFolder(Path src, Path dest) throws IOException {
        if (Files.notExists(dest))
            throw new IllegalArgumentException("Root destination dir/file must exist!");

        List<Path> files = walkAndClose(w -> w.collect(Collectors.toList()), src);
        for (Path source : files) {
            String relative = src.relativize(source).toString();
            final Path target = dest.resolve(relative);
            if (!target.equals(dest) && !target.equals(target.getFileSystem().getPath("/"))) //exclude root to be zipFS compatible
                Files.copy(source, target, REPLACE_EXISTING);
        }
    }

    /**
     * Lazy move operation. If move not possible files will be copied but source will not be deleted
     *
     * @param src  Source location
     * @param dest Target location
     * @throws IOException if I/O Error occurs
     */
    public static boolean moveFolder(Path src, Path dest) throws IOException {
        if (src.getFileSystem().provider() == dest.getFileSystem().provider()) {
            Files.move(src, dest, REPLACE_EXISTING);
            return true;
        } else {
            copyFolder(src, dest);
            return false;
        }
    }

    public static void deleteDirContentRecursively(Path rootPath) throws IOException {
        deleteRecursively(rootPath, true);
    }

    public static void deleteRecursively(Path rootPath) throws IOException {
        deleteRecursively(rootPath, false);
    }

    public static void deleteRecursively(Path rootPath, boolean keepRoot) throws IOException {
        if (Files.notExists(rootPath))
            return;
        if (Files.isRegularFile(rootPath)) {
            if (keepRoot)
                return;
            Files.deleteIfExists(rootPath);
        } else {
            List<Path> files = walkAndClose(w -> w.sorted(Comparator.reverseOrder()).collect(Collectors.toList()), rootPath);
            for (Path file : files)
                if (!keepRoot || !file.equals(rootPath))
                    Files.deleteIfExists(file);
        }
    }


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
        while ((line = reader.readLine()) != null) {
            list.add(f.apply(line));
        }
        return list;
    }

    public static <T> List<T> mapTable(BufferedReader reader, String separator, Function<String[], T> f) throws IOException {
        String line;
        final ArrayList<T> list = new ArrayList<>();
        while ((line = reader.readLine()) != null) {
            list.add(f.apply(line.split(separator, -1)));
        }
        return list;
    }

    public static <T> List<T> mapTable(BufferedReader reader, Function<String[], T> f) throws IOException {
        return mapTable(reader, "\t", f);
    }

    public static void eachLine(BufferedReader reader, TObjectProcedure<String> proc) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
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
        while ((line = reader.readLine()) != null) {
            if (!proc.execute(line.split("\t", -1)))
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

    public static String[][] readTable(File file, String sep, boolean skipHeader) throws IOException {
        try (final BufferedReader br = getReader(file)) {
            return readTable(br, sep, skipHeader);
        }
    }

    public static String[][] readTable(File file) throws IOException {
        try (final BufferedReader br = getReader(file)) {
            return readTable(br);
        }
    }

    public static String[][] readTable(File file, boolean skipHeader) throws IOException {
        try (final BufferedReader br = getReader(file)) {
            return readTable(br, "\t", skipHeader);
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
        while ((line = reader.readLine()) != null) {
            table.add(line.split(colSeparator, -1));
        }
        return table.toArray(new String[table.size()][]);
    }

    public static String[][] readTable(BufferedReader reader, String colSeparator) throws IOException {
        return readTable(reader, colSeparator, false);
    }

    public static String[] readLines(BufferedReader reader) throws IOException {
        String line;
        ArrayList<String> lines = new ArrayList<>();
        while ((line = reader.readLine()) != null) {
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
            return new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(file), getRecommendetBufferSize()), Charset.forName("UTF-8")));
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
        if (r instanceof BufferedReader) return (BufferedReader) r;
        else return new BufferedReader(r, getRecommendetBufferSize());
    }

    public static InputStream ensureBuffering(InputStream r) {
        if (r instanceof BufferedInputStream || r instanceof GZIPInputStream || r instanceof InflaterInputStream)
            return r;
        else return new BufferedInputStream(r, getRecommendetBufferSize());
    }

    private static int getRecommendetBufferSize() {
        return 1024 * 1024 * 8;
    }

    /*
    Write numbers into files
     */

    public static float[][] readAsFloatMatrix(BufferedReader reader) throws IOException {
        String line;
        final TFloatArrayList values = new TFloatArrayList();
        ArrayList<float[]> rows = new ArrayList<float[]>();
        while ((line = reader.readLine()) != null) {
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
                    n = i + 1;
                }
            }
            if (n < line.length()) values.add(Float.parseFloat(line.substring(n, line.length())));
            if (values.size() == 0) continue;
            rows.add(values.toArray());
            values.clear();
        }
        return rows.toArray(new float[rows.size()][]);
    }

    public static float[] readAsFloatVector(BufferedReader reader) throws IOException {
        String line;
        // skip comments
        while ((line = reader.readLine()) != null) {
            if (!line.isEmpty() && line.charAt(0) != '#') break;
        }
        if (line != null && !line.isEmpty()) {
            String[] tabs = line.split("\\s+");
            if (tabs.length > 1) {
                // we have a row vector
                final float[] vec = new float[tabs.length];
                for (int i = 0; i < tabs.length; ++i)
                    vec[i] = Float.parseFloat(tabs[i]);
                return vec;
            } else {
                final TFloatArrayList buffer = new TFloatArrayList(128);
                buffer.add(Float.parseFloat(tabs[0]));
                // we have a col vector
                while ((line = reader.readLine()) != null) {
                    if (!line.isEmpty() && line.charAt(0) != '#') {
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
        while ((line = reader.readLine()) != null) {
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
                    n = i + 1;
                }
            }
            if (n < line.length()) values.add(Double.parseDouble(line.substring(n, line.length())));
            if (values.size() == 0) continue;
            rows.add(values.toArray());
            values.clear();
        }
        return rows.toArray(new double[rows.size()][]);
    }

    public static double[] readAsDoubleVector(BufferedReader reader) throws IOException {
        String line;
        // skip comments
        while ((line = reader.readLine()) != null) {
            if (!line.isEmpty() && line.charAt(0) != '#') break;
        }
        if (line != null && !line.isEmpty()) {
            String[] tabs = line.split("\\s+");
            if (tabs.length > 1) {
                // we have a row vector
                final double[] vec = new double[tabs.length];
                for (int i = 0; i < tabs.length; ++i)
                    vec[i] = Double.parseDouble(tabs[i]);
                return vec;
            } else {
                final TDoubleArrayList buffer = new TDoubleArrayList(128);
                buffer.add(Double.parseDouble(tabs[0]));
                // we have a col vector
                while ((line = reader.readLine()) != null) {
                    if (!line.isEmpty() && line.charAt(0) != '#') {
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
        while ((line = reader.readLine()) != null) {
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
                    n = i + 1;
                }
            }
            if (n < line.length()) values.add(Integer.parseInt(line.substring(n, line.length())));
            if (values.size() == 0) continue;
            rows.add(values.toArray());
            values.clear();
        }
        return rows.toArray(new int[rows.size()][]);
    }

    public static int[] readAsIntVector(BufferedReader reader) throws IOException {
        String line;
        // skip comments
        while ((line = reader.readLine()) != null) {
            if (!line.isEmpty() && line.charAt(0) != '#') break;
        }
        if (line != null && !line.isEmpty()) {
            String[] tabs = line.split("\\s+");
            if (tabs.length > 1) {
                // we have a row vector
                final int[] vec = new int[tabs.length];
                for (int i = 0; i < tabs.length; ++i)
                    vec[i] = Integer.parseInt(tabs[i]);
                return vec;
            } else {
                final TIntArrayList buffer = new TIntArrayList(128);
                buffer.add(Integer.parseInt(tabs[0]));
                // we have a col vector
                while ((line = reader.readLine()) != null) {
                    if (!line.isEmpty() && line.charAt(0) != '#') {
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
            for (int k = 1; k < row.length; ++k) {
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
            for (int k = 1; k < row.length; ++k) {
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
            for (int k = 1; k < row.length; ++k) {
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

    public static void writeKeyValues(Writer stream, Map<?, ?> map) throws IOException {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            stream.write(String.valueOf(entry.getKey()));
            stream.write('\t');
            stream.write(String.valueOf(entry.getValue()));
            stream.write('\n');
        }
    }

    public static void writeTable(Writer w, @Nullable String[] header, Iterable<String[]> rows) throws IOException {
        if (header != null) {
            w.write(String.join("\t", header));
            w.write(System.lineSeparator());
        }
        for (String[] row : rows) {
            w.write(String.join("\t", row));
            w.write(System.lineSeparator());
        }
    }

    public static Map<String, String> readKeyValues(Path path) throws IOException {
        try (final BufferedReader br = Files.newBufferedReader(path)) {
            return readKeyValues(br);
        }
    }

    public static Map<String, String> readKeyValues(File file) throws IOException {
        try (final BufferedReader br = getReader(file)) {
            return readKeyValues(br);
        }
    }

    public static Map<String, String> readKeyValues(BufferedReader reader) throws IOException {
        final HashMap<String, String> keyValues = new HashMap<>();
        String line;
        while ((line = reader.readLine()) != null) {
            String[] kv = line.split("\\s+", 2);
            keyValues.put(kv[0], kv[1]);
        }
        return keyValues;
    }

    public static void readTable(BufferedReader br, boolean skipHeader, Consumer<String[]> f) throws IOException {
        readTable(br, skipHeader, 0, -1, f);
    }

    public static void readTable(BufferedReader br, boolean skipHeader, int fromLineInkl, int toLineExcl, Consumer<String[]> f) throws IOException {
        fromLineInkl = Math.max(0, fromLineInkl);

        String line;
        if (skipHeader)
            br.readLine();

        br.skip(fromLineInkl);

        int readCount = toLineExcl - fromLineInkl;

        while ((line = br.readLine()) != null && (toLineExcl < 0 || readCount-- > 0))
            f.accept(line.split("\t", -1));
    }

    /**
     * read the first nlines lines from file. Keep buffersize low.
     * If less than nlines lines exist in file, fill them with empty strings
     */
    public static String[] head(File file, int nlines) throws IOException {
        String[] lines = new String[nlines];
        int k = 0;
        try (final BufferedReader br = new BufferedReader(new FileReader(file), 40 * nlines)) {
            while (k < nlines) {
                String l = lines[k++] = br.readLine();
                if (l == null) {
                    Arrays.fill(lines, k, lines.length, "");
                    return lines;
                }
            }
        }
        return lines;
    }

    public static Path createTmpProjectSpaceLocation(@Nullable String ext) {
        return createTmpProjectSpaceLocation(ext, FileSystems.getDefault());
    }

    public static Path createTmpProjectSpaceLocation(@Nullable String ext, @NotNull FileSystem fs) {
        String tmpDir = System.getProperty("java.io.tmpdir");
        return fs.getPath(tmpDir).resolve("sirius-tmp-project-" + TsidCreator.getTsid() + (ext == null || ext.isBlank() ? "": (ext.startsWith(".") ? ext : ("." + ext))));
    }

    public static Path newTempFile(@NotNull String directory, @NotNull String prefix, @NotNull String suffix) {
        return newTempFile(directory, prefix, suffix, FileSystems.getDefault());
    }
    public static Path newTempFile(@NotNull String directory, @NotNull String prefix, @NotNull String suffix, @NotNull FileSystem fs) {
        return fs.getPath(directory, MessageFormat.format("{0}{1}{2}", prefix, TsidCreator.getTsid(), suffix));
    }

    public static Path newTempFile(@NotNull String prefix, @NotNull String suffix) {
        return newTempFile(prefix,suffix,FileSystems.getDefault());
    }

    public static Path newTempFile(@NotNull String prefix, @NotNull String suffix, @NotNull FileSystem fs) {
        return newTempFile(System.getProperty("java.io.tmpdir"), prefix, suffix, fs);
    }

    public static <R> R listAndClose(Path p, Function<Stream<Path>, R> tryWith) throws IOException {
        try (Stream<Path> s = Files.list(p)) {
            return tryWith.apply(s);
        }
    }

    public static <R> R findAndClose(Function<Stream<Path>, R> tryWith, Path p, int maxDepth,
                                     BiPredicate<Path, BasicFileAttributes> matcher,
                                     FileVisitOption... options) throws IOException {
        try (Stream<Path> s = Files.find(p, maxDepth, matcher, options)) {
            return tryWith.apply(s);
        }
    }

    public static <R> R walkAndClose(Function<Stream<Path>, R> tryWith, Path p, FileVisitOption... options) throws IOException {
        return walkAndClose(tryWith, p, null, options);
    }

    // If the parameter does not take the form: syntax:pattern
    public static <R> R walkAndClose(Function<Stream<Path>, R> tryWith, Path p, @Nullable String globOrRegex, FileVisitOption... options) throws IOException {
        try (Stream<Path> s = Files.walk(p, options)) {
            if (globOrRegex != null && !globOrRegex.equals("glob:*")) {
                final PathMatcher pathMatcher = p.getFileSystem().getPathMatcher(globOrRegex);
                return tryWith.apply(s.filter(pathMatcher::matches));
            }
            return tryWith.apply(s);
        }
    }

    public static <R> R walkAndClose(Function<Stream<Path>, R> tryWith, Path p, int maxDepth, FileVisitOption... options) throws IOException {
        return walkAndClose(tryWith, p, maxDepth, null, options);

    }

    // If the parameter does not take the form: syntax:pattern
    public static <R> R walkAndClose(Function<Stream<Path>, R> tryWith, Path p, int maxDepth, @Nullable String globOrRegex, FileVisitOption... options) throws IOException {
        try (Stream<Path> s = Files.walk(p, maxDepth, options)) {
            if (globOrRegex != null && !globOrRegex.equals("glob:*")) {
                final PathMatcher pathMatcher = p.getFileSystem().getPathMatcher(globOrRegex);
                return tryWith.apply(s.filter(pathMatcher::matches));
            }
            return tryWith.apply(s);
        }
    }

    /**
     * Same as {@link Files#walk(Path, FileVisitOption...)}, but IOException is wrapped into a RuntimeException
     * for usage in {@link Stream#flatMap(Function)}
     */
    public static Stream<Path> sneakyWalk(Path p, FileVisitOption... options) {
        try {
            return Files.walk(p, options);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <R> R linesAndClose(Path p, Function<Stream<String>, R> tryWith) throws IOException {
        try (Stream<String> s = Files.lines(p)) {
            return tryWith.apply(s);
        }
    }


    public static long estimateNumOfLines(Path p) throws IOException {
        return estimateNumOfLines(p, 1024, 10);
    }

    public static long estimateNumOfLines(Path p, int sampleSize, int maxNumOfSamples) throws IOException {
        return estimateCharOccurrence(p, System.lineSeparator().charAt(0), sampleSize, maxNumOfSamples); //returns  CR o LR

    }

    public static long estimateCharOccurrence(Path p, char query, int sampleSize, int maxNumOfSamples) throws IOException {
        final long size = Files.size(p);

        long chunkSize = size / maxNumOfSamples;

        sampleSize = (int) Math.min(sampleSize, chunkSize);

        try (InputStream stream = Files.newInputStream(p)) {
            byte[] buffer = new byte[sampleSize];
            int count = 0;

            int n;
            int totalN = 0;
            while ((n = stream.read(buffer)) > 0) {
                totalN += n;
                for (int i = 0; i < n; i++) {
                    if (buffer[i] == query) count++;
                }
                stream.skip(chunkSize - n);
            }

            return (long) ((double) count / (double) totalN * (double) size);
        }
    }

    /**
     * Converts a standard POSIX Shell globbing pattern into a regular expression
     * pattern. The result can be used with the standard {@link java.util.regex} API to
     * recognize strings which match the glob pattern.
     * <p>
     * See also, the POSIX Shell language:
     * http://pubs.opengroup.org/onlinepubs/009695399/utilities/xcu_chap02.html#tag_02_13_01
     *
     * @param pattern A glob pattern.
     * @return A regex pattern to recognize the given glob pattern.
     */
    public static final String convertGlobToRegex(String pattern) {
        StringBuilder sb = new StringBuilder(pattern.length());
        int inGroup = 0;
        int inClass = 0;
        int firstIndexInClass = -1;
        char[] arr = pattern.toCharArray();
        for (int i = 0; i < arr.length; i++) {
            char ch = arr[i];
            switch (ch) {
                case '\\':
                    if (++i >= arr.length) {
                        sb.append('\\');
                    } else {
                        char next = arr[i];
                        switch (next) {
                            case ',':
                                // escape not needed
                                break;
                            case 'Q':
                            case 'E':
                                // extra escape needed
                                sb.append('\\');
                            default:
                                sb.append('\\');
                        }
                        sb.append(next);
                    }
                    break;
                case '*':
                    if (inClass == 0)
                        sb.append(".*");
                    else
                        sb.append('*');
                    break;
                case '?':
                    if (inClass == 0)
                        sb.append('.');
                    else
                        sb.append('?');
                    break;
                case '[':
                    inClass++;
                    firstIndexInClass = i + 1;
                    sb.append('[');
                    break;
                case ']':
                    inClass--;
                    sb.append(']');
                    break;
                case '.':
                case '(':
                case ')':
                case '+':
                case '|':
                case '^':
                case '$':
                case '@':
                case '%':
                    if (inClass == 0 || (firstIndexInClass == i && ch == '^'))
                        sb.append('\\');
                    sb.append(ch);
                    break;
                case '!':
                    if (firstIndexInClass == i)
                        sb.append('^');
                    else
                        sb.append('!');
                    break;
                case '{':
                    inGroup++;
                    sb.append('(');
                    break;
                case '}':
                    inGroup--;
                    sb.append(')');
                    break;
                case ',':
                    if (inGroup > 0)
                        sb.append('|');
                    else
                        sb.append(',');
                    break;
                default:
                    sb.append(ch);
            }
        }
        return sb.toString();
    }

    public static final Pattern compileGlobToRegex(String glob) {
        return Pattern.compile(convertGlobToRegex(glob));
    }
}