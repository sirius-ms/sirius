package de.unijena.bioinf.ms.cli.parameters.sirius;

import de.unijena.bioinf.ms.cli.parameters.InstanceJob;
import de.unijena.bioinf.sirius.ExperimentResult;

public class SiriusSubToolJob extends InstanceJob {

    @Override
    protected ExperimentResult compute() throws Exception {
        final ExperimentResult expRes = awaitInput();
        System.out.println("I am Sirius on Experiment " + expRes.getSimplyfiedExperimentName());
        //todo Fill me
        return expRes;
    }
}
