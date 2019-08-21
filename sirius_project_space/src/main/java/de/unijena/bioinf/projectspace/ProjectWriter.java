package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ms.annotations.DataAnnotation;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.util.List;
import java.util.function.Consumer;

public interface ProjectWriter {

    public List<String> glob(String globPath);

    public void inDirectory(String relativePath, Consumer<ProjectReader> reader);
    public void textFile(String relativePath, Consumer<BufferedWriter> func);
    public void binaryFile(String relativePath, Consumer<BufferedOutputStream> func);

    public void delete(String relativePath);

    public static interface ForContainer extends ProjectWriter {
        public void writeAllComponents(ClassValueProducer producer);
        public void deleteAllComponents(ClassValueProducer producer);
    }

    public static interface ClassValueProducer {
        public <T extends DataAnnotation> T apply(Class<T> klass);
    }

}
