package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public interface ProjectIO {

    /**
     * Returns a list of all files in the current directory, filtered by the given globPattern
     * @param globPattern a glob-like pattern for the file. Does not support sub-directories!!!
     * @return list of files in the current directory that match globPattern
     * @throws IOException if io error occurs
     */
    public List<String> list(String globPattern) throws IOException;

    public boolean exists(String relativePath) throws IOException;

    public <A extends ProjectSpaceProperty> Optional<A> getProjectSpaceProperty(Class<A> klass);

    public <T> T inDirectory(String relativePath, IOFunctions.IOCallable<T> ioAction) throws IOException;

    public default URL asURL(String path) {
        try {
            return asPath(path).toUri().toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public Path asPath(String path);

}
