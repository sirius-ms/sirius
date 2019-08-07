package de.unijena.bioinf.ms.frontend.subtools.input_provider;

import de.unijena.bioinf.sirius.ExperimentResult;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

@FunctionalInterface
public interface InputProvider {
    @NotNull Iterator<ExperimentResult> newInputExperimentIterator();
}
