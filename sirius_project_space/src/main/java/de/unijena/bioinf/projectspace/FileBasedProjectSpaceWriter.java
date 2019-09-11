package de.unijena.bioinf.projectspace;

import com.google.common.base.Joiner;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class FileBasedProjectSpaceWriter extends FileBasedProjectSpaceIO implements ProjectWriter {

    public FileBasedProjectSpaceWriter(File dir, Function<Class<ProjectSpaceProperty>, ProjectSpaceProperty> propertyGetter) {
        super(dir, propertyGetter);
    }

    @Override
    public void textFile(String relativePath, IOFunctions.IOConsumer<BufferedWriter> func) throws IOException {
        try (final BufferedWriter stream = FileUtils.getWriter(resolveFilePath(relativePath))) {
            func.consume(stream);
        }
    }

    @Override
    public void binaryFile(String relativePath, IOFunctions.IOConsumer<BufferedOutputStream> func) throws IOException {
        try (final BufferedOutputStream stream = FileUtils.getOut(resolveFilePath(relativePath))) {
            func.consume(stream);
        }
    }

    @Override
    public void keyValues(String relativePath, Map<?, ?> map) throws IOException {
        try (final BufferedWriter stream = FileUtils.getWriter(resolveFilePath(relativePath))) {
            for (Map.Entry<?,?> entry : map.entrySet()) {
                stream.write(String.valueOf(entry.getKey()));
                stream.write('\t');
                stream.write(String.valueOf(entry.getValue()));
                stream.write('\n');
            }
        }
    }

    @Override
    public void table(String relativePath, @Nullable  String[] header, Iterable<String[]> rows) throws IOException {
        try (final BufferedWriter bw = FileUtils.getWriter(resolveFilePath(relativePath))) {
            String line;
            if (header!=null) {
                bw.write(Joiner.on('\t').join(header));
                bw.newLine();
            }
            for (String[] row : rows) {
                bw.write(Joiner.on('\t').join(row));
                bw.newLine();
            }
        }
    }

    @Override
    public void delete(String relativePath) throws IOException {
        Files.delete(asPath(relativePath));
    }

    protected File resolveFilePath(String relativePath) {
        File file = new File(dir, relativePath);
        file.getParentFile().mkdirs();
        return file;
    }
}
