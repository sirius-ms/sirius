package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FileBasedProjectSpaceReader implements ProjectReader {

    private File dir;

    FileBasedProjectSpaceReader(File dir) {
        this.dir = dir;
    }

    @Override
    public <A> A textFile(String relativePath, IOFunctions.IOFunction<BufferedReader, A> func)  throws IOException {
        try (final BufferedReader stream = FileUtils.getReader(new File(dir, relativePath))) {
            return func.apply(stream);
        }
    }

    @Override
    public <A> A binaryFile(String relativePath, IOFunctions.IOFunction<BufferedInputStream, A> func)  throws IOException {
        try (final BufferedInputStream stream = FileUtils.getIn(new File(dir, relativePath))) {
            return func.apply(stream);
        }
    }

    @Override
    public Map<String, String> keyValues(String relativePath) throws IOException {
        return FileUtils.readKeyValues(new File(dir,relativePath));
    }

    @Override
    public List<String> glob(String globPath)  throws IOException{
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
}
