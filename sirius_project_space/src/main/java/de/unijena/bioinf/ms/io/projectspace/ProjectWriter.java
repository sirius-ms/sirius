package de.unijena.bioinf.ms.io.projectspace;

import de.unijena.bioinf.sirius.ExperimentResult;

import java.io.Closeable;
import java.io.IOException;

public interface ProjectWriter extends AutoCloseable, Closeable {

    void writeExperiment(ExperimentResult result) throws IOException;

    default boolean deleteExperiment(ExperimentDirectory id) throws IOException {
        throw new UnsupportedOperationException("deletion not supported");
    }
}
