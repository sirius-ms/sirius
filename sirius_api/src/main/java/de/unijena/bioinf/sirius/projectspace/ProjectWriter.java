package de.unijena.bioinf.sirius.projectspace;

import java.io.Closeable;
import java.io.IOException;

public interface ProjectWriter extends AutoCloseable, Closeable {

    public void writeExperiment(ExperimentResult result) throws IOException;

}
