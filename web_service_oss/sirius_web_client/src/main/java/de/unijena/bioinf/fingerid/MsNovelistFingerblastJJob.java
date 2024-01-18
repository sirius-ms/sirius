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
import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.fingerid.blast.BayesnetScoring;
import de.unijena.bioinf.fingerid.blast.Fingerblast;
import de.unijena.bioinf.fingerid.blast.MsNovelistFingerblastResult;
import de.unijena.bioinf.fingerid.blast.parameters.ParameterStore;
import de.unijena.bioinf.fingerid.fingerprints.FixedFingerprinter;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.ms.rest.model.covtree.CovtreeJobInput;
import de.unijena.bioinf.ms.rest.model.msnovelist.MsNovelistCandidate;
import de.unijena.bioinf.ms.webapi.WebJJob;
import de.unijena.bioinf.webapi.WebAPI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

public class MsNovelistFingerblastJJob extends BasicMasterJJob<List<Scored<FingerprintCandidate>>> {

    private final WebAPI<?> webAPI;
    private final CSIPredictor predictor;
    // input data
    private final FingerIdResult idResult;
    private final List<MsNovelistCandidate> candidates;

    public MsNovelistFingerblastJJob(@NotNull CSIPredictor predictor, @NotNull WebAPI<?> webAPI) throws IOException {
        this(predictor, webAPI, null);
    }

    public MsNovelistFingerblastJJob(@NotNull CSIPredictor predictor, @NotNull WebAPI<?> webAPI, @Nullable FingerIdResult idResult) throws IOException {
        this(predictor, webAPI, idResult, null);
    }

    public MsNovelistFingerblastJJob(@NotNull CSIPredictor predictor, @NotNull WebAPI<?> webAPI, @Nullable FingerIdResult idResult, @Nullable List<MsNovelistCandidate> candidates) throws IOException {
        super(JobType.SCHEDULER);
        this.predictor = predictor;
        this.idResult = idResult;
        this.webAPI = webAPI;
        this.candidates = candidates;
    }

    protected void checkInput() {
        if (idResult == null)
            throw new IllegalArgumentException("No Input Data found.");
        if (candidates == null)
            throw new IllegalArgumentException("No MSNovelist predictions found.");
    }

    @Override
    protected List<Scored<FingerprintCandidate>> compute() throws Exception {

        checkInput();

        if (candidates.isEmpty())
            return Collections.emptyList();

        BayesnetScoring bayesnetScoring = webAPI.getBayesnetScoring(
                predictor.predictorType,
                webAPI.getFingerIdData(predictor.predictorType),
                idResult.getMolecularFormula());

        checkForInterruption();

        // bayesnetScoring is null --> make a prepare job which computes the bayessian network (covTree) for the
        // given molecular formula
        if (bayesnetScoring == null) {
            WebJJob<CovtreeJobInput, ?, BayesnetScoring, ?> covTreeJob =
                    webAPI.submitCovtreeJob(idResult.getMolecularFormula(), predictor.predictorType);
            covTreeJob.awaitResult();
            bayesnetScoring = covTreeJob.getResult();
        }

        checkForInterruption();

        // turn MSNovelist candidate list to scoring-compatible FingerprintCandidates
        Collection<FingerprintCandidate> combinedCandidates = new ArrayList<>(Collections.emptyList());
        FixedFingerprinter fixedFingerprinter = new FixedFingerprinter(webAPI.getCDKChemDBFingerprintVersion());
        FingerIdData fingerIdData = idResult.getPrecursorIonType().getCharge() > 0 ? webAPI.getFingerIdData(PredictorType.CSI_FINGERID_POSITIVE) : webAPI.getFingerIdData(PredictorType.CSI_FINGERID_NEGATIVE);
        MaskedFingerprintVersion fpMask = fingerIdData.getFingerprintVersion();

        combinedCandidates.addAll(candidates.stream()
                .map(candidate -> {
                    FingerprintCandidate fingerprintCandidate = new FingerprintCandidate(
                            new InChI("", ""),
                            Objects.requireNonNull(fpMask.mask(fixedFingerprinter.computeFingerprintFromSMILES(
                                    candidate.getSmiles()))));
                    fingerprintCandidate.setSmiles(candidate.getSmiles());
                    return fingerprintCandidate;
                })
                .toList());

        checkForInterruption();

        // to get a prepared FingerblastScorer, an object of BayesnetScoring that is specific to the molecular formula
        // has to be initialized
        ProbabilityFingerprint fp = idResult.getPredictedFingerprint();

        List<JJob<List<Scored<FingerprintCandidate>>>> scoreJobs = Fingerblast.makeScoringJobs(
                predictor.getPreparedFingerblastScorer(ParameterStore.of(fp, bayesnetScoring)), combinedCandidates, fp);

        checkForInterruption();
        scoreJobs.forEach(this::submitSubJob);
        checkForInterruption();

        List<Scored<FingerprintCandidate>> scoredCandidates = scoreJobs.stream().flatMap(r -> r.takeResult().stream())
                .sorted(Comparator.reverseOrder())
                .map(fpc -> new Scored<>(fpc.getCandidate(), fpc.getScore())).collect(Collectors.toList());

        checkForInterruption();

        // annotate result of BayesNet scoring
        idResult.addAnnotation(MsNovelistFingerblastResult.class, new MsNovelistFingerblastResult(scoredCandidates));
        return scoredCandidates;
    }
}
