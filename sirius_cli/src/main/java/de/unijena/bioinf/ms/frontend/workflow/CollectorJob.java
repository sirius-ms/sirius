package de.unijena.bioinf.ms.frontend.workflow;

import de.unijena.bioinf.ms.frontend.parameters.DataSetJob;
import de.unijena.bioinf.sirius.ExperimentResult;

public class CollectorJob extends DataSetJob {
    @Override
    protected Iterable<ExperimentResult> compute() throws Exception {
        return awaitInputs();
    }
}
