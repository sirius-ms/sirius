/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

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
import de.unijena.bioinf.jjobs.JobSubmitter;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.InstanceJob;
import de.unijena.bioinf.ms.frontend.utils.PicoUtils;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import de.unijena.bioinf.projectspace.FormulaScoring;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.projectspace.fingerid.FingerIdDataProperty;
import de.unijena.bioinf.projectspace.sirius.FormulaResult;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.utils.NetUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Subtooljob for CSI:FingerID
 * good exaple for how to create such a job
 */
public class FingeridSubToolJob extends InstanceJob {

    public static final boolean enableConfidence = PropertyManager.getBoolean("de.unijena.bioinf.fingerid.confidence", false);

    public FingeridSubToolJob(JobSubmitter submitter) {
        super(submitter);
    }

    @Override
    public boolean isAlreadyComputed(@NotNull Instance inst) {
        return inst.loadCompoundContainer().hasResult() && inst.loadFormulaResults(FingerprintResult.class, FBCandidates.class).stream().map(SScored::getCandidate).anyMatch(c -> c.hasAnnotation(FingerprintResult.class) && c.hasAnnotation(FBCandidates.class));/*{
            logInfo("Skipping CSI:FingerID for Instance \"" + inst.getExperiment().getName() + "\" because results already exist or result list is empty.");
            return;
        }*/
    }

    @Override
    protected void computeAndAnnotateResult(final @NotNull Instance inst) throws Exception {
        List<? extends SScored<FormulaResult, ? extends FormulaScore>> formulaResults =
                inst.loadFormulaResults(FormulaScoring.class, FTree.class, FingerprintResult.class, FBCandidates.class);

        if (formulaResults == null || formulaResults.isEmpty()) {
            logInfo("Skipping instance \"" + inst.getExperiment().getName() + "\" because there are no trees computed.");
            return;
        }

        PredictorTypeAnnotation type = inst.getExperiment().getAnnotationOrThrow(PredictorTypeAnnotation.class);

        //todo currently there is only csi -> change if there are multiple methods
        // we need to run multiple structure elucidation jobs and need  different prediction results then.
        EnumSet<PredictorType> predictors = type.toPredictors(inst.getExperiment().getPrecursorIonType().getCharge());
        final @NotNull CSIPredictor csi = NetUtils.tryAndWait(() -> (CSIPredictor) ApplicationCore.WEB_API.getStructurePredictor(predictors.iterator().next()), this::checkForInterruption);

        final FingerIDJJob<?> job = new FingerIDJJob<>(csi, inst.getExperiment(),
                formulaResults.stream().map(res -> new IdentificationResult<>(res.getCandidate().getAnnotationOrThrow(FTree.class), res.getScoreObject())).collect(Collectors.toList()),
                enableConfidence);

        // do computation and await results
        List<FingerIdResult> result = submitJob(job).awaitResult();

        final Map<FTree, FormulaResult> formulaResultsMap = formulaResults.stream().collect(Collectors.toMap(r -> r.getCandidate().getAnnotationOrThrow(FTree.class), SScored::getCandidate));

        // add CSIClientData to PS if it is not already there
        if (inst.getProjectSpaceManager().getProjectSpaceProperty(FingerIdDataProperty.class).isEmpty()) {
            final FingerIdData pos = NetUtils.tryAndWait(() -> ApplicationCore.WEB_API.getFingerIdData(PredictorType.CSI_FINGERID_POSITIVE), this::checkForInterruption);
            final FingerIdData neg = NetUtils.tryAndWait(() -> ApplicationCore.WEB_API.getFingerIdData(PredictorType.CSI_FINGERID_NEGATIVE), this::checkForInterruption);
            inst.getProjectSpaceManager().setProjectSpaceProperty(FingerIdDataProperty.class, new FingerIdDataProperty(pos, neg));
        }

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

        submitJobsInBatchesByThreads(tanimotoJobs, SiriusJobs.getCPUThreads()).forEach(JJob::getResult);

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

    @Override
    public String getToolName() {
        return PicoUtils.getCommand(FingerIdOptions.class).name();
    }
}
