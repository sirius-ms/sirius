package de.unijena.bioinf.ms.projectspace;

import de.unijena.bioinf.sirius.ExperimentResult;

import java.io.Closeable;
import java.io.IOException;

public interface ProjectWriter extends AutoCloseable, Closeable {

    void writeExperiment(ExperimentResult result) throws IOException;

}
