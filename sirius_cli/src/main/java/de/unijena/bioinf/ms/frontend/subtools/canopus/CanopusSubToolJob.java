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

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.ChemistryBase.utils.NetUtils;
import de.unijena.bioinf.canopus.CanopusResult;
import de.unijena.bioinf.fingerid.FingerprintResult;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.jjobs.JobSubmitter;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.InstanceJob;
import de.unijena.bioinf.ms.frontend.utils.PicoUtils;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusCfData;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusJobInput;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusNpcData;
import de.unijena.bioinf.ms.webapi.WebJJob;
import de.unijena.bioinf.projectspace.FormulaResult;
import de.unijena.bioinf.projectspace.FormulaScoring;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.projectspace.canopus.CanopusCfDataProperty;
import de.unijena.bioinf.projectspace.canopus.CanopusNpcDataProperty;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CanopusSubToolJob extends InstanceJob {
    private Map<FormulaResult, WebJJob<CanopusJobInput, ?, CanopusResult, ?>> jobs;
    public CanopusSubToolJob(JobSubmitter submitter) {
        super(submitter);
        asWEBSERVICE();
    }

    @Override
    public boolean isAlreadyComputed(@NotNull Instance inst) {
        return inst.loadCompoundContainer().hasResults() && inst.loadFormulaResults(CanopusResult.class).stream().anyMatch((it -> it.getCandidate().hasAnnotation(CanopusResult.class)));
    }

    @Override
    protected void computeAndAnnotateResult(final @NotNull Instance inst) throws Exception {
        List<? extends SScored<FormulaResult, ? extends FormulaScore>> input = inst.loadFormulaResults(FormulaScoring.class, FingerprintResult.class, CanopusResult.class);

        checkForInterruption();

        // create input
        List<FormulaResult> res = input.stream().map(SScored::getCandidate)
                .filter(ir -> ir.hasAnnotation(FingerprintResult.class)).collect(Collectors.toList());

        // check for valid input
        if (res.isEmpty()) {
            logInfo("Skipping because there are no formula results available");
            return;
        }

        checkFingerprintCompatibilityOrThrow();

        updateProgress(10);
        checkForInterruption();

        //todo we might need a generic solution here because CANOPUS will predict many more stuff in the future.

        // write ClassyFire client data
        if (inst.getProjectSpaceManager().getProjectSpaceProperty(CanopusCfDataProperty.class).isEmpty()) {
            final CanopusCfData pos = NetUtils.tryAndWait(() -> ApplicationCore.WEB_API.getCanopusCfData(PredictorType.CSI_FINGERID_POSITIVE), this::checkForInterruption);
            final CanopusCfData neg = NetUtils.tryAndWait(() -> ApplicationCore.WEB_API.getCanopusCfData(PredictorType.CSI_FINGERID_NEGATIVE), this::checkForInterruption);
            inst.getProjectSpaceManager().setProjectSpaceProperty(new CanopusCfDataProperty(pos, neg));
        }

        // write NPC client data
        if (inst.getProjectSpaceManager().getProjectSpaceProperty(CanopusNpcDataProperty.class).isEmpty()) {
            final CanopusNpcData pos = NetUtils.tryAndWait(() -> ApplicationCore.WEB_API.getCanopusNpcData(PredictorType.CSI_FINGERID_POSITIVE), this::checkForInterruption);
            final CanopusNpcData neg = NetUtils.tryAndWait(() -> ApplicationCore.WEB_API.getCanopusNpcData(PredictorType.CSI_FINGERID_NEGATIVE), this::checkForInterruption);
            inst.getProjectSpaceManager().setProjectSpaceProperty(new CanopusNpcDataProperty(pos, neg));
        }

        updateProgress(20);
        checkForInterruption();
        // spec has to count compounds
        final int specHash = Spectrums.mergeSpectra(inst.getExperiment().getMs2Spectra()).hashCode();
        updateProgress(25);

        // submit canopus jobs for Identification results that contain CSI:FingerID results
        jobs = res.stream().collect(Collectors.toMap(r -> r, ir -> buildAndSubmitRemote(ir, specHash)));
        updateProgress(30);


        checkForInterruption();
        jobs.forEach((k, v) -> k.setAnnotation(CanopusResult.class, v.takeResult()));
        updateProgress(80);

        // write canopus results
        for (FormulaResult r : res)
            inst.updateFormulaResult(r, CanopusResult.class);
        updateProgress(97);
    }

    private WebJJob<CanopusJobInput, ?, CanopusResult, ?> buildAndSubmitRemote(@NotNull final FormulaResult ir, int specHash)  {
        try {
            return ApplicationCore.WEB_API.submitCanopusJob(
                    ir.getId().getMolecularFormula(), ir.getId().getIonType().getCharge(),
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
    protected Class<? extends DataAnnotation>[] formulaResultComponentsToClear() {
        return new Class[]{CanopusResult.class};
    }

    @Override
    public String getToolName() {
        return PicoUtils.getCommand(CanopusOptions.class).name();
    }
}
