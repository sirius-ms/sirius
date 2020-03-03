package de.unijena.bioinf.ms.frontend.subtools.fingerid;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.fp.Tanimoto;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.fingerid.*;
import de.unijena.bioinf.fingerid.blast.FBCandidateFingerprints;
import de.unijena.bioinf.fingerid.blast.FBCandidates;
import de.unijena.bioinf.fingerid.blast.FingerblastResult;
import de.unijena.bioinf.fingerid.blast.TopCSIScore;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.fingerid.predictor_types.PredictorTypeAnnotation;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.io.projectspace.Instance;
import de.unijena.bioinf.ms.frontend.subtools.InstanceJob;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import de.unijena.bioinf.projectspace.FormulaScoring;
import de.unijena.bioinf.projectspace.sirius.FormulaResult;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.utils.NetUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FingeridSubToolJob extends InstanceJob {

    @Override
    protected void computeAndAnnotateResult(final @NotNull Instance inst) throws Exception {
        List<? extends SScored<FormulaResult, ? extends FormulaScore>> formulaResults =
                inst.loadFormulaResults(FormulaScoring.class, FTree.class, FingerprintResult.class, FBCandidates.class);

        if (formulaResults == null || formulaResults.isEmpty()) {
            logInfo("Skipping instance \"" + inst.getExperiment().getName() + "\" because there are not trees computed.");
            return;
        }

        if (!isRecompute(inst) && formulaResults.stream().findFirst().map(SScored::getCandidate)
                .map(c -> c.hasAnnotation(FingerprintResult.class) && c.hasAnnotation(FBCandidates.class)).orElse(true)) {
            logInfo("Skipping CSI:FingerID for Instance \"" + inst.getExperiment().getName() + "\" because results already exist or result list is empty.");
            return;
        }

        invalidateResults(inst);

        PredictorTypeAnnotation type = inst.getExperiment().getAnnotationOrThrow(PredictorTypeAnnotation.class);


        //todo currently there is only csi -> change if there are multiple methods
        // we need to run multiple structure elucidation jobs and need  different prediction results then.
        EnumSet<PredictorType> predictors = type.toPredictors(inst.getExperiment().getPrecursorIonType().getCharge());
        final @NotNull CSIPredictor csi = NetUtils.tryAndWait(() -> (CSIPredictor) ApplicationCore.WEB_API.getStructurePredictor(predictors.iterator().next()), this::checkForInterruption);

        final FingerIDJJob<?> job = csi.makeFingerIDJJob(inst.getExperiment(),
                formulaResults.stream().map(res -> new IdentificationResult<>(res.getCandidate().getAnnotationOrThrow(FTree.class), res.getScoreObject()))
                        .collect(Collectors.toList()));

        // do computation and await results
        List<FingerIdResult> result = SiriusJobs.getGlobalJobManager().submitJob(job).awaitResult();

        final Map<FTree, FormulaResult> formulaResultsMap = formulaResults.stream().collect(Collectors.toMap(r -> r.getCandidate().getAnnotationOrThrow(FTree.class), SScored::getCandidate));

        // add CSIClientData to PS if it is not already there
        if (inst.getProjectSpaceManager().getProjectSpaceProperty(FingerIdData.class).isEmpty())
            inst.getProjectSpaceManager().setProjectSpaceProperty(FingerIdData.class, ApplicationCore.WEB_API.getFingerIdData(predictors.iterator().next()));


        // add new id results to projectspace and mal.
        Map<? extends IdentificationResult<?>, ? extends IdentificationResult<?>> addedResults = job.getAddedIdentificationResults();

        addedResults.forEach((k, v) ->
                inst.newFormulaResultWithUniqueId(k.getTree())
                        .ifPresent(fr -> {
                            fr.getAnnotationOrThrow(FormulaScoring.class).setAnnotationsFrom(
                                    formulaResultsMap.get(v.getTree()).getAnnotationOrThrow(FormulaScoring.class));
                            inst.updateFormulaResult(fr, FormulaScoring.class);

                            formulaResultsMap.put(fr.getAnnotationOrThrow(FTree.class), fr);
                        }));


        assert formulaResultsMap.size() >= result.size();

        //calculate and annotate tanimoto scores
        List<BasicJJob<Double>> tanimotoJobs = new ArrayList<>();
        result.stream().filter(it -> it.hasAnnotation(FingerprintResult.class) && it.hasAnnotation(FingerblastResult.class)).forEach(it -> {
            final ProbabilityFingerprint fp = it.getPredictedFingerprint();
            it.getFingerprintCandidates().stream().map(SScored::getCandidate).forEach(candidate ->
                    tanimotoJobs.add(new BasicJJob<>() {
                        @Override
                        protected Double compute() {
                            double t = Tanimoto.probabilisticTanimoto(fp, candidate.getFingerprint()).expectationValue();
                            candidate.setTanimoto(t);
                            return t;
                        }
                    })
            );
        });

        SiriusJobs.getGlobalJobManager().submitJobsInBatches(tanimotoJobs).forEach(JJob::getResult);

        //annotate FingerIdResults to FormulaResult
        for (FingerIdResult structRes : result) {
            final FormulaResult formRes = formulaResultsMap.get(structRes.sourceTree);
            assert structRes.sourceTree == formRes.getAnnotationOrThrow(FTree.class);

            // annotate results
            formRes.setAnnotation(FingerprintResult.class, structRes.getAnnotationOrNull(FingerprintResult.class));

            formRes.setAnnotation(FBCandidates.class, structRes.getAnnotation(FingerblastResult.class).map(FingerblastResult::getCandidates).orElse(null));
            formRes.setAnnotation(FBCandidateFingerprints.class, structRes.getAnnotation(FingerblastResult.class).map(FingerblastResult::getCandidateFingerprints).orElse(null));
            formRes.getAnnotationOrThrow(FormulaScoring.class)
                    .setAnnotation(TopCSIScore.class, structRes.getAnnotation(FingerblastResult.class).map(FingerblastResult::getTopHitScore).orElse(null));

            formRes.getAnnotationOrThrow(FormulaScoring.class)
                    .setAnnotation(ConfidenceScore.class, structRes.getAnnotation(ConfidenceResult.class).map(x -> x.score).orElse(null));

            // write results
            inst.updateFormulaResult(formRes,
                    FormulaScoring.class, FingerprintResult.class, FBCandidates.class, FBCandidateFingerprints.class);
        }
    }

    @Override
    protected Class<? extends DataAnnotation>[] formulaResultComponentsToClear() {
        return new Class[]{FTree.class, FBCandidates.class, FBCandidateFingerprints.class};
    }
}
