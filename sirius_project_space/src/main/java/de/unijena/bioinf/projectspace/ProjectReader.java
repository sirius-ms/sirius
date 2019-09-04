package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
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

    /*
    This methods might be redundant (as they are just special textfiles) but we might later use different ways to serialize key/value or
    tables, e.g. when using databases
     */
    public default Map<String,String> keyValues(String relativePath) throws IOException {
        return textFile(relativePath, (r)-> FileUtils.readKeyValues(r));
    }

    public default int[] intVector(String relativePath) throws IOException {
        return textFile(relativePath, FileUtils::readAsIntVector);
    }
    public default double[] doubleVector(String relativePath) throws IOException {
        return textFile(relativePath, FileUtils::readAsDoubleVector);
    }
    public default int[][] intMatrix(String relativePath) throws IOException {
        return textFile(relativePath, FileUtils::readAsIntMatrix);
    }
    public default double[][] doubleMatrix(String relativePath) throws IOException {
        return textFile(relativePath, FileUtils::readAsDoubleMatrix);
    }

    public void table(String relativePath, boolean skipHeader, Consumer<String[]> f) throws IOException;


    public static interface ForContainer<S extends ProjectSpaceContainerId,T extends ProjectSpaceContainer<S>> {
        public void readAllComponents(ProjectReader reader, T container, IOFunctions.ClassValueConsumer consumer)  throws IOException;
    }

}
