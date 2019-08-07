package de.unijena.bioinf.ms.frontend.subtools.input_provider;

import de.unijena.bioinf.sirius.ExperimentResult;
import org.jetbrains.annotations.NotNull;
import picocli.CommandLine;

import java.io.File;
import java.util.Iterator;
import java.util.List;

public class MzmlInputProvider implements InputProvider {
    final List<File> mzmlInput;

    public MzmlInputProvider(List<File> mzmlInput) {
        this.mzmlInput = mzmlInput;
        //todo remove after implementation
        throw new CommandLine.PicocliException("MZML input is not yet supported! This should not be possible. BUG?");
    }

    @NotNull
    @Override
    public Iterator<ExperimentResult> newInputExperimentIterator() {
        //todo implement
        return null;
    }
}
