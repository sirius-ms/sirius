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

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.chemdb.annotations.StructureSearchDB;
import de.unijena.bioinf.confidence_score.ConfidenceScorer;
import de.unijena.bioinf.fingerid.blast.*;
import de.unijena.bioinf.fingerid.blast.parameters.ParameterStore;
import de.unijena.bioinf.jjobs.BasicDependentMasterJJob;
import de.unijena.bioinf.jjobs.JJob;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;


/**
 * Created by martin on 08.08.18.
 */
public class ConfidenceJJob extends BasicDependentMasterJJob<ConfidenceResult> {

    //fina inputs
    protected final ConfidenceScorer confidenceScorer;
    protected final Ms2Experiment experiment;

    // inputs that can be either set by a setter or from dependent jobs
//    private List<Scored<FingerprintCandidate>> allScoredCandidates = null;
//    private List<Scored<FingerprintCandidate>> requestedScoredCandidates = null;
//    private ProbabilityFingerprint predictedFpt = null;
//    private RestWithCustomDatabase.CandidateResult candidates = null;
    protected final Set<FingerblastSearchJJob> inputInstances = new LinkedHashSet<>();
    protected final ScoringMethodFactory.CSIFingerIdScoringMethod csiScoring;


//    List<Scored<FingerprintCandidate>> allMergedRestDbCandidates;
//    List<FingerblastResult> fbResults = new ArrayList<>();
//    List<RestWithCustomDatabase.CandidateResult> fbRawResults = new ArrayList<>();

//    FingerblastJJob topHitJob = null;

//    private FingerblastJJob searchDBJob = null;
//    private FingerblastJJob additionalPubchemDBJob = null;

    //INPUT
    // puchem resultlist
    // filterflag oder filtered list
    // ConfidenceScoreComputer
    // Scorings: CovarianceScoring, CSIFingerIDScoring (reuse)
    // IdentificationResult
    // Experiment -> CollisionEnergies


    //OUTPUT
    // ConfidenceResult -> Annotate to

    public ConfidenceJJob(@NotNull CSIPredictor predictor, Ms2Experiment experiment) {
        super(JobType.CPU);
        this.confidenceScorer = predictor.getConfidenceScorer();
        this.csiScoring = new ScoringMethodFactory.CSIFingerIdScoringMethod(predictor.performances);
        this.experiment = experiment;

    }


    @Override
    public synchronized void handleFinishedRequiredJob(JJob required) {
        if (required instanceof FingerblastSearchJJob) {
            final FingerblastSearchJJob searchDBJob = (FingerblastSearchJJob) required;
            if (searchDBJob.result() != null && searchDBJob.result().getTopHitScore() != null) {
                inputInstances.add(searchDBJob);
            } else {
                if (searchDBJob.result() == null)
                    LoggerFactory.getLogger(getClass()).warn("Fingerblast Job '" + searchDBJob.identifier() + "' skipped because of result was null.");
            }
        }
    }


