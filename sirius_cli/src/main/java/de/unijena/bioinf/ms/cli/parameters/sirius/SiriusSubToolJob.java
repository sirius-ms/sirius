package de.unijena.bioinf.ms.cli.parameters.sirius;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.properties.FinalConfig;
import de.unijena.bioinf.ms.cli.parameters.InstanceJob;
import de.unijena.bioinf.sirius.ExperimentResult;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.Sirius;
import de.unijena.bioinf.sirius.SiriusCachedFactory;

import java.util.List;

public class SiriusSubToolJob extends InstanceJob {
    protected final SiriusCachedFactory siriusProvider;

    public SiriusSubToolJob(SiriusCachedFactory siriusProvider) {
        this.siriusProvider = siriusProvider;
    }
    //todo use white set
    //todo search db for formula
    @Override
    protected ExperimentResult compute() throws Exception {
        final ExperimentResult expRes = awaitInput();
        final Ms2Experiment exp = expRes.getExperiment();

        System.out.println("I am Sirius on Experiment " + expRes.getSimplyfiedExperimentName());
        final Sirius sirius = siriusProvider.sirius(exp.getAnnotation(FinalConfig.class).config.getConfigValue("AlgorithmProfile"));
        List<IdentificationResult> results = SiriusJobs.getGlobalJobManager().submitJob(sirius.makeIdentificationJob(exp)).awaitResult();

        //annotate result
        expRes.getResults().clear();
        expRes.getResults().addAll(results);

        return expRes;
    }
}
