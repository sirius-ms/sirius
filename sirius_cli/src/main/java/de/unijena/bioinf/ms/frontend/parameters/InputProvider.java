package de.unijena.bioinf.ms.frontend.parameters;

import de.unijena.bioinf.sirius.ExperimentResult;

import java.io.IOException;
import java.util.Iterator;

@FunctionalInterface
public interface InputProvider {
    Iterator<ExperimentResult> newInputExperimentIterator() throws IOException;
}
