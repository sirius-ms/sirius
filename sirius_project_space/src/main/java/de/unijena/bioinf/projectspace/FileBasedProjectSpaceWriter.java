package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class FileBasedProjectSpaceWriter extends FileBasedProjectSpaceIO implements ProjectWriter {

    public FileBasedProjectSpaceWriter(Path dir, Function<Class<ProjectSpaceProperty>, Optional<ProjectSpaceProperty>> propertyGetter) {
        super(dir, propertyGetter);
    }

    @Override
    public void textFile(String relativePath, IOFunctions.IOConsumer<BufferedWriter> func) throws IOException {
        try (final BufferedWriter stream = Files.newBufferedWriter(resolveAndMkFilePath(relativePath))) {
            func.accept(stream);
        }
    }

    @Override
    public void binaryFile(String relativePath, IOFunctions.IOConsumer<BufferedOutputStream> func) throws IOException {
        try (final BufferedOutputStream stream = new BufferedOutputStream(Files.newOutputStream(resolveAndMkFilePath(relativePath)))) {
            func.accept(stream);
        }
    }

    @Override
    public void keyValues(String relativePath, Map<?, ?> map) throws IOException {
        try (final BufferedWriter stream = Files.newBufferedWriter(resolveAndMkFilePath(relativePath))) {
            FileUtils.writeKeyValues(stream, map);
        }
    }

    @Override
    public void table(String relativePath, @Nullable  String[] header, Iterable<String[]> rows) throws IOException {
        try (final BufferedWriter bw = Files.newBufferedWriter(resolveAndMkFilePath(relativePath))) {
            FileUtils.writeTable(bw, header, rows);
        }
    }

    @Override
    public void delete(String relativePath) throws IOException {
        FileUtils.deleteRecursively((asPath(relativePath)));
    }

    @Override
    public void deleteIfExists(String relativePath) throws IOException {
        Files.deleteIfExists(asPath(relativePath));
    }

    protected Path resolveAndMkFilePath(String relativePath) throws IOException {
        Path file = asPath(relativePath);
        Files.createDirectories(file.getParent());
        return file;
    }
}
