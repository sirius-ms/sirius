package de.unijena.bioinf.ms.cli.parameters.canopus;

import de.unijena.bioinf.ms.cli.parameters.InstanceJob;
import de.unijena.bioinf.sirius.ExperimentResult;

public class CanopusSubToolJob extends InstanceJob {
    @Override
    protected ExperimentResult compute() throws Exception {
        final ExperimentResult expRes = awaitInput();
        System.out.println("I am Canopus on Experiment " + expRes.getSimplyfiedExperimentName());
//        expRes.getExperiment().forEachAnnotation().forEachRemaining(e -> System.out.println(e.getKey().toString() + " -> " + e.getValue().toString()));

        return expRes;
    }
}
