package de.unijena.bioinf.ms.cli.parameters;

import de.unijena.bioinf.ms.io.projectspace.SiriusProjectSpace;
import de.unijena.bioinf.sirius.ExperimentResult;
import picocli.CommandLine;

import java.util.Iterator;
//todo move to GUI?
//maypbe define API in interface and make a abstract basic root cli

@CommandLine.Command
public class RootOptionsGUI extends RootOptionsCLI {
    //input an project-space
    private final SiriusProjectSpace projectSpace;
    private final Iterator<ExperimentResult> inputIterator;

    public RootOptionsGUI(SiriusProjectSpace projectSpace, Iterator<ExperimentResult> inputIterator) {
        this.projectSpace = projectSpace;
        this.inputIterator = inputIterator;
    }

    @Override
    public IO call() throws Exception {
        return new IO(projectSpace, inputIterator);
    }
}
