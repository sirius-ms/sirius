package de.unijena.bioinf.ms.io.projectspace;

import de.unijena.bioinf.sirius.ExperimentResult;

import java.io.Closeable;
import java.io.IOException;

public interface ProjectReader extends Iterable<ExperimentDirectory>, Closeable, AutoCloseable {

    ExperimentResult parseExperiment(final ExperimentDirectory expDir) throws IOException;

}