    @Override
    protected ConfidenceResult compute() throws Exception {
        checkForInterruption();
        if (inputInstances.isEmpty())
            return new ConfidenceResult(Double.NaN, null);

        Map<FingerblastSearchJJob, List<JJob<List<Scored<FingerprintCandidate>>>>> csiScoreJobs = new HashMap<>();


        final List<Scored<FingerprintCandidate>> allMergedCandidatesCov = new ArrayList<>();
        final List<Scored<FingerprintCandidate>> allMergedCandidatesCSI = new ArrayList<>();
        final List<Scored<FingerprintCandidate>> requestedMergedCandidatesCov = new ArrayList<>();
        final List<Scored<FingerprintCandidate>> requestedMergedCandidatesCSI = new ArrayList<>();


        Double topHitScore = null;
        ProbabilityFingerprint topHitFP = null;
        FTree topHitTree = null;
        MolecularFormula topHitFormula = null;
        BayesnetScoring topHitScoring = null;
        boolean structureSearchDBIsPubChem = ((experiment.getAnnotationOrNull(StructureSearchDB.class).getDBFlag() & 2L) !=0)? true : false;


        for (FingerblastSearchJJob searchDBJob : inputInstances) {
            FingerblastResult r = searchDBJob.result();
            final List<Scored<FingerprintCandidate>> allRestDbScoredCandidates = searchDBJob.getCandidates().getAllDbCandidatesInChIs().map(set ->
                    searchDBJob.getAllScoredCandidates().stream().filter(sc -> set.contains(sc.getCandidate().getInchiKey2D())).collect(Collectors.toList())).
                    orElseThrow(() -> new IllegalArgumentException("Additional candidates Flag 'ALL' from DataSource is not Available but mandatory to compute Confidence scores!"));


            allMergedCandidatesCov.addAll(allRestDbScoredCandidates);
            requestedMergedCandidatesCov.addAll(r.getResults());

            if (topHitScore == null || topHitScore < r.getTopHitScore().score()) {
                topHitScore = r.getTopHitScore().score();
                topHitFP = searchDBJob.fp;
                topHitTree = searchDBJob.ftree;
                topHitFormula = searchDBJob.formula;
                topHitScoring = searchDBJob.bayesnetScoring;
            }

            // build csi scoring jobs
//            csiScoring.getScoring().
            CSIFingerIdScoring scoring = csiScoring.getScoring();
            scoring.prepare(searchDBJob.fp);
            List<JJob<List<Scored<FingerprintCandidate>>>> j = Fingerblast.makeScoringJobs(scoring,
                    new ArrayList<>(searchDBJob.getCandidates().getCombCandidates()),
                    searchDBJob.fp);
            csiScoreJobs.put(searchDBJob, j);
            j.forEach(this::submitSubJob);
        }

        checkForInterruption();

        csiScoreJobs.forEach((k,v) -> {
            List<Scored<FingerprintCandidate>> combinedAllCSI = v.stream().map(JJob::takeResult).flatMap(Collection::stream).collect(Collectors.toList());
            Set<String> requestFilterSet = k.result().getResults().stream().map(SScored::getCandidate).map(FingerprintCandidate::getInchiKey2D).collect(Collectors.toSet());
            Set<String> allFilterSet = k.getCandidates().getAllDbCandidatesInChIs().map(HashSet::new).orElseThrow();
            List<Scored<FingerprintCandidate>> allCSI = combinedAllCSI.stream().filter(c -> allFilterSet.contains(c.getCandidate().getInchiKey2D())).collect(Collectors.toList());
            List<Scored<FingerprintCandidate>> requestCSI = combinedAllCSI.stream().filter(c -> requestFilterSet.contains(c.getCandidate().getInchiKey2D())).collect(Collectors.toList());
            allMergedCandidatesCSI.addAll(allCSI);
            requestedMergedCandidatesCSI.addAll(requestCSI);
        });

        checkForInterruption();

        csiScoreJobs.clear();
        inputInstances.clear();

        allMergedCandidatesCov.sort(Comparator.reverseOrder());
        requestedMergedCandidatesCov.sort(Comparator.reverseOrder());

        allMergedCandidatesCSI.sort(Comparator.reverseOrder());
        requestedMergedCandidatesCSI.sort(Comparator.reverseOrder());

        assert  allMergedCandidatesCov.size() == allMergedCandidatesCSI.size();
        assert  requestedMergedCandidatesCov.size() == requestedMergedCandidatesCSI.size();

        checkForInterruption();

        final double score = confidenceScorer.computeConfidence(experiment,
                allMergedCandidatesCov, allMergedCandidatesCSI,
                requestedMergedCandidatesCov, requestedMergedCandidatesCSI,
                ParameterStore.of(topHitFP, topHitScoring, topHitTree, topHitFormula),structureSearchDBIsPubChem);

        checkForInterruption();
        return new ConfidenceResult(score, requestedMergedCandidatesCov.size() > 0 ? requestedMergedCandidatesCov.get(0) : null);
    }

}
