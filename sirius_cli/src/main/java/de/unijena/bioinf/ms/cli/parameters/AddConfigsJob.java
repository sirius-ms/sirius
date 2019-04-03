package de.unijena.bioinf.ms.cli.parameters;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.properties.FinalConfig;
import de.unijena.bioinf.babelms.ms.MsFileConfig;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.io.projectspace.ProjectSpaceConfig;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.sirius.ExperimentResult;

import java.util.Map;

public class AddConfigsJob extends InstanceJob {
    protected final Map<Class<Ms2ExperimentAnnotation>, Ms2ExperimentAnnotation> configInstances;
    private ParameterConfig cliConfig;

    public AddConfigsJob(ParameterConfig cliConfig, Map<Class<Ms2ExperimentAnnotation>, Ms2ExperimentAnnotation> configInstances) {
        this.configInstances = configInstances;
        this.cliConfig = cliConfig;
    }

    @Override
    protected ExperimentResult compute() throws Exception {
        final ExperimentResult expRes = awaitInput();
        final Ms2Experiment exp = expRes.getExperiment();


        ParameterConfig baseConfig;
        if (exp.hasAnnotation(ProjectSpaceConfig.class)) //override defaults
            baseConfig = exp.getAnnotation(ProjectSpaceConfig.class).config.newIndependentInstance(cliConfig);
        else
            baseConfig = cliConfig;

        if (exp.hasAnnotation(MsFileConfig.class))
            baseConfig = baseConfig.newIndependentInstance(exp.getAnnotation(MsFileConfig.class).config);

        exp.addAnnotationsFrom(configInstances);
        exp.setAnnotation(FinalConfig.class, new FinalConfig(baseConfig));

        return expRes;
    }
}