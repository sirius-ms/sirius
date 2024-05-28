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

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.canopus.CanopusResult;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.chemdb.DataSource;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.chemdb.annotations.StructureSearchDB;
import de.unijena.bioinf.confidence_score.ConfidenceScoreApproximateDistance;
import de.unijena.bioinf.confidence_score.ExpansiveSearchConfidenceMode;
import de.unijena.bioinf.elgordo.TagStructuresByElGordo;
import de.unijena.bioinf.fingerid.blast.BayesnetScoring;
import de.unijena.bioinf.fingerid.blast.FBCandidates;
import de.unijena.bioinf.fingerid.blast.FingerblastResult;
import de.unijena.bioinf.fingerid.blast.parameters.ParameterStore;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusJobInput;
import de.unijena.bioinf.ms.rest.model.covtree.CovtreeJobInput;
import de.unijena.bioinf.ms.webapi.WebJJob;
import de.unijena.bioinf.rest.NetUtils;
import de.unijena.bioinf.webapi.WebAPI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

// FingerID Scheduler job does not manage dependencies between different  tools.
// this is done by the respective subtooljobs in the frontend
public class FingerblastJJob extends BasicMasterJJob<List<FingerIdResult>> {
    public static final boolean enableConfidence = useConfidenceScore();
    private final WebAPI<?> webAPI;

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
    private List<FingerIdResult> idResults;

    private StructureSearchResult structureSearchResult;

    Set<WebJJob> webJJobs = new HashSet<>();

    public FingerblastJJob(@NotNull CSIPredictor predictor, @NotNull WebAPI<?> webAPI) {
        this(predictor, webAPI, null);
    }

    public FingerblastJJob(@NotNull CSIPredictor predictor, @NotNull WebAPI<?> webAPI, @Nullable Ms2Experiment experiment) {
        this(predictor, webAPI, experiment, null);
    }

    public FingerblastJJob(@NotNull CSIPredictor predictor, @NotNull WebAPI<?> webAPI, @Nullable Ms2Experiment experiment, @Nullable List<FingerIdResult> idResults) {
        super(JobType.SCHEDULER);
        this.predictor = predictor;
        this.experiment = experiment;
        this.idResults = idResults;
        this.webAPI = webAPI;
    }

    public void setInput(Ms2Experiment experiment, List<FingerIdResult> idResult) {
        notSubmittedOrThrow();
        this.experiment = experiment;
        this.idResults = idResult;
    }


