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
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.chemdb.RestWithCustomDatabase;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.confidence_score.ConfidenceScorer;
import de.unijena.bioinf.jjobs.BasicDependentJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.ms.annotations.AnnotationJJob;
import de.unijena.bioinf.sirius.IdentificationResult;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;


/**
 * Created by martin on 08.08.18.
 */
public class ConfidenceJJob extends BasicDependentJJob<ConfidenceResult> implements AnnotationJJob<ConfidenceResult, FingerIdResult> {

    //fina inputs
    protected final ConfidenceScorer scorer;
    protected final Ms2Experiment experiment;
    protected final IdentificationResult<?> siriusidresult;

    // inputs that can be either set by a setter or from dependent jobs
    private List<Scored<FingerprintCandidate>> allScoredCandidates = null;
    private List<Scored<FingerprintCandidate>> requestedScoredCandidates = null;
    private ProbabilityFingerprint predictedFpt = null;
    private RestWithCustomDatabase.CandidateResult candidates = null;

//    private FingerblastJJob searchDBJob = null;
//    private FingerblastJJob additionalPubchemDBJob = null;

    //INPUT
    // puchem resultlist
    // filterflag oder filtered list
    // ConfidenceScoreComputor
    // Scorings: CovarianceScoring, CSIFingerIDScoring (reuse)
    // IdentificationResult
    // Experiment -> CollisionEnergies


    //OUTPUT
    // ConfidenceResult -> Annotate to

    public ConfidenceJJob(@NotNull CSIPredictor predictor, Ms2Experiment experiment, IdentificationResult<?> siriusResult) {
        this(predictor.getConfidenceScorer(), experiment, siriusResult);
    }

    public ConfidenceJJob(@NotNull ConfidenceScorer scorer, Ms2Experiment experiment, IdentificationResult<?> siriusResult) {
        super(JobType.CPU);
        this.scorer = scorer;
        this.experiment = experiment;
        this.siriusidresult = siriusResult;
    }

    protected void checkInput() {
        if (allScoredCandidates == null || siriusidresult == null || predictedFpt == null)
            throw new IllegalArgumentException("No Input Data found.");
    }

    @Override
    public synchronized void handleFinishedRequiredJob(JJob required) {
        if (required instanceof FingerblastJJob) {
            FingerblastJJob searchDBJob = (FingerblastJJob) required;
            predictedFpt = searchDBJob.fp;
            allScoredCandidates = searchDBJob.getAllScoredCandidates();
            candidates = searchDBJob.getCandidates();
            requestedScoredCandidates = searchDBJob.result().getResults();
        }
    }

    public void setAllScoredCandidates(List<Scored<FingerprintCandidate>> allScoredCandidates) {
        this.allScoredCandidates = allScoredCandidates;
    }

    public void setCandidates(RestWithCustomDatabase.CandidateResult candidates) {
        this.candidates = candidates;
    }

    public void setPredictedFpt(ProbabilityFingerprint predictedFpt) {
        this.predictedFpt = predictedFpt;
    }

    @Override
    protected ConfidenceResult compute() throws Exception {
        checkInput();

        final List<Scored<FingerprintCandidate>> allRestDbScoredCandidates = candidates.getAllDbCandidatesInChIs().map(set ->
                allScoredCandidates.stream().filter(sc -> set.contains(sc.getCandidate().getInchiKey2D())).collect(Collectors.toList())).
                orElseThrow(() -> new IllegalArgumentException("Additional candidates Flag 'ALL' from DataSource is not Available but mandatory to compute Confidence scores!"));

        final double score = scorer.computeConfidence(experiment, siriusidresult, allRestDbScoredCandidates, requestedScoredCandidates, predictedFpt);

        return new ConfidenceResult(score, requestedScoredCandidates.size() > 0 ? requestedScoredCandidates.get(0) : null);
    }
}
