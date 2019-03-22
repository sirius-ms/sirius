package de.unijena.bioinf.ms.cli.parameters;

import de.unijena.bioinf.ChemistryBase.ms.FinalConfig;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.babelms.ms.MsFileConfig;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.sirius.ExperimentResult;

import java.util.Map;

public class AddConfigsJob extends InstanceJob {
    protected final FinalConfig configAnnotation;
    protected final Map<Class<Ms2ExperimentAnnotation>, Ms2ExperimentAnnotation> configInstances;

    public AddConfigsJob(ParameterConfig sourceConfig, Map<Class<Ms2ExperimentAnnotation>, Ms2ExperimentAnnotation> configInstances) {
        this.configInstances = configInstances;
        this.configAnnotation = new FinalConfig(sourceConfig.newIndependendInstance(true));
    }

    @Override
    protected ExperimentResult compute() throws Exception {
        final ExperimentResult expRes = awaitInput();
        final Ms2Experiment exp = expRes.getExperiment();

        if (exp.hasAnnotation(MsFileConfig.class)) {
            MsFileConfig msc = exp.getAnnotation(MsFileConfig.class);
            expRes.getExperiment().setAnnotationsFrom(msc.config.createInstancesWithModifiedDefaults(Ms2ExperimentAnnotation.class));
            configAnnotation.config.changeModifiedFrom(msc.config);
        }

        exp.addAnnotationsFrom(configInstances);
        exp.setAnnotation(FinalConfig.class, configAnnotation);

        return expRes;
    }
}