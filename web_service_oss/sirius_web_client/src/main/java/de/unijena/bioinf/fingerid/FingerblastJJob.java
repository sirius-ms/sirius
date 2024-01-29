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
import de.unijena.bioinf.ms.annotations.AnnotationJJob;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusJobInput;
import de.unijena.bioinf.ms.rest.model.covtree.CovtreeJobInput;
import de.unijena.bioinf.ms.webapi.WebJJob;
import de.unijena.bioinf.rest.NetUtils;
import de.unijena.bioinf.webapi.WebAPI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.util.*;
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
    private List<FingerIdResult> idResult;

    private StructureSearchResult structureSearchResult;


    private List<CanopusResult> canopusResult;

    List<WebJJob<CovtreeJobInput, ?, BayesnetScoring, ?>> covtreeJobs = new ArrayList<>();

    public FingerblastJJob(@NotNull CSIPredictor predictor, @NotNull WebAPI<?> webAPI) {
        this(predictor, webAPI, null);
    }

    public FingerblastJJob(@NotNull CSIPredictor predictor, @NotNull WebAPI<?> webAPI, @Nullable Ms2Experiment experiment) {
        this(predictor, webAPI, experiment, null,null);
    }

    public FingerblastJJob(@NotNull CSIPredictor predictor, @NotNull WebAPI<?> webAPI, @Nullable Ms2Experiment experiment, @Nullable List<FingerIdResult> idResult, @Nullable List<CanopusResult> canopusResult) {
        super(JobType.SCHEDULER);
        this.predictor = predictor;
        this.experiment = experiment;
        this.idResult = idResult;
        this.webAPI = webAPI;
        this.canopusResult=canopusResult;
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

        //Distance <= value for which structures are considered identical for approximate mode confidence score
        final ConfidenceScoreApproximateDistance confScoreApproxDist =  experiment.getAnnotationOrNull(ConfidenceScoreApproximateDistance.class);

        //Expansive search confidence mode. OFF= No expansive search, Exact = Use exact conf score, Approx = Use approximate conf score
        final ExpansiveSearchConfidenceMode expansiveSearchConfidenceMode = experiment.getAnnotationOrNull(ExpansiveSearchConfidenceMode.class);

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
                        experiment.getAnnotation(TagStructuresByElGordo.class)
                                .orElse(TagStructuresByElGordo.TRUE).value ? DataSource.LIPID.flag : 0)
        ).collect(Collectors.toList());

        submitSubJobsInBatches(formulaJobs, PropertyManager.getNumberOfThreads());

        checkForInterruption();


        final BayesnetScoring[] scorings = NetUtils.tryAndWait(() -> {
            BayesnetScoring[] s = new BayesnetScoring[idResult.size()];
            webAPI.executeBatch((api, client) -> {
                for (int i = 0; i < idResult.size(); i++) {
                    final FingerIdResult fingeridInput = idResult.get(i);
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


        ArrayList<FingerblastSearchJJob> searchJJobs = new ArrayList<>();

        for (int i = 0; i < idResult.size(); i++) {
            final FingerIdResult fingeridInput = idResult.get(i);

            final FingerblastSearchJJob blastJob;
            if (scorings[i] != null) {
                blastJob = FingerblastSearchJJob.of(predictor, scorings[i], fingeridInput);
                searchJJobs.add(blastJob);

            } else {
                // bayesnetScoring is null --> make a prepare job which computes the bayessian network (covTree) for the
                // given molecular formula
                blastJob = FingerblastSearchJJob.of(predictor, fingeridInput);
                searchJJobs.add(blastJob);
                WebJJob<CovtreeJobInput, ?, BayesnetScoring, ?> covTreeJob =
                        webAPI.submitCovtreeJob(fingeridInput.getMolecularFormula(), predictor.predictorType);
                blastJob.addRequiredJob(covTreeJob);
                covtreeJobs.add(covTreeJob);
            }

            blastJob.addRequiredJob(formulaJobs.get(i));

            annotationJJobs.put(submitJob(blastJob), fingeridInput);
        }

        final ArrayList<Scored<FingerprintCandidate>> allMergedCandidates = new ArrayList<>();
        final ArrayList<Scored<FingerprintCandidate>> requestedMergedCandidates = new ArrayList<>();

        Double topHitScoreRequested = null,topHitScoreAll = null;
        ProbabilityFingerprint topHitFPRequested = null, topHitFPAll = null;
        FTree topHitTreeRequested = null, topHitTreeAll = null;
        MolecularFormula topHitFormulaRequested = null, topHitFormulaAll = null;
        BayesnetScoring topHitScoringRequested = null, topHitScoringAll= null;
        CanopusResult topFormulaCanopusResultRequested = null, topFormulaCanopusResultAll=null;




        /**
         * Wait for all search jobs to finish and generate the merged ranked lists. MCES, Epi and conf score jobs all need this combined list
         *
         */

        HashMap<FTree, FBCandidates> fTreeCandidatesMapRequested = new HashMap<>();
        HashMap<FTree, FBCandidates> fTreeCandidatesMapAll = new HashMap<>();


        for (int i=0;i<searchJJobs.size();i++) {
            FingerblastSearchJJob searchDBJob =searchJJobs.get(i);
            FingerblastResult r = searchDBJob.awaitResult();
            final List<Scored<FingerprintCandidate>> allRestDbScoredCandidates = searchDBJob.getCandidates().getAllDbCandidatesInChIs().map(set ->
                            searchDBJob.getAllScoredCandidates().stream().filter(sc -> set.contains(sc.getCandidate().getInchiKey2D())).collect(Collectors.toList())).
                    orElseThrow(() -> new IllegalArgumentException("Additional candidates Flag 'ALL' from DataSource is not Available but mandatory to compute Confidence scores!"));


            allMergedCandidates.addAll(allRestDbScoredCandidates);
            requestedMergedCandidates.addAll(r.getResults());

            fTreeCandidatesMapRequested.put(searchDBJob.ftree,new FBCandidates(r.getResults().stream().map(fc -> new Scored<CompoundCandidate>(fc.getCandidate().toCompoundCandidate(),fc.getScore())).collect(Collectors.toList())));
            fTreeCandidatesMapAll.put(searchDBJob.ftree,new FBCandidates(allRestDbScoredCandidates.stream().map(fc -> new Scored<CompoundCandidate>(fc.getCandidate().toCompoundCandidate(),fc.getScore())).collect(Collectors.toList())));

            if(r.getTopHitScore()!=null){

            if (topHitScoreRequested == null || topHitScoreRequested < r.getTopHitScore().score()) {
                topHitScoreRequested = r.getTopHitScore().score();
                topHitFPRequested = searchDBJob.fp;
                topHitTreeRequested = searchDBJob.ftree;
                topHitFormulaRequested = searchDBJob.formula;
                topHitScoringRequested = searchDBJob.bayesnetScoring;
                topFormulaCanopusResultRequested = canopusResult.get(i);
            }}

            if(allMergedCandidates!=null) {

                if (topHitScoreAll == null || topHitScoreAll < allMergedCandidates.get(0).getScore()) {
                    topHitScoreAll = allMergedCandidates.get(0).getScore();
                    topHitFPAll = searchDBJob.fp;
                    topHitTreeAll = searchDBJob.ftree;
                    topHitFormulaAll = searchDBJob.formula;
                    topHitScoringAll = searchDBJob.bayesnetScoring;
                    topFormulaCanopusResultAll = canopusResult.get(i);
                }
            }


        }


        allMergedCandidates.sort(Comparator.reverseOrder());
        requestedMergedCandidates.sort(Comparator.reverseOrder());


        assert requestedMergedCandidates.get(0).getScore() == topHitScoreRequested;


        //Start and finish MCES job for requested DBs here, since Epi and conf are dependent on the mces-condensed list
        final MCESJJob mcesJJobRequested = new MCESJJob(confScoreApproxDist.value,requestedMergedCandidates);
        submitSubJob(mcesJJobRequested);
        int mcesIndexRequested = mcesJJobRequested.awaitResult();


        //MCES-condensed list for requested
        ArrayList<Scored<FingerprintCandidate>> requestedMergedCandidatesMCESCondensed = new ArrayList<>();
        requestedMergedCandidatesMCESCondensed.add(requestedMergedCandidates.get(0));
        Map removedCandidatesrequested = requestedMergedCandidates.subList(1,mcesIndexRequested+1).stream().collect(Collectors.toMap(c -> c.getCandidate().getInchiKey2D(),Scored<FingerprintCandidate>::getScore));
        requestedMergedCandidatesMCESCondensed.addAll(requestedMergedCandidates.subList(mcesIndexRequested+1,requestedMergedCandidates.size()));


        /**
         *
         * Submit epi jobs for requested databases
         */

        //epi Job for <exact, requested>
        final SubstructureAnnotationJJob epiJJobExactRequested =  new SubstructureAnnotationJJob(requestedMergedCandidates.size()>=5 ? 5 : requestedMergedCandidates.size()>=2 ? 2 : requestedMergedCandidates.size()>=1 ? 1 : 0);
        epiJJobExactRequested.setInput(fTreeCandidatesMapRequested);
        submitSubJob(epiJJobExactRequested);

        //epi job for <approximate, requested>. Remove candidate from ftreeCandidatesMap that are within MCES distance of approximate mode
        final SubstructureAnnotationJJob epiJJobApproximateRequested =  new SubstructureAnnotationJJob(requestedMergedCandidates.size()>=5 ? 5 : requestedMergedCandidates.size()>=2 ? 2 : requestedMergedCandidates.size()>=1 ? 1 : 0);
        HashMap<FTree, FBCandidates> fTreeCandidatesMapMCESCondensedRequested = new HashMap<>();
        for (FTree t :  fTreeCandidatesMapRequested.keySet()){
            List<Scored<CompoundCandidate>> filteredCandidates = fTreeCandidatesMapRequested.get(t).getResults().stream().filter(a -> !removedCandidatesrequested.containsKey(a.getCandidate().getInchiKey2D())).collect(Collectors.toList());
            fTreeCandidatesMapMCESCondensedRequested.put(t,new FBCandidates(filteredCandidates));
        }
        epiJJobApproximateRequested.setInput(fTreeCandidatesMapMCESCondensedRequested);
        submitSubJob(epiJJobApproximateRequested);

        //canopus job for requested

        final int specHash = Spectrums.mergeSpectra(experiment.getMs2Spectra()).hashCode();
        WebJJob<CanopusJobInput, ?, CanopusResult, ?> canopusJob = webAPI.submitCanopusJob(requestedMergedCandidates.get(0).getCandidate().getInchi().extractFormula(),experiment.getPrecursorIonType().getCharge(),requestedMergedCandidates.get(0).getCandidate().getFingerprint().asProbabilistic(),specHash);

        //confidence job for requested

        final ConfidenceJJob confidenceJJobRequested = (predictor.getConfidenceScorer() != null) && enableConfidence
                ? new ConfidenceJJob(predictor, experiment,allMergedCandidates,requestedMergedCandidates, requestedMergedCandidatesMCESCondensed,searchDB, ParameterStore.of(topHitFPRequested, topHitScoringRequested, topHitTreeRequested, topHitFormulaRequested),topFormulaCanopusResultRequested,mcesIndexRequested)
                : null;


        confidenceJJobRequested.setEpiExact(() -> epiJJobExactRequested.getResult());
        confidenceJJobRequested.setEpiApprox(() -> epiJJobApproximateRequested.getResult());
        confidenceJJobRequested.setCanopusResultTopHit(() -> canopusJob.getResult());

        confidenceJJobRequested.addRequiredJob(epiJJobExactRequested);
        confidenceJJobRequested.addRequiredJob(epiJJobApproximateRequested);
        confidenceJJobRequested.addRequiredJob(canopusJob);


        submitSubJob(confidenceJJobRequested);


        /**
         *
         * If Expansive search is not OFF, we have to do it all again for PubChem aka ALL. We still need to compute approximate AND exact confidences here.
         *
         */
        ConfidenceResult finalConfidenceResult = null;

        if(!expansiveSearchConfidenceMode.confidenceScoreSimilarityMode.equals(ExpansiveSearchConfidenceMode.Mode.OFF)) {

            StructureSearchDB searchDBFake=StructureSearchDB.fromString("PubChem");


            //Start and finish MCES job for All DBs here, since Epi and conf are dependent on the mces-condensed list
            final MCESJJob mcesJJobAll = new MCESJJob(confScoreApproxDist.value,allMergedCandidates);
            submitSubJob(mcesJJobAll);
            int mcesIndexAll = mcesJJobAll.awaitResult();

            //MCES-condensed list for all
            ArrayList<Scored<FingerprintCandidate>> allMergedCandidatesMCESCondensed = new ArrayList<>();
            allMergedCandidatesMCESCondensed.add(allMergedCandidates.get(0));
            Map<String, Double> removedCandidatesAll = allMergedCandidates.subList(1, mcesIndexAll + 1).stream().collect(Collectors.toMap(c -> c.getCandidate().getInchiKey2D(), Scored<FingerprintCandidate>::getScore));
            allMergedCandidatesMCESCondensed.addAll(allMergedCandidates.subList(mcesIndexAll + 1, allMergedCandidates.size()));




            //epi Job for <exact, all>
            final SubstructureAnnotationJJob epiJJobExactAll =  new SubstructureAnnotationJJob(allMergedCandidates.size()>=5 ? 5 : allMergedCandidates.size()>=2 ? 2 : allMergedCandidates.size()>=1 ? 1 : 0);
            epiJJobExactAll.setInput(fTreeCandidatesMapAll);
            submitSubJob(epiJJobExactAll);

            //epi job for <approximate, all>. Remove candidate from ftreeCandidatesMap that are within MCES distance of approximate mode
            final SubstructureAnnotationJJob epiJJobApproximateAll =  new SubstructureAnnotationJJob(allMergedCandidatesMCESCondensed.size()>=5 ? 5 : allMergedCandidatesMCESCondensed.size()>=2 ? 2 : allMergedCandidatesMCESCondensed.size()>=1 ? 1 : 0); //TODO pass correct top
            HashMap<FTree, FBCandidates> fTreeCandidatesMapMCESCondensedAll = new HashMap<>();
            for (FTree t :  fTreeCandidatesMapAll.keySet()){
                List<Scored<CompoundCandidate>> filteredCandidates = fTreeCandidatesMapAll.get(t).getResults().stream().filter(a -> !removedCandidatesAll.containsKey(a.getCandidate().getInchiKey2D())).collect(Collectors.toList());
                fTreeCandidatesMapMCESCondensedAll.put(t,new FBCandidates(filteredCandidates));
            }
            epiJJobApproximateAll.setInput(fTreeCandidatesMapMCESCondensedAll);
            submitSubJob(epiJJobApproximateAll);

            //canopus job for all

            WebJJob<CanopusJobInput, ?, CanopusResult, ?> canopusJobAll = webAPI.submitCanopusJob(allMergedCandidates.get(0).getCandidate().getInchi().extractFormula(),experiment.getPrecursorIonType().getCharge(),allMergedCandidates.get(0).getCandidate().getFingerprint().asProbabilistic(),specHash);

            //confidence job for all

            final ConfidenceJJob confidenceJJobAll= (predictor.getConfidenceScorer() != null) && enableConfidence
                    ? new ConfidenceJJob(predictor, experiment,allMergedCandidates,allMergedCandidates, allMergedCandidatesMCESCondensed,searchDBFake, ParameterStore.of(topHitFPAll, topHitScoringAll, topHitTreeAll, topHitFormulaAll),topFormulaCanopusResultAll,mcesIndexAll)
                    : null;


            confidenceJJobAll.setEpiExact(() -> epiJJobExactAll.getResult());
            confidenceJJobAll.setEpiApprox(() -> epiJJobApproximateAll.getResult());
            confidenceJJobAll.setCanopusResultTopHit(() -> canopusJobAll.getResult());

            confidenceJJobAll.addRequiredJob(epiJJobExactAll);
            confidenceJJobAll.addRequiredJob(epiJJobApproximateAll);
            confidenceJJobAll.addRequiredJob(canopusJobAll);


            submitSubJob(confidenceJJobAll);

            ConfidenceResult confidenceResultRequested  = confidenceJJobRequested.awaitResult();
            ConfidenceResult confidenceResultAll = confidenceJJobAll.awaitResult();

            /**
             * expansive search decision happens here
             */

            if (expansiveSearchConfidenceMode.confidenceScoreSimilarityMode.equals(ExpansiveSearchConfidenceMode.Mode.EXACT)){
                if(confidenceResultAll.score.score()*expansiveSearchConfidenceMode.confPubChemFactor>confidenceResultRequested.score.score()){
                    //All wins over requested
                    structureSearchResult= StructureSearchResult.of(confidenceResultAll ,expansiveSearchConfidenceMode.confidenceScoreSimilarityMode);
                    finalConfidenceResult = confidenceResultAll;
                }else{
                    structureSearchResult= StructureSearchResult.of(confidenceResultRequested, expansiveSearchConfidenceMode.confidenceScoreSimilarityMode);
                    finalConfidenceResult = confidenceResultRequested;
                }
            }else if (expansiveSearchConfidenceMode.confidenceScoreSimilarityMode.equals(ExpansiveSearchConfidenceMode.Mode.APPROXIMATE)){
                if(confidenceResultAll.scoreApproximate.score()*expansiveSearchConfidenceMode.confPubChemFactor>confidenceResultRequested.scoreApproximate.score()){
                    //All wins over requested
                    structureSearchResult= StructureSearchResult.of(confidenceResultAll, expansiveSearchConfidenceMode.confidenceScoreSimilarityMode);
                    finalConfidenceResult = confidenceResultAll;
                }else {
                    structureSearchResult= StructureSearchResult.of(confidenceResultRequested, expansiveSearchConfidenceMode.confidenceScoreSimilarityMode);
                    finalConfidenceResult = confidenceResultRequested;
                }
            }


        }else {
            ConfidenceResult confidenceResultRequested  = confidenceJJobRequested.awaitResult();
            structureSearchResult= StructureSearchResult.of(confidenceResultRequested,expansiveSearchConfidenceMode.confidenceScoreSimilarityMode);
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
        annotationJJobs.forEach(AnnotationJJob::takeAndAnnotateResult);
        checkForInterruption();

        if (finalConfidenceResult != null) {
            if (finalConfidenceResult.topHit != null) {
                FingerIdResult topHit = annotationJJobs.values().stream().distinct()
                        .filter(fpr -> !fpr.getFingerprintCandidates().isEmpty()).max(Comparator.comparing(fpr -> fpr.getFingerprintCandidates().get(0))).orElse(null);
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
        return annotationJJobs.values().stream().distinct().collect(Collectors.toList());
    }

    @Override
    public void cancel(boolean mayInterruptIfRunning) {
        super.cancel(mayInterruptIfRunning);
        if (covtreeJobs != null)
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
