package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

public interface ProjectWriter extends ProjectIO {

    public void textFile(String relativePath, IOFunctions.IOConsumer<BufferedWriter> func)  throws IOException;
    public void binaryFile(String relativePath, IOFunctions.IOConsumer<BufferedOutputStream> func)  throws IOException;

    public <A extends ProjectSpaceProperty> Optional<A> getProjectSpaceProperty(Class<A> klass);

    public void keyValues(String relativePath, Map<?,?> map) throws IOException;

    public void table(String relativePath,@Nullable String[] header, Iterable<String[]> rows) throws IOException;

    public default void intVector(String relativePath, int[] vector) throws IOException {
        textFile(relativePath, w->FileUtils.writeIntVector(w,vector));
    }
    public default void doubleVector(String relativePath, double[] vector) throws IOException {
        textFile(relativePath, w->FileUtils.writeDoubleVector(w,vector));
    }
    public default void intMatrix(String relativePath, int[][] matrix) throws IOException {
        textFile(relativePath, w->FileUtils.writeIntMatrix(w,matrix));
    }
    public default void doubleMatrix(String relativePath, double[][] matrix) throws IOException {
        textFile(relativePath, w->FileUtils.writeDoubleMatrix(w,matrix));
    }

    public void delete(String relativePath)  throws IOException;

    public void deleteIfExists(String relativePath) throws IOException;

    public static interface ForContainer<S extends ProjectSpaceContainerId,T extends ProjectSpaceContainer<S>> {
        public void writeAllComponents(ProjectWriter writer, T container, IOFunctions.ClassValueProducer producer)  throws IOException;
    }

    public static interface DeleteContainer<S extends ProjectSpaceContainerId> {
        public void deleteAllComponents(ProjectWriter writer, S containerId) throws IOException;
    }
}
