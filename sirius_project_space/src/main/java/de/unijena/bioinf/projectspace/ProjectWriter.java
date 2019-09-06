package de.unijena.bioinf.projectspace;

import org.jetbrains.annotations.Nullable;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;

public interface ProjectWriter extends ProjectIO {

    public void textFile(String relativePath, IOFunctions.IOConsumer<BufferedWriter> func)  throws IOException;
    public void binaryFile(String relativePath, IOFunctions.IOConsumer<BufferedOutputStream> func)  throws IOException;

    public void keyValues(String relativePath, Map<?,?> map) throws IOException;

    public void table(String relativePath,@Nullable String[] header, Iterable<String[]> rows) throws IOException;

    public void delete(String relativePath)  throws IOException;

    public static interface ForContainer<S extends ProjectSpaceContainerId,T extends ProjectSpaceContainer<S>> {
        public void writeAllComponents(ProjectWriter writer, T container, IOFunctions.ClassValueProducer producer)  throws IOException;
    }

    public static interface DeleteContainer<S extends ProjectSpaceContainerId> {
        public void deleteAllComponents(ProjectWriter writer, S containerId) throws IOException;
    }
}
