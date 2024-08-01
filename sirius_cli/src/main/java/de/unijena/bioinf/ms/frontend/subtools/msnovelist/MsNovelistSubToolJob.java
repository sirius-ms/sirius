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

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.fp.Tanimoto;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.fingerid.*;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.JobSubmitter;
import de.unijena.bioinf.jjobs.Partition;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.InstanceJob;
import de.unijena.bioinf.ms.frontend.utils.PicoUtils;
import de.unijena.bioinf.ms.rest.model.msnovelist.MsNovelistJobInput;
import de.unijena.bioinf.ms.rest.model.msnovelist.MsNovelistJobOutput;
import de.unijena.bioinf.ms.webapi.WebJJob;
import de.unijena.bioinf.projectspace.FCandidate;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.rest.NetUtils;
import it.unimi.dsi.fastutil.Pair;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Subtooljob for Novelist candidate prediction
 */
public class MsNovelistSubToolJob extends InstanceJob {

    private Map<FCandidate<?>, WebJJob<MsNovelistJobInput, ?, MsNovelistJobOutput, ?>> msnJobs;

    public MsNovelistSubToolJob(JobSubmitter submitter) {
        super(submitter);
        asWEBSERVICE();
    }

    @Override
    public boolean isAlreadyComputed(@NotNull Instance inst) {
        return inst.hasMsNovelistResult();
    }

    @Override
    protected void computeAndAnnotateResult(final @NotNull Instance inst) throws Exception {
        final List<FCandidate<?>> inputData = inst.getMsNovelistInput().stream()
                .filter(c -> c.hasAnnotation(FingerprintResult.class))
                .filter(c -> c.hasAnnotation(FTree.class))
                .peek(c -> c.annotate(c.asFingerIdResult()))
                .toList();

        checkForInterruption();

        if (inputData.isEmpty()) {
            logInfo("Skipping instance \"" + inst.getName() + "\" because there are no formula candidates with tree and fingerprint data.");
            return;
        }

        checkFingerprintCompatibilityOrThrow();

        checkForInterruption();

        // add CSIClientData to PS if it is not already there
        NetUtils.tryAndWait(() -> inst.getProjectSpaceManager().writeFingerIdDataIfMissing(ApplicationCore.WEB_API), this::checkForInterruption);

        updateProgress(10);
        checkForInterruption();

        final int specHash = Spectrums.mergeSpectra(inst.getExperiment().getMs2Spectra()).hashCode();

        updateProgress(15);
        checkForInterruption();


        updateProgress(20);
        checkForInterruption();

        // submit prediction jobs; order of prediction from worker(s) will not matter as we use map
        msnJobs = inputData.stream().collect(Collectors.toMap(ir -> ir,
                        ir -> buildAndSubmitRemote(ir, specHash)
                ));

        updateProgress(25);
        checkForInterruption();

        // collect predictions; order of prediction from worker(s) does not matter as we used map
        Map<FCandidate<?>, MsNovelistJobOutput> msnJobResults =
                msnJobs.entrySet().stream().collect(Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> entry.getValue().takeResult()
                ));

        updateProgress(40);
        checkForInterruption();
        //needed to create bayes net scoring... better solution possible
        //todo msnovelist does just need a scorer so no need for the predictor if the scring function is initialized manually in MSNovelist job
        final @NotNull CSIPredictor csi = NetUtils.tryAndWait(() -> (CSIPredictor)
                        ApplicationCore.WEB_API.getStructurePredictor(inst.getIonType().getCharge()),
                this::checkForInterruption);

        updateProgress(45);
        checkForInterruption();

        // build scoring jobs
        // order of completion will not matter as we annotate results directly in MsNovelistFingerblastJJob
        List<MsNovelistFingerblastJJob> jobList = new ArrayList<>(Collections.emptyList());
        for (FCandidate<?> formRes : msnJobResults.keySet()) {
            final MsNovelistFingerblastJJob job = new MsNovelistFingerblastJJob(
                    csi,
                    ApplicationCore.WEB_API,
                    ApplicationCore.IFP_CACHE(),
                    formRes.getAnnotationOrThrow(FingerIdResult.class),
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
            inputData.stream().map(fc -> fc.getAnnotationOrThrow(FingerIdResult.class))
                    .filter(it -> it.hasAnnotation(FingerprintResult.class) && it.hasAnnotation(MsNovelistFingerblastResult.class))
                    .forEach(it -> {
                        final ProbabilityFingerprint fp = it.getPredictedFingerprint();
                        it.getMsNovelistFingerprintCandidates().stream().map(SScored::getCandidate)
                                .forEach(candidate -> tanimotoJobs.add(Pair.of(fp, candidate)));
                    });

            updateProgress(75);
            checkForInterruption();

            List<BasicJJob<Void>> jobs = Partition.ofNumber(tanimotoJobs, 2 * SiriusJobs.getCPUThreads())
                    .stream().map(l -> new BasicJJob<Void>(JobType.CPU) {
                        @Override
                        protected Void compute() {
                            l.forEach(p -> p.second().setTanimoto(
                                    Tanimoto.nonProbabilisticTanimoto(p.second().getFingerprint(), p.first())));
                            return null;
                        }
                    }).collect(Collectors.toList());

            jobs.forEach(this::submitJob);
            updateProgress(80);
            jobs.forEach(JJob::getResult);

            updateProgress(90);
            checkForInterruption();
        }

        inst.saveMsNovelistResult(inputData);


        updateProgress(97);
    }

    @Override
    public void cancel(boolean mayInterruptIfRunning) {
        super.cancel(mayInterruptIfRunning);
        if (msnJobs != null)
            msnJobs.values().forEach(j -> j.cancel(mayInterruptIfRunning));
    }

    @Override
    protected void cleanup() {
        super.cleanup();
        msnJobs = null;
    }

    @Override
    public boolean needsProperIonizationMode() {
        return true;
    }

    private WebJJob<MsNovelistJobInput, ?, MsNovelistJobOutput, ?> buildAndSubmitRemote(@NotNull final FCandidate<?> ir, int specHash) {
        try {
            return ApplicationCore.WEB_API.submitMsNovelistJob(
                    ir.getMolecularFormula(), ir.getAdduct().getCharge(),
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
