package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ms.annotations.DataAnnotation;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.util.List;
import java.util.function.Consumer;

public interface ProjectReader {

    public List<String> glob(String globPath);

    public void inDirectory(String relativePath, Consumer<ProjectReader> reader);
    public void textFile(String relativePath, Consumer<BufferedReader> func);
    public void binaryFile(String relativePath, Consumer<BufferedInputStream> func);

    public static interface ForContainer extends ProjectReader {
        public void readAllComponents(ClassValueConsumer function);
    }

    public static interface ClassValueConsumer {
        public <T extends DataAnnotation> void apply(Class<T> klass, T value);
    }

}
