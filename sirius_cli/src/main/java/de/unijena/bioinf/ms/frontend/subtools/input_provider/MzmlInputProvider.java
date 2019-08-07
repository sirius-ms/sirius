package de.unijena.bioinf.ms.frontend.subtools.input_provider;

import de.unijena.bioinf.babelms.projectspace.SiriusProjectSpace;
import de.unijena.bioinf.sirius.ExperimentResult;
import org.jetbrains.annotations.NotNull;
import picocli.CommandLine;

import java.io.File;
import java.util.Iterator;
import java.util.List;

public class MzmlInputProvider implements InputProvider {
    private final List<File> mzmlInput;
    private final SiriusProjectSpace pSpace;

    public MzmlInputProvider(@NotNull List<File> mzmlInput, @NotNull final SiriusProjectSpace spaceToWrite) {
        this.mzmlInput = mzmlInput;
        this.pSpace = spaceToWrite;
        //todo remove after implementation
        throw new CommandLine.PicocliException("MZML input is not yet supported! This should not be possible. BUG?");
    }

    @NotNull
    @Override
    public Iterator<ExperimentResult> newInputExperimentIterator() {
        if (true) {
            //todo parse mzml and write experiments into given projectspace
        }
        return pSpace.parseExperimentIterator();
    }
}
