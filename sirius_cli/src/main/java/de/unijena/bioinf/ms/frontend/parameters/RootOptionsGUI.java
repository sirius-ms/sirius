package de.unijena.bioinf.ms.frontend.parameters;

import de.unijena.bioinf.ms.io.projectspace.SiriusProjectSpace;
import de.unijena.bioinf.sirius.ExperimentResult;
import picocli.CommandLine;

import java.io.IOException;
import java.util.Iterator;
//todo move to GUI?
//maypbe define API in interface and make a abstract basic root cli

@CommandLine.Command
public class RootOptionsGUI implements RootOptions, InputProvider {
    //input an project-space
    private final SiriusProjectSpace projectSpace;
    private final Iterator<ExperimentResult> inputIterator;

    //todo we want to restrict this? Could make sence if we have a project space so that not everything is in the memory
    private final int initialInsanceBuffer = 0;
    private final int maxInstanceBuffer = 0;

    public RootOptionsGUI(SiriusProjectSpace projectSpace, Iterator<ExperimentResult> inputIterator) {
        this.projectSpace = projectSpace;
        this.inputIterator = inputIterator;
    }

    @Override
    public Integer getMaxInstanceBuffer() {
        return maxInstanceBuffer;
    }

    @Override
    public Integer getInitialInstanceBuffer() {
        return initialInsanceBuffer;
    }

    @Override
    public SiriusProjectSpace getProjectSpace() throws IOException {
        return projectSpace;
    }

    @Override
    public Iterator<ExperimentResult> newInputExperimentIterator() {
        return inputIterator;
    }
}
