package de.unijena.bioinf.ms.frontend.subtools.fingerid;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.fingerid.CSIPredictor;
import de.unijena.bioinf.fingerid.FingerIDJJob;
import de.unijena.bioinf.fingerid.FingerIdResult;
import de.unijena.bioinf.fingerid.annotations.FormulaResultRankingScore;
import de.unijena.bioinf.fingerid.blast.FingerblastResult;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.fingerid.predictor_types.PredictorTypeAnnotation;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.Instance;
import de.unijena.bioinf.ms.frontend.subtools.InstanceJob;
import de.unijena.bioinf.projectspace.FormulaScoring;
import de.unijena.bioinf.projectspace.sirius.FormulaResult;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.scores.FormulaScore;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FingeridSubToolJob extends InstanceJob {

    @Override
    protected void computeAndAnnotateResult(final @NotNull Instance inst) throws Exception {
        List<? extends SScored<FormulaResult, ? extends FormulaScore>> formRes = inst.getProjectSpace()
                .getFormulaResultsOrderedBy(inst.getID(),
                        inst.getExperiment().getAnnotation(FormulaResultRankingScore.class).value,
                        FormulaScoring.class, FTree.class, FingerIdResult.class, FingerblastResult.class);

//        if (!inst.hasAnnotation(IdentificationResults.class))
//            throw new IllegalArgumentException("No formula identification. Cannot Run CSI:FingerID without formula identifications. You may want to run the SIRIUS SubTool first.");

        if (!isRecompute(inst) && formRes.stream().findFirst().map(SScored::getCandidate)
                .map(c -> c.hasAnnotation(FingerIdResult.class) && c.hasAnnotation(FingerblastResult.class)).orElse(true)) {
            LOG().info("Skipping CSI:FingerID for Instance \"" + inst.getExperiment().getName() + "\" because results already exist or result list is empty.");
            return;
        }

        System.out.println("I am FingerID on Experiment " + inst.getID());
        invalidateResults(inst);
        PredictorTypeAnnotation type = inst.getExperiment().getAnnotation(PredictorTypeAnnotation.class);


        //todo currently there is only csi -> change if there are multiple methods
        // we need to run multiple structure elucidation jobs and need  different prediction results then.
        EnumSet<PredictorType> predictors = type.toPredictors(inst.getExperiment().getPrecursorIonType().getCharge());
        final @NotNull CSIPredictor csi = (CSIPredictor) ApplicationCore.WEB_API.getPredictorFromType(predictors.iterator().next());
        final FingerIDJJob job = csi.makeFingerIDJJob(inst.getExperiment(),
                formRes.stream().map(res -> new IdentificationResult<>(res.getCandidate().getAnnotation(FTree.class),res.getScoreObject()))
                        .collect(Collectors.toList()));
        Map<IdentificationResult, FingerIdResult> result = SiriusJobs.getGlobalJobManager().submitJob(job).awaitResult();
    }
}
