/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.frontend.subtools.msnovelist;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.fp.Tanimoto;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.fingerid.*;
import de.unijena.bioinf.fingerid.blast.MsNovelistFBCandidateFingerprints;
import de.unijena.bioinf.fingerid.blast.MsNovelistFBCandidates;
import de.unijena.bioinf.fingerid.blast.TopMsNovelistScore;
import de.unijena.bioinf.fingerid.predictor_types.PredictorTypeAnnotation;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.JobSubmitter;
import de.unijena.bioinf.jjobs.Partition;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.InstanceJob;
import de.unijena.bioinf.ms.frontend.utils.PicoUtils;
import de.unijena.bioinf.ms.rest.model.msnovelist.MsNovelistJobInput;
import de.unijena.bioinf.ms.rest.model.msnovelist.MsNovelistJobOutput;
import de.unijena.bioinf.ms.webapi.WebJJob;
import de.unijena.bioinf.projectspace.FormulaResult;
import de.unijena.bioinf.projectspace.FormulaScoring;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.projectspace.ProjectSpaceManagers;
import de.unijena.bioinf.rest.NetUtils;
import org.apache.commons.math3.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Subtooljob for Novelist candidate prediction
 */
public class MsNovelistSubToolJob extends InstanceJob {

    public static final List<Class<? extends DataAnnotation>> formulaResultComponentsToClear = List.of(MsNovelistFBCandidates.class, MsNovelistFBCandidateFingerprints.class);

    public MsNovelistSubToolJob(JobSubmitter submitter) {
        super(submitter);
        asWEBSERVICE();
    }

    @Override
    public boolean isAlreadyComputed(@NotNull Instance inst) {
        return inst.loadCompoundContainer().hasResults() && inst.loadFormulaResults(MsNovelistFBCandidates.class).stream().map(SScored::getCandidate).anyMatch(c -> c.hasAnnotation(MsNovelistFBCandidates.class));
    }

