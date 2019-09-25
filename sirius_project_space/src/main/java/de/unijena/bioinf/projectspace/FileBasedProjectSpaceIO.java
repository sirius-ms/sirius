package de.unijena.bioinf.projectspace;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class FileBasedProjectSpaceIO implements ProjectIO {


    protected File dir;
    protected final Function<Class<ProjectSpaceProperty>, Optional<ProjectSpaceProperty>> propertyGetter;

    public FileBasedProjectSpaceIO(File dir, Function<Class<ProjectSpaceProperty>, Optional<ProjectSpaceProperty>> propertyGetter) {
        this.dir = dir;
        this.propertyGetter = propertyGetter;
    }

    @Override
    public <A extends ProjectSpaceProperty> Optional<A> getProjectSpaceProperty(Class<A> klass) {
        return (Optional<A>)propertyGetter.apply((Class<ProjectSpaceProperty>)klass);
    }


    @Override
    public List<String> list(String globPattern)  throws IOException {
        final ArrayList<String> content = new ArrayList<>();
        Path r = dir.toPath();
        for (Path p : Files.newDirectoryStream(r, globPattern)) {
            content.add(r.relativize(p).toString());
        }
        return content;
    }


    @Override
    public boolean exists(String relativePath) throws IOException {
        return new File(dir,relativePath).exists();
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
    public Path asPath(String relativePath) {
        return dir.toPath().resolve(relativePath);
    }
}
