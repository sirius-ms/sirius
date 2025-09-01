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

package de.unijena.bioinf.ms.frontend.subtools.canopus;

import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.canopus.CanopusResult;
import de.unijena.bioinf.fingerid.FingerprintResult;
import de.unijena.bioinf.jjobs.JobSubmitter;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.InstanceJob;
import de.unijena.bioinf.ms.frontend.utils.PicoUtils;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusJobInput;
import de.unijena.bioinf.ms.webapi.WebJJob;
import de.unijena.bioinf.projectspace.FCandidate;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.rest.NetUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CanopusSubToolJob extends InstanceJob {
    private Map<FCandidate<?>, WebJJob<CanopusJobInput, ?, CanopusResult, ?>> jobs;
    public CanopusSubToolJob(JobSubmitter submitter) {
        super(submitter);
        asWEBSERVICE();
    }

    @Override
    public boolean isAlreadyComputed(@NotNull Instance inst) {
        return inst.hasCanopusResult();
    }

    @Override
    protected void computeAndAnnotateResult(final @NotNull Instance inst) throws Exception {
        List<FCandidate<?>> inputData = inst.getCanopusInput().stream()
                .filter(c -> c.hasAnnotation(FingerprintResult.class))
                .toList();

        checkForInterruption();


        // check for valid input
        if (inputData.isEmpty()) {
            logInfo("Skipping because there are no formula results available");
            return;
        }

        checkFingerprintCompatibilityOrThrow();

        updateProgress(10);
        checkForInterruption();

        NetUtils.tryAndWait(() -> inst.getProjectSpaceManager().writeCanopusDataIfMissing(ApplicationCore.WEB_API()), this::checkForInterruption);

        updateProgress(20);
        checkForInterruption();
        // spec has to count compounds
        final int specHash = Spectrums.mergeSpectra(inst.getExperiment().getMs2Spectra()).hashCode();
        updateProgress(25);

        // submit canopus jobs for Identification results that contain CSI:FingerID results
        jobs = inputData.stream().collect(Collectors.toMap(r -> r, ir -> buildAndSubmitRemote(ir, specHash)));
        updateProgress(30);


        checkForInterruption();
        jobs.forEach((k, v) -> k.setAnnotation(CanopusResult.class, v.takeResult()));
        updateProgress(80);

        // write canopus results
        inst.saveCanopusResult(inputData);
        updateProgress(97);
    }

    private WebJJob<CanopusJobInput, ?, CanopusResult, ?> buildAndSubmitRemote(@NotNull final FCandidate<?> ir, int specHash)  {
        try {
            return ApplicationCore.WEB_API().submitCanopusJob(
                    ir.getMolecularFormula(), ir.getAdduct().getCharge(),
                    ir.getAnnotationOrThrow(FingerprintResult.class).fingerprint, specHash
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void cancel(boolean mayInterruptIfRunning) {
        super.cancel(mayInterruptIfRunning);
        if (jobs != null)
            jobs.values().forEach(j -> j.cancel(mayInterruptIfRunning));
    }

    @Override
    protected void cleanup() {
        super.cleanup();
        jobs = null;
    }

    @Override
    public String getToolName() {
        return PicoUtils.getCommand(CanopusOptions.class).name();
    }
}
