package de.unijena.bioinf.ms.cli.workflow;

import de.unijena.bioinf.ms.cli.parameters.DataSetJob;
import de.unijena.bioinf.sirius.ExperimentResult;

public class CollectorJob extends DataSetJob {
    @Override
    protected Iterable<ExperimentResult> compute() throws Exception {
        return awaitInputs();
    }
}
