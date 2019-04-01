package de.unijena.bioinf.ms.cli.parameters;

import de.unijena.bioinf.sirius.ExperimentResult;

import java.util.Iterator;

@FunctionalInterface
public interface InputProvider {
    Iterator<ExperimentResult> newInputExperimentIterator();
}
