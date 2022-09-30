/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.utils.NetUtils;
import de.unijena.bioinf.chemdb.DataSource;
import de.unijena.bioinf.chemdb.annotations.StructureSearchDB;
import de.unijena.bioinf.elgordo.InjectElGordoCompounds;
import de.unijena.bioinf.fingerid.blast.BayesnetScoring;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.ms.annotations.AnnotationJJob;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.ms.rest.model.covtree.CovtreeJobInput;
import de.unijena.bioinf.ms.webapi.WebJJob;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

// FingerID Scheduler job does not manage dependencies between different  tools.
// this is done by the respective subtooljobs in the frontend
public class FingerblastJJob extends BasicMasterJJob<List<FingerIdResult>> {
    public static final boolean enableConfidence = useConfidenceScore();

    private static boolean useConfidenceScore() {
        boolean useIt = PropertyManager.getBoolean("de.unijena.bioinf.fingerid.confidence", true);
        if (!useIt)
            LoggerFactory.getLogger(FingerblastJJob.class).warn("===> CONFIDENCE SCORE IS DISABLED VIA PROPERTY! <===");
        return useIt;
    }

    // scoring provider
    private final CSIPredictor predictor;
    // input data
    private Ms2Experiment experiment;
    private List<FingerIdResult> idResult;

    List<WebJJob<CovtreeJobInput, ?, BayesnetScoring, ?>> covtreeJobs = new ArrayList<>();

    public FingerblastJJob(@NotNull CSIPredictor predictor) {
        this(predictor, null);
    }

    public FingerblastJJob(@NotNull CSIPredictor predictor, @Nullable Ms2Experiment experiment) {
        this(predictor, experiment, null);
    }

    public FingerblastJJob(@NotNull CSIPredictor predictor, @Nullable Ms2Experiment experiment, @Nullable List<FingerIdResult> idResult) {
        super(JobType.SCHEDULER);
        this.predictor = predictor;
        this.experiment = experiment;
        this.idResult = idResult;
    }

    public void setInput(Ms2Experiment experiment, List<FingerIdResult> idResult) {
        notSubmittedOrThrow();
        this.experiment = experiment;
        this.idResult = idResult;
    }


    public void setFingerIdResults(List<FingerIdResult> results) {
        notSubmittedOrThrow();
        this.idResult = results;
    }

    public void setExperiment(Ms2Experiment experiment) {
        notSubmittedOrThrow();
        this.experiment = experiment;
    }

    public Ms2Experiment getExperiment() {
        return experiment;
    }

