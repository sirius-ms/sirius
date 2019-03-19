package de.unijena.bioinf.ms.cli.parameters;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.sirius.ExperimentResult;

import java.util.Map;

public class AddConfigsJob extends InstanceJob {
    protected final Map<Class<Ms2ExperimentAnnotation>, Ms2ExperimentAnnotation> config;

    public AddConfigsJob(Map<Class<Ms2ExperimentAnnotation>, Ms2ExperimentAnnotation> config) {
        this.config = config;
    }

    @Override
    protected ExperimentResult compute() throws Exception {
        final ExperimentResult expRes = awaitInput();
        expRes.getExperiment().addAnnotationsFrom(config);
        return expRes;
    }


}
