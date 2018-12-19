package de.unijena.bioinf.ms.projectspace;

import de.unijena.bioinf.sirius.ExperimentResult;

import java.io.IOException;

public interface ProjectReader extends Iterable<ExperimentDirectory> {

    ExperimentResult parseExperiment(final ExperimentDirectory expDir) throws IOException;

}