    @Override
    protected List<FingerIdResult> compute() throws Exception {
        logDebug("Instance '" + experiment.getName() + "': Starting CSI:FingerID Computation.");
        if ((experiment.getPrecursorIonType().getCharge() > 0) != (predictor.predictorType.isPositive()))
            throw new IllegalArgumentException("Charges of predictor and instance are not equal");

        if (this.idResult == null || this.idResult.isEmpty()) {
            logWarn("No suitable input fingerprints found.");
            return List.of();
        }

        final StructureSearchDB searchDB = experiment.getAnnotationOrThrow(StructureSearchDB.class);

        logDebug("Preparing CSI:FingerID structure db search jobs.");
        ////////////////////////////////////////
        //submit jobs for db search
        ///////////////////////////////////////
        final Map<AnnotationJJob<?, FingerIdResult>, FingerIdResult> annotationJJobs = new LinkedHashMap<>(idResult.size());

        // formula job: retrieve fingerprint candidates for specific MF;
        // no SubmitSubJob needed because ist is not a CPU or IO job
        final List<FormulaJob> formulaJobs = idResult.stream().map(fingeridInput ->
                new FormulaJob(
                        fingeridInput.getMolecularFormula(),
                        predictor.database,
                        searchDB.searchDBs,
                        fingeridInput.getPrecursorIonType(),
                        true,
                        experiment.getAnnotation(InjectElGordoCompounds.class)
                                .orElse(InjectElGordoCompounds.TRUE).value ? DataSource.LIPID.flag : 0)
        ).collect(Collectors.toList());

        submitSubJobsInBatches(formulaJobs, PropertyManager.getNumberOfThreads());

        checkForInterruption();

        final ConfidenceJJob confidenceJJob = (predictor.getConfidenceScorer() != null) && enableConfidence
                ? new ConfidenceJJob(predictor, experiment)
                : null;

        for (int i = 0; i < idResult.size(); i++) {
            final FingerIdResult fingeridInput = idResult.get(i);

            // fingerblast job: score candidate fingerprints against predicted fingerprint
            final BayesnetScoring bayesnetScoring = NetUtils.tryAndWait(() ->
                            predictor.csiWebAPI.getBayesnetScoring(predictor.predictorType, fingeridInput.getMolecularFormula()),
                    this::checkForInterruption);


            final FingerblastSearchJJob blastJob;
            if (bayesnetScoring != null) {
                blastJob = FingerblastSearchJJob.of(predictor, bayesnetScoring, fingeridInput);
            } else {
                // bayesnetScoring is null --> make a prepare job which computes the bayessian network (covTree) for the
                // given molecular formula
                blastJob = FingerblastSearchJJob.of(predictor, fingeridInput);
                WebJJob<CovtreeJobInput, ?, BayesnetScoring, ?> covTreeJob = NetUtils.tryAndWait(() ->
                                predictor.csiWebAPI.submitCovtreeJob(fingeridInput.getMolecularFormula(), predictor.predictorType),
                        this::checkForInterruption);
                blastJob.addRequiredJob(covTreeJob);
                covtreeJobs.add(covTreeJob);
            }

            blastJob.addRequiredJob(formulaJobs.get(i));
            if (confidenceJJob != null)
                confidenceJJob.addRequiredJob(blastJob);
            annotationJJobs.put(submitSubJob(blastJob), fingeridInput);
        }

        // confidence job: calculate confidence of scored candidate list that are MERGED among ALL
        // IdentificationResults results with none empty candidate lists.
        if (confidenceJJob != null)
            submitSubJob(confidenceJJob);

        logDebug("Searching structure DB with CSI:FingerID");
        /////////////////////////////////////////////////
        //collect results and annotate to CSI:FingerID Result
        /////////////////////////////////////////////////
        // Sub Jobs should also be usable without annotation stuff -> So we annotate outside the subjobs compute methods
        // this loop is just to ensure that we wait until all jobs are finished.
        // the logic if a job can fail or not is done via job dependencies (see above)
        checkForInterruption();
        annotationJJobs.forEach(AnnotationJJob::takeAndAnnotateResult);
        checkForInterruption();

        if (confidenceJJob != null) {
            try {
                ConfidenceResult confidenceRes = confidenceJJob.awaitResult();
                if (confidenceRes.topHit != null) {
                    FingerIdResult topHit = annotationJJobs.values().stream().distinct()
                            .filter(fpr -> !fpr.getFingerprintCandidates().isEmpty()).max(Comparator.comparing(fpr -> fpr.getFingerprintCandidates().get(0))).orElse(null);
                    if (topHit != null) {
                        if (topHit.getFingerprintCandidates().get(0).getCandidate().getInchiKey2D().equals(confidenceRes.topHit.getCandidate().getInchiKey2D()))
                            topHit.setAnnotation(ConfidenceResult.class, confidenceRes);
                        else
                            logWarn("TopHit does not Match Confidence Result TopHit!'" + confidenceRes.topHit.getCandidate().getInchiKey2D() + "' vs '" + topHit.getFingerprintCandidates().get(0).getCandidate().getInchiKey2D() + "'.  Confidence might be lost!");
                    } else {
                        logWarn("No TopHit found for. But confidence was calculated for: '" + confidenceRes.topHit.getCandidate().getInchiKey2D() + "'.  Looks like a Bug. Confidence might be lost!");
                    }
                } else {
                    logWarn("No Confidence computed.");
                }
            } catch (ExecutionException e) {
                logWarn("Could not compute confidence, due to and Error!", e);
            }
        }


        logDebug("CSI:FingerID structure DB Search DONE!");
        //in linked maps values() collection is not a set -> so we have to make that distinct
        return annotationJJobs.values().stream().distinct().collect(Collectors.toList());
    }

    @Override
    public void cancel(boolean mayInterruptIfRunning) {
        super.cancel(mayInterruptIfRunning);
        covtreeJobs.forEach(c -> c.cancel(mayInterruptIfRunning));
    }

    @Override
    protected void cleanup() {
        super.cleanup();
        covtreeJobs = null;
    }

    @Override
    public String identifier() {
        return super.identifier() + " | " + experiment.getName() + "@" + experiment.getIonMass() + "m/z";
    }
}
