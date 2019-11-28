package de.unijena.bioinf.projectspace;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class FileBasedProjectSpaceIO implements ProjectIO {


    protected Path dir;
    protected final Function<Class<ProjectSpaceProperty>, Optional<ProjectSpaceProperty>> propertyGetter;

    public FileBasedProjectSpaceIO(Path dir, Function<Class<ProjectSpaceProperty>, Optional<ProjectSpaceProperty>> propertyGetter) {
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
        try (final DirectoryStream<Path> stream = Files.newDirectoryStream(dir, globPattern)) {
            for (Path p : stream)
                content.add(dir.relativize(p).toString());
        }
        return content;
    }


    @Override
    public boolean exists(String relativePath) {
        return Files.exists(asPath(relativePath));
    }


    @Override
    public <T> T inDirectory(String relativePath, IOFunctions.IOCallable<T> ioAction)  throws IOException {
        final Path newDir = asPath(relativePath);
        final Path oldDir = dir;
        try {
            dir = newDir;
            return ioAction.call();
        } finally {
            dir = oldDir;
        }
    }

    @Override
    public Path asPath(String relativePath) {
        return dir.resolve(relativePath);
    }
}
