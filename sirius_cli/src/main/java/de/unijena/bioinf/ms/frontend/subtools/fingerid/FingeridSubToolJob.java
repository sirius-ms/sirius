package de.unijena.bioinf.ms.frontend.subtools.fingerid;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.fingerid.*;
import de.unijena.bioinf.fingerid.annotations.FormulaResultRankingScore;
import de.unijena.bioinf.fingerid.blast.FingerblastResult;
import de.unijena.bioinf.fingerid.blast.TopFingerblastScore;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.fingerid.predictor_types.PredictorTypeAnnotation;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.Instance;
import de.unijena.bioinf.ms.frontend.subtools.InstanceJob;
import de.unijena.bioinf.projectspace.FormulaScoring;
import de.unijena.bioinf.projectspace.fingerid.CSIClientData;
import de.unijena.bioinf.projectspace.sirius.CompoundContainer;
import de.unijena.bioinf.projectspace.sirius.FormulaResult;
import de.unijena.bioinf.sirius.IdentificationResult;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FingeridSubToolJob extends InstanceJob {

    @Override
    protected void computeAndAnnotateResult(final @NotNull Instance inst) throws Exception {
        List<? extends SScored<FormulaResult, ? extends FormulaScore>> formulaResults = inst.loadFormulaResults(
                inst.getExperiment().getAnnotationOrThrow(FormulaResultRankingScore.class).value,
                FormulaScoring.class, FTree.class, FingerprintResult.class, FingerblastResult.class);

        if (formulaResults == null || formulaResults.isEmpty())
            throw new IllegalArgumentException("No formula identification. Cannot Run CSI:FingerID without formula identifications. You may want to run the SIRIUS SubTool first.");

        if (!isRecompute(inst) && formulaResults.stream().findFirst().map(SScored::getCandidate)
                .map(c -> c.hasAnnotation(FingerprintResult.class) && c.hasAnnotation(FingerblastResult.class)).orElse(true)) {
            LOG().info("Skipping CSI:FingerID for Instance \"" + inst.getExperiment().getName() + "\" because results already exist or result list is empty.");
            return;
        }

        System.out.println("I am FingerID on Experiment " + inst.getID());
        invalidateResults(inst);

        PredictorTypeAnnotation type = inst.getExperiment().getAnnotationOrThrow(PredictorTypeAnnotation.class);


        //todo currently there is only csi -> change if there are multiple methods
        // we need to run multiple structure elucidation jobs and need  different prediction results then.
        EnumSet<PredictorType> predictors = type.toPredictors(inst.getExperiment().getPrecursorIonType().getCharge());
        final @NotNull CSIPredictor csi = (CSIPredictor) ApplicationCore.WEB_API.getPredictorFromType(predictors.iterator().next());
        final FingerIDJJob job = csi.makeFingerIDJJob(inst.getExperiment(),
                formulaResults.stream().map(res -> new IdentificationResult<>(res.getCandidate().getAnnotationOrThrow(FTree.class), res.getScoreObject()))
                        .collect(Collectors.toList()));


        // do computation and await results
        List<FingerIdResult> result = SiriusJobs.getGlobalJobManager().submitJob(job).awaitResult();

        final Map<FTree, FormulaResult> formulaResultsMap = formulaResults.stream().collect(Collectors.toMap(r -> r.getCandidate().getAnnotationOrThrow(FTree.class), SScored::getCandidate));

        // add CSIClientData to PS if it is not already there
        if (inst.getProjectSpace().getProjectSpaceProperty(CSIClientData.class).isEmpty())
            inst.getProjectSpace().setProjectSpaceProperty(CSIClientData.class, new CSIClientData(csi));


        // add new id results to projectspace and mal.
        final CompoundContainer ioC = inst.loadCompoundContainer();
        for (IdentificationResult idr : job.getAddedIdentificationResults())
            inst.getProjectSpace().newFormulaResultWithUniqueId(ioC, idr.getTree())
                    .ifPresent(fr -> formulaResultsMap.put(fr.getAnnotationOrThrow(FTree.class), fr));


        assert formulaResultsMap.size() == result.size();

        for (FingerIdResult structRes : result) {

            final FormulaResult formRes = formulaResultsMap.get(structRes.sourceTree);
            // annotate results

            assert structRes.sourceTree == formRes.getAnnotationOrThrow(FTree.class);

            formRes.setAnnotation(FingerprintResult.class, structRes.getAnnotationOrThrow(FingerprintResult.class));
            formRes.setAnnotation(FingerblastResult.class, structRes.getAnnotationOrThrow(FingerblastResult.class));

            formRes.getAnnotationOrThrow(FormulaScoring.class).setAnnotation(TopFingerblastScore.class, structRes.getAnnotationOrThrow(FingerblastResult.class).getTopHitScore());
            formRes.getAnnotationOrThrow(FormulaScoring.class).setAnnotation(ConfidenceScore.class, structRes.getAnnotationOrThrow(ConfidenceResult.class).score);

            //setRanking score
            inst.getExperiment().setAnnotation(FormulaResultRankingScore.class, new FormulaResultRankingScore(TopFingerblastScore.class));
            // write results back to project space
            inst.updateFormulaResult(formRes,
                    FormulaScoring.class, FingerprintResult.class, FingerblastResult.class);


        }
    }
}