    @Override
    protected void computeAndAnnotateResult(final @NotNull Instance inst) throws Exception {
        List<? extends SScored<FormulaResult, ? extends FormulaScore>> formulaResults =
                inst.loadFormulaResults(FormulaScoring.class, FTree.class, FingerprintResult.class);

        checkForInterruption();

        if (formulaResults == null || formulaResults.isEmpty()) {
            logInfo("Skipping instance \"" + inst.getExperiment().getName() + "\" because there are no trees computed.");
            return;
        }

        checkFingerprintCompatibilityOrThrow();

        checkForInterruption();

        // add CSIClientData to PS if it is not already there
        NetUtils.tryAndWait(() -> ProjectSpaceManagers.writeFingerIdDataIfMissing(inst.getProjectSpaceManager(), ApplicationCore.WEB_API), this::checkForInterruption);

        updateProgress(10);
        checkForInterruption();

        final int specHash = Spectrums.mergeSpectra(inst.getExperiment().getMs2Spectra()).hashCode();

        updateProgress(15);
        checkForInterruption();

        final Map<FormulaResult, FingerIdResult> formulaResultsMap = formulaResults.stream().map(SScored::getCandidate)
                .filter(res -> res.hasAnnotation(FingerprintResult.class))
                .collect(Collectors.toMap(res -> res, res -> {
                    FingerIdResult idr = new FingerIdResult(res.getAnnotationOrThrow(FTree.class));
                    idr.setAnnotation(FingerprintResult.class, res.getAnnotationOrThrow(FingerprintResult.class));
                    return idr;
                }));

        updateProgress(20);
        checkForInterruption();

        // submit prediction jobs; order of prediction from worker(s) will not matter as we use map
        Map<FormulaResult, WebJJob<MsNovelistJobInput, ?, MsNovelistJobOutput, ?>> msnJobs =
                formulaResultsMap.keySet().stream().collect(Collectors.toMap(ir -> ir,
                        ir -> buildAndSubmitRemote(ir, specHash)
                ));

        updateProgress(25);
        checkForInterruption();

        // collect predictions; order of prediction from worker(s) does not matter as we used map
        Map<FormulaResult, MsNovelistJobOutput> msnJobResults =
                msnJobs.entrySet().stream().collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> entry.getValue().takeResult()
                        ));

        updateProgress(40);
        checkForInterruption();

        final @NotNull CSIPredictor csi = NetUtils.tryAndWait(() -> (CSIPredictor)
                        ApplicationCore.WEB_API.getStructurePredictor(
                                inst.getExperiment().getAnnotationOrThrow(PredictorTypeAnnotation.class)
                                        .toPredictors(inst.getExperiment().getPrecursorIonType().getCharge()).iterator().next()),
                this::checkForInterruption);

        updateProgress(45);
        checkForInterruption();

        // build scoring jobs
        // order of completion will not matter as we annotate results directly in MsNovelistFingerblastJJob
        List<MsNovelistFingerblastJJob> jobList = new ArrayList<>(Collections.emptyList());
        for (FormulaResult formRes : msnJobResults.keySet().stream().toList()) {
            final MsNovelistFingerblastJJob job = new MsNovelistFingerblastJJob(
                    csi,
                    ApplicationCore.WEB_API,
                    formulaResultsMap.get(formRes),
                    msnJobResults.get(formRes).getCandidates());

            checkForInterruption();
            jobList.add(job);
        }

        jobList.forEach(this::submitSubJob);
        updateProgress(50);
        checkForInterruption();

        jobList.forEach(JJob::getResult);
        updateProgress(70);
        checkForInterruption();

        {
            //calculate and annotate tanimoto scores
            List<Pair<ProbabilityFingerprint, FingerprintCandidate>> tanimotoJobs = new ArrayList<>();
            formulaResultsMap.values().stream()
                    .filter(it -> it.hasAnnotation(FingerprintResult.class) && it.hasAnnotation(MsNovelistFingerblastResult.class))
                    .forEach(it -> {
                        final ProbabilityFingerprint fp = it.getPredictedFingerprint();
                        it.getMsNovelistFingerprintCandidates().stream().map(SScored::getCandidate)
                                .forEach(candidate -> tanimotoJobs.add(Pair.create(fp, candidate)));
                    });

            updateProgress(75);
            checkForInterruption();

            List<BasicJJob<Void>> jobs = Partition.ofNumber(tanimotoJobs, 2 * SiriusJobs.getCPUThreads())
                    .stream().map(l -> new BasicJJob<Void>(JobType.CPU) {
                        @Override
                        protected Void compute() {
                            l.forEach(p -> p.getSecond().setTanimoto(
                                    Tanimoto.nonProbabilisticTanimoto(p.getSecond().getFingerprint(), p.getFirst())));
                            return null;
                        }
                    }).collect(Collectors.toList());

            jobs.forEach(this::submitJob);
            updateProgress(80);
            jobs.forEach(JJob::getResult);

            updateProgress(90);
            checkForInterruption();
        }

        for (final FormulaResult formRes : formulaResultsMap.keySet()) {

            final FingerIdResult idResult = formulaResultsMap.get(formRes);
            // annotation check: only FingerIdResults that hold valid MSNovelist results are annotated in
            // MsNovelistFingerblastJJob
            if (idResult.hasAnnotation(MsNovelistFingerblastResult.class)) {
                // annotate result to FormulaResult
                formRes.setAnnotation(MsNovelistFBCandidates.class,
                        idResult.getAnnotation(MsNovelistFingerblastResult.class)
                                .map(MsNovelistFingerblastResult::getCandidates).orElse(null));
                formRes.setAnnotation(MsNovelistFBCandidateFingerprints.class, idResult.getAnnotation(MsNovelistFingerblastResult.class)
                        .map(MsNovelistFingerblastResult::getCandidateFingerprints).orElse(null));
                formRes.getAnnotationOrThrow(FormulaScoring.class)
                        .setAnnotation(TopMsNovelistScore.class, idResult.getAnnotation(MsNovelistFingerblastResult.class)
                                .map(MsNovelistFingerblastResult::getTopHitScore).orElse(null));
                // write results to project space
                inst.updateFormulaResult(formRes, FormulaScoring.class, MsNovelistFBCandidates.class, MsNovelistFBCandidateFingerprints.class);
            }
        }

        updateProgress(97);
    }

    private WebJJob<MsNovelistJobInput, ?, MsNovelistJobOutput, ?> buildAndSubmitRemote(@NotNull final FormulaResult ir, int specHash) {
        try {
            return ApplicationCore.WEB_API.submitMsNovelistJob(
                    ir.getId().getMolecularFormula(), ir.getId().getIonType().getCharge(),
                    ir.getAnnotationOrThrow(FingerprintResult.class).fingerprint, specHash
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getToolName() {
        return PicoUtils.getCommand(MsNovelistOptions.class).name();
    }
}
