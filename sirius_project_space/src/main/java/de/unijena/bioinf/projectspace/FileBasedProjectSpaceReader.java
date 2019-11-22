package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public class FileBasedProjectSpaceReader extends FileBasedProjectSpaceIO implements ProjectReader {

    FileBasedProjectSpaceReader(Path dir, Function<Class<ProjectSpaceProperty>, Optional<ProjectSpaceProperty>> propertyGetter) {
        super(dir,propertyGetter);
    }

    @Override
    public <A> A textFile(String relativePath, IOFunctions.IOFunction<BufferedReader, A> func)  throws IOException {
        try (final BufferedReader stream = Files.newBufferedReader(asPath(relativePath))) {
            return func.apply(stream);
        }
    }

    @Override
    public <A> A binaryFile(String relativePath, IOFunctions.IOFunction<BufferedInputStream, A> func)  throws IOException {
        try (final BufferedInputStream stream = new BufferedInputStream(Files.newInputStream(asPath(relativePath)))) {
            return func.apply(stream);
        }
    }

    @Override
    public Map<String, String> keyValues(String relativePath) throws IOException {
        return FileUtils.readKeyValues(asPath(relativePath));
    }

    @Override
    public void table(String relativePath, boolean skipHeader, Consumer<String[]> f) throws IOException {
        try (final BufferedReader br = Files.newBufferedReader(asPath(relativePath))) {
            String line;
            if (skipHeader)
                line = br.readLine();
            while ((line=br.readLine())!=null) {
                f.accept(line.split("\t",-1));
            }
        }
    }

}
