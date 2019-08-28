package de.unijena.bioinf.projectspace;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

public interface ProjectIO {

    public List<String> glob(String globPath) throws IOException;

    public <T> T  inDirectory(String relativePath, IOFunctions.IOCallable<T> reader) throws IOException;

    public default URL asURL(String path) {
        try {
            return asPath(path).toUri().toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }
    public default File asFile(String path) {
        return asPath(path).toFile();
    };
    public Path asPath(String path);

}
