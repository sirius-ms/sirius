package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;

import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FileBasedProjectSpaceWriter implements ProjectWriter {

    private File dir;

    FileBasedProjectSpaceWriter(File dir) {
        this.dir = dir;
    }

    @Override
    public List<String> glob(String globPath) throws IOException {
        final ArrayList<String> content = new ArrayList<>();
        Path r = dir.toPath();
        for (Path p : Files.newDirectoryStream(r, globPath)) {
            content.add(p.relativize(r).toString());
        }
        return content;
    }

    @Override
    public <T> T inDirectory(String relativePath, IOFunctions.IOCallable<T> reader)  throws IOException {
        final File newDir = new File(dir, relativePath);
        final File oldDir = dir;
        try {
            dir = newDir;
            return reader.call();
        } finally {
            dir = oldDir;
        }
    }

    @Override
    public Path asPath(String path) {
        return FileSystems.getDefault().getPath(path);
    }

    @Override
    public void textFile(String relativePath, IOFunctions.IOConsumer<BufferedWriter> func) throws IOException {
        try (final BufferedWriter stream = FileUtils.getWriter(new File(dir, relativePath))) {
            func.consume(stream);
        }
    }

    @Override
    public void binaryFile(String relativePath, IOFunctions.IOConsumer<BufferedOutputStream> func) throws IOException {
        try (final BufferedOutputStream stream = FileUtils.getOut(new File(dir, relativePath))) {
            func.consume(stream);
        }
    }

    @Override
    public void keyValues(String relativePath, Map<?, ?> map) throws IOException {
        try (final BufferedWriter stream = FileUtils.getWriter(new File(dir, relativePath))) {
            for (Map.Entry<?,?> entry : map.entrySet()) {
                stream.write(String.valueOf(entry.getKey()));
                stream.write('\t');
                stream.write(String.valueOf(entry.getValue()));
            }
        }
    }

    @Override
    public void delete(String relativePath) throws IOException {
        Files.delete(asPath(relativePath));
    }
}
