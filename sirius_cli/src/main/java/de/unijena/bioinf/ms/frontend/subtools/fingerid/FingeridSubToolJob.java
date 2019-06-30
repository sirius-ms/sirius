package de.unijena.bioinf.ms.frontend.subtools.fingerid;

import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.fingerid.CSIPredictor;
import de.unijena.bioinf.fingerid.FingerIDJJob;
import de.unijena.bioinf.fingerid.FingerIdResult;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.fingerid.predictor_types.PredictorTypeAnnotation;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.InstanceJob;
import de.unijena.bioinf.sirius.ExperimentResult;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.IdentificationResults;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Map;

public class FingeridSubToolJob extends InstanceJob {

    @Override
    protected void computeAndAnnotateResult(final @NotNull ExperimentResult expRes) throws Exception {
        if (!expRes.hasAnnotation(IdentificationResults.class))
            throw new IllegalArgumentException("No formula identification. Cannot Run CSI:FingerID without formula identifications. You may want to run the SIRIUS SubTool first.");

        if (!isRecompute(expRes) && expRes.getAnnotation(IdentificationResults.class).getBest().map(x->x.hasAnnotation(FingerIdResult.class)).orElse(true)){
            LOG().info("Skipping CSI:FingerID for Instance \"" + expRes.getExperiment().getName() + "\" because results already exist or result list is empty.");
            return;
        }

        System.out.println("I am FingerID on Experiment " + expRes.getSimplyfiedExperimentName());
        invalidateResults(expRes);
        PredictorTypeAnnotation type = expRes.getExperiment().getAnnotation(PredictorTypeAnnotation.class);


        //todo currently ther is only csi -> change if there are multiple methods
        // we need to run multiple structure elucidation jobs and need  different prediction results then.
        EnumSet<PredictorType> predictors = type.toPredictors(expRes.getExperiment().getPrecursorIonType().getCharge());
        final @NotNull CSIPredictor csi = (CSIPredictor) ApplicationCore.WEB_API.getPredictorFromType(predictors.iterator().next());
        final FingerIDJJob job = csi.makeFingerIDJJob(expRes);
        final Map<IdentificationResult, ProbabilityFingerprint> result = SiriusJobs.getGlobalJobManager().submitJob(job).awaitResult();
    }
}