    public void setFingerIdResults(List<FingerIdResult> results) {
        notSubmittedOrThrow();
        this.idResults = results;
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

        if (this.idResults == null || this.idResults.isEmpty()) {
            logWarn("No suitable input fingerprints found.");
            return List.of();
        }

        //Distance <= value for which structures are considered identical for approximate mode confidence score
        final ConfidenceScoreApproximateDistance confScoreApproxDist = experiment.getAnnotationOrNull(ConfidenceScoreApproximateDistance.class);

        //Expansive search confidence mode. OFF= No expansive search, Exact = Use exact conf score, Approx = Use approximate conf score
        final ExpansiveSearchConfidenceMode expansiveSearchConfidenceMode = experiment.getAnnotationOrNull(ExpansiveSearchConfidenceMode.class);

        final StructureSearchDB searchDB = experiment.getAnnotationOrThrow(StructureSearchDB.class);


        logDebug("Preparing CSI:FingerID structure db search jobs.");
        ArrayList<FingerblastSearchJJob> searchJJobs = new ArrayList<>();

        {
            // formula job: retrieve fingerprint candidates for specific MF;
            // no SubmitSubJob needed because ist is not a CPU or IO job
            final List<FormulaJob> formulaJobs = idResults.stream().map(fingeridInput ->
                    new FormulaJob(
                            fingeridInput.getMolecularFormula(),
                            predictor.database,
                            searchDB.searchDBs,
                            fingeridInput.getPrecursorIonType(),
                            true,
                            experiment.getAnnotation(TagStructuresByElGordo.class)
                                    .orElse(TagStructuresByElGordo.TRUE).value ? DataSource.LIPID.flag : 0)
            ).collect(Collectors.toList());

            submitSubJobsInBatches(formulaJobs, jobManager.getCPUThreads());

            checkForInterruption();


            final BayesnetScoring[] scorings = NetUtils.tryAndWait(() -> {
                BayesnetScoring[] s = new BayesnetScoring[idResults.size()];
                webAPI.executeBatch((api, client) -> {
                    for (int i = 0; i < idResults.size(); i++) {
                        try {//interrupt if canceled
                            checkForInterruption();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        final FingerIdResult fingeridInput = idResults.get(i);
                        // fingerblast job: score candidate fingerprints against predicted fingerprint
                        s[i] = api.fingerprintClient().getCovarianceScoring(
                                predictor.predictorType,
                                predictor.getFingerprintVersion(),
                                fingeridInput.getMolecularFormula(),
                                predictor.getPerformances(),
                                client
                        );
                    }
                });
                return s;
            }, this::checkForInterruption);

            {
                //compute missing trees
                WebJJob<CovtreeJobInput, ?, BayesnetScoring, ?>[] bayesJobs = new WebJJob[scorings.length];
                for (int i = 0; i < scorings.length; i++) {
                    if (scorings[i] == null) {
                        logInfo("Starting new BayesTree Job.");
                        bayesJobs[i] = webAPI.submitCovtreeJob(idResults.get(i).getMolecularFormula(), predictor.predictorType);
                        webJJobs.add(bayesJobs[i]);
                    }
                }


                // Loop over all fingerprints and start SearchJjobs for each one. Jobs gets saved in searchJJobs, jobFidResult maps a searchjob back to the FingerIDresult
                for (int i = 0; i < idResults.size(); i++) {
                    final FingerIdResult fingeridInput = idResults.get(i);

                    if (scorings[i] == null) {
                        if (bayesJobs[i] == null)
                            throw new IllegalStateException("Expected bayes tree job missing.");
                        scorings[i] = bayesJobs[i].awaitResult();
                        webJJobs.remove(bayesJobs[i]);
                    }
                    final FingerblastSearchJJob blastJob = FingerblastSearchJJob.of(predictor, scorings[i], fingeridInput);
                    searchJJobs.add(blastJob);

                    blastJob.addRequiredJob(formulaJobs.get(i));
                    submitJob(blastJob); //no submitsubjob because we are waiting for web job.
                }
            }
        }
        //search job are now prepared and submitted


        final ArrayList<Scored<FingerprintCandidate>> allMergedCandidates = new ArrayList<>();
        final ArrayList<Scored<FingerprintCandidate>> requestedMergedCandidates = new ArrayList<>();

        Double topHitScoreRequested = null, topHitScoreAll = null;
        ProbabilityFingerprint topHitFPRequested = null, topHitFPAll = null;
        FTree topHitTreeRequested = null, topHitTreeAll = null;
        MolecularFormula topHitFormulaRequested = null, topHitFormulaAll = null;
        BayesnetScoring topHitScoringRequested = null, topHitScoringAll = null;
        CanopusResult topFormulaCanopusResultRequested = null, topFormulaCanopusResultAll = null;
        FingerIdResult topHitFidResultRequested = null, topHitFidResultAll = null;




        /*
         * Wait for all search jobs to finish and generate the merged ranked lists. MCES, Epi and conf score jobs all need this combined list
         *
         */
        ConfidenceJJob confidenceJJobRequested;
        HashMap<FTree, FBCandidates> fTreeCandidatesMapAll = new HashMap<>();
        {
            HashMap<FTree, FBCandidates> fTreeCandidatesMapRequested = new HashMap<>();
            for (int i = 0; i < searchJJobs.size(); i++) {
                FingerblastSearchJJob searchDBJob = searchJJobs.get(i);
                FingerblastResult r = searchDBJob.awaitResult();

                final List<Scored<FingerprintCandidate>> allRestDbScoredCandidates = searchDBJob.getCandidates().getAllDbCandidatesInChIs().map(set ->
                                searchDBJob.getAllScoredCandidates().stream().filter(sc -> set.contains(sc.getCandidate().getInchiKey2D())).collect(Collectors.toList())).
                        orElseThrow(() -> new IllegalArgumentException("Additional candidates Flag 'ALL' from DataSource is not Available but mandatory to compute Confidence scores!"));


                allMergedCandidates.addAll(allRestDbScoredCandidates);
                requestedMergedCandidates.addAll(r.getResults());

                fTreeCandidatesMapRequested.put(searchDBJob.ftree, new FBCandidates(r.getResults().stream().map(fc -> new Scored<CompoundCandidate>(fc.getCandidate().toCompoundCandidate(), fc.getScore())).collect(Collectors.toList())));
                fTreeCandidatesMapAll.put(searchDBJob.ftree, new FBCandidates(allRestDbScoredCandidates.stream().map(fc -> new Scored<CompoundCandidate>(fc.getCandidate().toCompoundCandidate(), fc.getScore())).collect(Collectors.toList())));

                if (r.getTopHitScore() != null) {

                    if (topHitScoreRequested == null || topHitScoreRequested < r.getTopHitScore().score()) {
                        topHitScoreRequested = r.getTopHitScore().score();
                        topHitFPRequested = searchDBJob.fp;
                        topHitTreeRequested = searchDBJob.ftree;
                        topHitFidResultRequested = idResults.get(i);
                        topHitFormulaRequested = searchDBJob.formula;
                        topHitScoringRequested = searchDBJob.bayesnetScoring;
                        topFormulaCanopusResultRequested = idResults.get(i).getAnnotationOrThrow(CanopusResult.class);
                    }
                }

                if (allRestDbScoredCandidates != null && !allRestDbScoredCandidates.isEmpty()) {

                    if (topHitScoreAll == null || topHitScoreAll < allRestDbScoredCandidates.get(0).getScore()) {
                        topHitScoreAll = allRestDbScoredCandidates.get(0).getScore();
                        topHitFPAll = searchDBJob.fp;
                        topHitTreeAll = searchDBJob.ftree;
                        topHitFormulaAll = searchDBJob.formula;
                        topHitFidResultAll = idResults.get(i);
                        topHitScoringAll = searchDBJob.bayesnetScoring;
                        topFormulaCanopusResultAll = idResults.get(i).getAnnotationOrThrow(CanopusResult.class);
                    }
                }
            }


            allMergedCandidates.sort(Comparator.<Scored<FingerprintCandidate>>reverseOrder().thenComparing((Scored<FingerprintCandidate> s) -> s.getCandidate().getInchiKey2D()));

            requestedMergedCandidates.sort(Comparator.<Scored<FingerprintCandidate>>reverseOrder().thenComparing((Scored<FingerprintCandidate> s) -> s.getCandidate().getInchiKey2D()));

            //We always submit this job, regardless of expansive search
            ParameterStore parameterStoreRequested = (topHitFPRequested == null || topHitScoringRequested == null || topHitTreeRequested == null || topHitFormulaRequested == null) ? null : ParameterStore.of(topHitFPRequested, topHitScoringRequested, topHitTreeRequested, topHitFormulaRequested);

            confidenceJJobRequested = executeConfidenceStack(requestedMergedCandidates, allMergedCandidates, fTreeCandidatesMapRequested, confScoreApproxDist.value, searchDB, parameterStoreRequested, topFormulaCanopusResultRequested);
        }


        /*
         *
         * If Expansive search is not OFF, we have to do it all again for PubChem aka ALL. We still need to compute approximate AND exact confidences here.
         *
         */
        ConfidenceResult finalConfidenceResult = null;

        if (!expansiveSearchConfidenceMode.confidenceScoreSimilarityMode.equals(ExpansiveSearchConfidenceMode.Mode.OFF)) {

            StructureSearchDB searchDBFake = StructureSearchDB.fromString("PUBCHEM");


            ParameterStore parameterStoreAll = (topHitFPAll == null || topHitScoringAll == null || topHitTreeAll == null || topHitFormulaAll == null) ? null : ParameterStore.of(topHitFPAll, topHitScoringAll, topHitTreeAll, topHitFormulaAll);

            ConfidenceJJob confidenceJJobAll = executeConfidenceStack(allMergedCandidates, allMergedCandidates, fTreeCandidatesMapAll, confScoreApproxDist.value, searchDBFake, parameterStoreAll, topFormulaCanopusResultAll);
            fTreeCandidatesMapAll = null; //just try to reduce memory consumption


            ConfidenceResult confidenceResultRequested = confidenceJJobRequested != null ? confidenceJJobRequested.awaitResult() : ConfidenceResult.NaN;
            confidenceJJobRequested = null;
            ConfidenceResult confidenceResultAll = confidenceJJobAll != null ? confidenceJJobAll.awaitResult() : ConfidenceResult.NaN;
            confidenceJJobAll = null;

            /**
             * expansive search decision happens here
             */

            if (expansiveSearchConfidenceMode.confidenceScoreSimilarityMode.equals(ExpansiveSearchConfidenceMode.Mode.EXACT)) {
                if (confidenceResultAll.score.score() * expansiveSearchConfidenceMode.confPubChemFactor > confidenceResultRequested.score.score()) {
                    //All wins over requested
                    structureSearchResult = StructureSearchResult.of(confidenceResultAll, expansiveSearchConfidenceMode.confidenceScoreSimilarityMode);
                    finalConfidenceResult = confidenceResultAll;
                } else {
                    structureSearchResult = StructureSearchResult.of(confidenceResultRequested, ExpansiveSearchConfidenceMode.Mode.OFF);
                    finalConfidenceResult = confidenceResultRequested;
                }
            } else if (expansiveSearchConfidenceMode.confidenceScoreSimilarityMode.equals(ExpansiveSearchConfidenceMode.Mode.APPROXIMATE)) {
                if (confidenceResultAll.scoreApproximate.score() * expansiveSearchConfidenceMode.confPubChemFactor > confidenceResultRequested.scoreApproximate.score()) {
                    //All wins over requested
                    structureSearchResult = StructureSearchResult.of(confidenceResultAll, expansiveSearchConfidenceMode.confidenceScoreSimilarityMode);
                    finalConfidenceResult = confidenceResultAll;
                } else {
                    structureSearchResult = StructureSearchResult.of(confidenceResultRequested, ExpansiveSearchConfidenceMode.Mode.OFF);
                    finalConfidenceResult = confidenceResultRequested;
                }
            }


        } else {
            ConfidenceResult confidenceResultRequested = confidenceJJobRequested != null ? confidenceJJobRequested.awaitResult() : ConfidenceResult.NaN;
            structureSearchResult = StructureSearchResult.of(confidenceResultRequested, expansiveSearchConfidenceMode.confidenceScoreSimilarityMode);
            finalConfidenceResult = confidenceResultRequested;
        }


        // confidence job: calculate confidence of scored candidate list that are MERGED among ALL
        // IdentificationResults results with none empty candidate lists.

        logDebug("Searching structure DB with CSI:FingerID");
        /////////////////////////////////////////////////
        //collect results and annotate to CSI:FingerID Result
        /////////////////////////////////////////////////
        // Sub Jobs should also be usable without annotation stuff -> So we annotate outside the subjobs compute methods
        // this loop is just to ensure that we wait until all jobs are finished.
        // the logic if a job can fail or not is done via job dependencies (see above)
        checkForInterruption();


        //if expansive search expanded the results to "All", we need to annotate the ALL fingerblast result. If not, then regular requested
        boolean isExpanded = !structureSearchResult.getExpansiveSearchConfidenceMode().equals(ExpansiveSearchConfidenceMode.Mode.OFF);

        for (int i = 0; i < searchJJobs.size(); i++) {
            FingerblastSearchJJob job = searchJJobs.get(i);
            FingerblastResult r = isExpanded ? new FingerblastResult(job.getCandidates().getAllDbCandidatesInChIs().map(set ->
                            job.getAllScoredCandidates().stream().filter(sc -> set.contains(sc.getCandidate().getInchiKey2D())).collect(Collectors.toList())).
                    orElseThrow(() -> new IllegalArgumentException("Additional candidates Flag 'ALL' from DataSource is not Available but mandatory to compute Confidence scores!"))) : job.result(); //result already present
            idResults.get(i).setAnnotation(FingerblastResult.class, r);
        }

        checkForInterruption();

        if (finalConfidenceResult != null) {
            if (finalConfidenceResult.topHit != null) {
                FingerIdResult topHit = isExpanded ? topHitFidResultAll : topHitFidResultRequested;
                if (topHit != null) {
                    if (topHit.getFingerprintCandidates().get(0).getCandidate().getInchiKey2D().equals(finalConfidenceResult.topHit.getCandidate().getInchiKey2D())) {
                        topHit.setAnnotation(ConfidenceResult.class, finalConfidenceResult);
                        topHit.setAnnotation(StructureSearchResult.class, structureSearchResult);
                    } else
                        logWarn("TopHit does not Match Confidence Result TopHit!'" + finalConfidenceResult.topHit.getCandidate().getInchiKey2D() + "' vs '" + topHit.getFingerprintCandidates().get(0).getCandidate().getInchiKey2D() + "'.  Confidence might be lost!");
                } else {
                    logWarn("No TopHit found for. But confidence was calculated for: '" + finalConfidenceResult.topHit.getCandidate().getInchiKey2D() + "'.  Looks like a Bug. Confidence might be lost!");
                }
            } else {
                logWarn("No Confidence computed.");
            }
        }


        logDebug("CSI:FingerID structure DB Search DONE!");
        //in linked maps values() collection is not a set -> so we have to make that distinct
        return idResults;
    }


    private ConfidenceJJob executeConfidenceStack(ArrayList<Scored<FingerprintCandidate>> requestedMergedCandidates,
                                                  ArrayList<Scored<FingerprintCandidate>> allMergedCandidates,
                                                  HashMap<FTree, FBCandidates> fTreeCandidatesMap,
                                                  int confScoreApproxDist,
                                                  StructureSearchDB searchDB,
                                                  ParameterStore parameterStore,
                                                  CanopusResult topFormulaCanopusResult) throws InterruptedException {

        if (requestedMergedCandidates.isEmpty() || parameterStore == null) return null;

        try {
            checkForInterruption();
            //Start and finish MCES job for requested DBs here, since Epi and conf are dependent on the mces-condensed list
            final MCESJJob mcesJJobRequested = new MCESJJob(confScoreApproxDist, requestedMergedCandidates);
            submitJob(mcesJJobRequested);
            int mcesIndexRequested = mcesJJobRequested.awaitResult();

            checkForInterruption();


            //MCES-condensed list for requested
            ArrayList<Scored<FingerprintCandidate>> requestedMergedCandidatesMCESCondensed = new ArrayList<>();
            requestedMergedCandidatesMCESCondensed.add(requestedMergedCandidates.get(0));
            Map<String, Double> removedCandidatesrequested = requestedMergedCandidates.subList(1, mcesIndexRequested + 1).stream().collect(Collectors.toMap(c -> c.getCandidate().getInchiKey2D(), Scored<FingerprintCandidate>::getScore));
            requestedMergedCandidatesMCESCondensed.addAll(requestedMergedCandidates.subList(mcesIndexRequested + 1, requestedMergedCandidates.size()));


            checkForInterruption();

            //Submit epi jobs for requested databases
            //epi Job for <exact, requested>
            final SubstructureAnnotationJJob epiJJobExactRequested = new SubstructureAnnotationJJob(requestedMergedCandidates.size() >= 5 ? 5 : requestedMergedCandidates.size() >= 2 ? 2 : requestedMergedCandidates.size() >= 1 ? 1 : 0);
            epiJJobExactRequested.setInput(fTreeCandidatesMap);
            submitJob(epiJJobExactRequested);

            checkForInterruption();

            //epi job for <approximate, requested>. Remove candidate from ftreeCandidatesMap that are within MCES distance of approximate mode
            final SubstructureAnnotationJJob epiJJobApproximateRequested = new SubstructureAnnotationJJob(requestedMergedCandidatesMCESCondensed.size() >= 5 ? 5 : requestedMergedCandidatesMCESCondensed.size() >= 2 ? 2 : requestedMergedCandidatesMCESCondensed.size() >= 1 ? 1 : 0);
            HashMap<FTree, FBCandidates> fTreeCandidatesMapMCESCondensedRequested = new HashMap<>();
            for (FTree t : fTreeCandidatesMap.keySet()) {
                List<Scored<CompoundCandidate>> filteredCandidates = fTreeCandidatesMap.get(t).getResults().stream().filter(a -> !removedCandidatesrequested.containsKey(a.getCandidate().getInchiKey2D())).collect(Collectors.toList());
                fTreeCandidatesMapMCESCondensedRequested.put(t, new FBCandidates(filteredCandidates));
            }
            epiJJobApproximateRequested.setInput(fTreeCandidatesMapMCESCondensedRequested);
            submitJob(epiJJobApproximateRequested);

            checkForInterruption();

            //todo can probably be optimized by starting them before other local computations
            final int specHash = Spectrums.mergeSpectra(experiment.getMs2Spectra()).hashCode();
            WebJJob<CanopusJobInput, ?, CanopusResult, ?> canopusWebJJob = webAPI.submitCanopusJob(
                    requestedMergedCandidates.get(0).getCandidate().getInchi().extractFormulaOrThrow(),
                    experiment.getPrecursorIonType().getCharge(),
                    requestedMergedCandidates.get(0).getCandidate().getFingerprint().asProbabilistic(),
                    specHash);
            webJJobs.add(canopusWebJJob);

            checkForInterruption();

            CanopusResult canopusResultTopHit = canopusWebJJob.awaitResult();
            webJJobs.remove(canopusWebJJob);

            checkForInterruption();


            //confidence job for requested
            final ConfidenceJJob confidenceJJobRequested = (predictor.getConfidenceScorer() != null) && enableConfidence
                    ? new ConfidenceJJob(predictor, experiment, allMergedCandidates, requestedMergedCandidates, requestedMergedCandidatesMCESCondensed, searchDB, parameterStore, topFormulaCanopusResult, canopusResultTopHit, mcesIndexRequested)
                    : null;

            //we use result because it is non blocking...
            confidenceJJobRequested.setEpiExact(epiJJobExactRequested::result);
            confidenceJJobRequested.setEpiApprox(epiJJobApproximateRequested::result);

            confidenceJJobRequested.addRequiredJob(epiJJobExactRequested);
            confidenceJJobRequested.addRequiredJob(epiJJobApproximateRequested);

            checkForInterruption();

            return submitJob(confidenceJJobRequested);
        } catch (ExecutionException | IOException e) {
            logError("Couldn't compute confidence Job", e);
            return null;
        }

    }

    @Override
    public void cancel(boolean mayInterruptIfRunning) {
        super.cancel(mayInterruptIfRunning);
        if (webJJobs != null)
            webJJobs.forEach(c -> c.cancel(mayInterruptIfRunning));
    }

    @Override
    protected void cleanup() {
        super.cleanup();
        webJJobs = null;
    }

    @Override
    public String identifier() {
        return super.identifier() + " | " + experiment.getName() + "@" + experiment.getIonMass() + "m/z";
    }
}
