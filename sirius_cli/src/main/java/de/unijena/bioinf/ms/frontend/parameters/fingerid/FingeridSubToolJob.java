package de.unijena.bioinf.ms.frontend.parameters.fingerid;

import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.fingerid.CSIPredictor;
import de.unijena.bioinf.fingerid.FingerIDJJob;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.fingerid.predictor_types.PredictorTypeAnnotation;
import de.unijena.bioinf.ms.frontend.parameters.InstanceJob;
import de.unijena.bioinf.sirius.ExperimentResult;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Map;

public class FingeridSubToolJob extends InstanceJob {
    @Override
    protected ExperimentResult compute() throws Exception {
        final ExperimentResult expRes = awaitInput();
        System.out.println("I am FingerID on Experiment " + expRes.getSimplyfiedExperimentName());
        PredictorTypeAnnotation type = expRes.getExperiment().getAnnotation(PredictorTypeAnnotation.class);

        //todo currently ther is only csi -> change if there are multiple methods
        // we need to run multiple structure ilucidation jobs and need  different prediciton results then.
        EnumSet<PredictorType> predictors = type.toPredictors(expRes.getExperiment().getPrecursorIonType().getCharge());
        final @NotNull CSIPredictor csi = (CSIPredictor) ApplicationCore.WEB_API.getPredictorFromType(predictors.iterator().next());
        final FingerIDJJob job = csi.makeFingerIDJJob(expRes);
        final Map<IdentificationResult, ProbabilityFingerprint> result = SiriusJobs.getGlobalJobManager().submitJob(job).awaitResult();

        return expRes;
    }





}
