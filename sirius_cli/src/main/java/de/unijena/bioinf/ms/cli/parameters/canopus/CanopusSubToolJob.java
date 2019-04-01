package de.unijena.bioinf.ms.cli.parameters.canopus;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.fingerid.CanopusJJob;
import de.unijena.bioinf.fingerid.FingerIdResult;
import de.unijena.bioinf.ms.cli.parameters.InstanceJob;
import de.unijena.bioinf.sirius.ExperimentResult;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.IdentificationResultAnnotationJJob;
import de.unijena.bioinf.sirius.core.ApplicationCore;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Collectors;

public class CanopusSubToolJob extends InstanceJob {

    @Override
    protected ExperimentResult compute() throws Exception {
        final ExperimentResult expRes = awaitInput();
        System.out.println("I am Canopus on Experiment " + expRes.getSimplyfiedExperimentName());
        // submit canopus jobs if needed
        expRes.getResults().stream()
                .map(this::buildAndSubmit)
                .collect(Collectors.toList())
                .forEach(IdentificationResultAnnotationJJob::takeAndAnnotateResult);

        return expRes;
    }

    private CanopusJJob buildAndSubmit(@NotNull final IdentificationResult ir) {
        final CanopusJJob canopusJob = new CanopusJJob(ApplicationCore.CANOPUS);
        canopusJob.setIdentificationResult(ir)
                .setFingerprint(ir.getAnnotationOrThrow(FingerIdResult.class).getPredictedFingerprint());
        return SiriusJobs.getGlobalJobManager().submitJob(canopusJob);
    }
}
