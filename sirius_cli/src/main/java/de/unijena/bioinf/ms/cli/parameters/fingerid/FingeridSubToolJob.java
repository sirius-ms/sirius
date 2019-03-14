package de.unijena.bioinf.ms.cli.parameters.fingerid;

import de.unijena.bioinf.ms.cli.parameters.InstanceJob;
import de.unijena.bioinf.sirius.ExperimentResult;

public class FingeridSubToolJob extends InstanceJob {
    @Override
    protected ExperimentResult compute() throws Exception {
        final ExperimentResult expRes = awaitInput();
        System.out.println("I am FingerID on Experiment " + expRes.getSimplyfiedExperimentName());

        //todo Fill Me
        return expRes;
    }
}
