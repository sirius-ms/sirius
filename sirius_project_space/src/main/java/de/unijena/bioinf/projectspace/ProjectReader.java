package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.projectspace.sirius.CompoundContainer;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

public interface ProjectReader extends ProjectIO {
    public <A> A textFile(String relativePath, IOFunctions.IOFunction<BufferedReader, A> func)  throws IOException;
    public <A> A binaryFile(String relativePath, IOFunctions.IOFunction<BufferedInputStream, A> func)  throws IOException;

    public Map<String,String> keyValues(String relativePath) throws IOException;


    public static interface ForContainer<S extends ProjectSpaceContainerId,T extends ProjectSpaceContainer<S>> {
        public void readAllComponents(ProjectReader writer, T container, IOFunctions.ClassValueProducer producer)  throws IOException;
    }

}
