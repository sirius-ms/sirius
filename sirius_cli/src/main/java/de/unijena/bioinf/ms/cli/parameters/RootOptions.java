package de.unijena.bioinf.ms.cli.parameters;

import de.unijena.bioinf.ms.io.projectspace.SiriusProjectSpace;
import de.unijena.bioinf.sirius.ExperimentResult;

import java.util.Iterator;
import java.util.concurrent.Callable;

public interface RootOptions extends Callable<RootOptions.IO> {

    final class IO {
        public final SiriusProjectSpace projectSpace;
        public final Iterator<ExperimentResult> inputIterator;

        public IO(SiriusProjectSpace projectSpace, Iterator<ExperimentResult> inputIterator) {
            this.projectSpace = projectSpace;
            this.inputIterator = inputIterator;
        }
    }
}
