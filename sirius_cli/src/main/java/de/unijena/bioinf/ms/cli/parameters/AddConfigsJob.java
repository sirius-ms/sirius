package de.unijena.bioinf.ms.cli.parameters;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.Whiteset;
import de.unijena.bioinf.ChemistryBase.ms.properties.FinalConfig;
import de.unijena.bioinf.babelms.ms.MsFileConfig;
import de.unijena.bioinf.fingerid.FormulaWhiteListJob;
import de.unijena.bioinf.fingerid.db.annotation.FormulaSearchDB;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.io.projectspace.ProjectSpaceConfig;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.sirius.ExperimentResult;
import de.unijena.bioinf.sirius.core.ApplicationCore;

public class AddConfigsJob extends InstanceJob {
    private ParameterConfig cliConfig;

    public AddConfigsJob(ParameterConfig cliConfig) {
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

        //fill all annotations
        exp.setAnnotation(FinalConfig.class, new FinalConfig(baseConfig));
        exp.addAnnotationsFrom(baseConfig, Ms2ExperimentAnnotation.class);

        return expRes;
    }
}