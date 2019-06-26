package de.unijena.bioinf.ms.frontend.subtools.canopus;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.fingerid.CanopusJJob;
import de.unijena.bioinf.fingerid.CanopusResult;
import de.unijena.bioinf.fingerid.FingerIdResult;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.InstanceJob;
import de.unijena.bioinf.sirius.ExperimentResult;
import de.unijena.bioinf.sirius.IdentificationResult;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class CanopusSubToolJob extends InstanceJob {

    @Override
    protected void computeAndAnnotateResult(final @NotNull ExperimentResult expRes) throws Exception {
        System.out.println("I am Canopus on Experiment " + expRes.getSimplyfiedExperimentName());
        // check if we need to skip
        if (!isRecompute(expRes) && expRes.getResults().stream().anyMatch((it -> it.hasAnnotation(CanopusResult.class)))) {
            LOG().info("Skipping Canopus for Instance \"" + expRes.getExperiment().getName() + "\" because results already exist.");
            return;
        }

        //invalidate result for following computation
        invalidateResults(expRes);

        // create input
        final List<IdentificationResult> res = expRes.getResults().stream()
                .filter(ir -> ir.hasAnnotation(FingerIdResult.class)).collect(Collectors.toList());

        // check for valid input
        if (res.size() < 1)
            throw new IllegalArgumentException("No FingerID Result available for compound class prediction");

        // submit canopus jobs for Identification results that contain CSI:FingerID results
        res.stream().map(this::buildAndSubmit).forEach(CanopusJJob::takeAndAnnotateResult);
    }

    private CanopusJJob buildAndSubmit(@NotNull final IdentificationResult ir) {
        final CanopusJJob canopusJob = new CanopusJJob(ApplicationCore.CANOPUS);
        canopusJob.setIdentificationResult(ir)
                .setFingerprint(ir.getAnnotation(FingerIdResult.class).getPredictedFingerprint());
        return SiriusJobs.getGlobalJobManager().submitJob(canopusJob);
    }
}
