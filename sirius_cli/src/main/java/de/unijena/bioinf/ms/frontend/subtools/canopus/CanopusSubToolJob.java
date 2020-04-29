package de.unijena.bioinf.ms.frontend.subtools.canopus;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.fingerid.CanopusResult;
import de.unijena.bioinf.fingerid.CanopusWebJJob;
import de.unijena.bioinf.fingerid.FingerprintResult;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.jjobs.JobSubmitter;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.InstanceJob;
import de.unijena.bioinf.ms.frontend.utils.PicoUtils;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusData;
import de.unijena.bioinf.projectspace.FormulaScoring;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.projectspace.canopus.CanopusDataProperty;
import de.unijena.bioinf.projectspace.sirius.FormulaResult;
import de.unijena.bioinf.utils.NetUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class CanopusSubToolJob extends InstanceJob {

    public CanopusSubToolJob(JobSubmitter submitter) {
        super(submitter);
    }

    @Override
    public boolean isAlreadyComputed(@NotNull Instance inst) {
        return inst.loadCompoundContainer().hasResult() && inst.loadFormulaResults(CanopusResult.class).stream().anyMatch((it -> it.getCandidate().hasAnnotation(CanopusResult.class)));
    }

    @Override
    protected void computeAndAnnotateResult(final @NotNull Instance inst) throws Exception {
        //skip negative data
        if (inst.getID().getIonType().orElseGet(() -> inst.getExperiment().getPrecursorIonType()).isNegative()){
            logWarn("Skipping because NEGATIVE ion mode data is currently not supported by CANOPUS.");
            return;
        }

        List<? extends SScored<FormulaResult, ? extends FormulaScore>> input = inst.loadFormulaResults(FormulaScoring.class, FingerprintResult.class, CanopusResult.class);

        // create input
        List<FormulaResult> res = input.stream().map(SScored::getCandidate)
                .filter(ir -> ir.hasAnnotation(FingerprintResult.class)).collect(Collectors.toList());

        // check for valid input
        if (res.isEmpty()) {
            logInfo("Skipping because there are no formula results available");
            return;
        }

        // submit canopus jobs for Identification results that contain CSI:FingerID results
        Map<FormulaResult, CanopusWebJJob> jobs = res.stream().collect(Collectors.toMap(r -> r, this::buildAndSubmitRemote));

        jobs.forEach((k, v) -> k.setAnnotation(CanopusResult.class, v.takeResult()));

        // write Canopus client data
        if (inst.getProjectSpaceManager().getProjectSpaceProperty(CanopusDataProperty.class).isEmpty()) {
            final CanopusData pos = NetUtils.tryAndWait(() -> ApplicationCore.WEB_API.getCanopusdData(PredictorType.CSI_FINGERID_POSITIVE), this::checkForInterruption);
            final CanopusData neg = NetUtils.tryAndWait(() -> ApplicationCore.WEB_API.getCanopusdData(PredictorType.CSI_FINGERID_NEGATIVE), this::checkForInterruption);
            inst.getProjectSpaceManager().setProjectSpaceProperty(CanopusDataProperty.class, new CanopusDataProperty(pos, neg));
        }

        // write canopus results
        for (FormulaResult r : res)
            inst.updateFormulaResult(r, CanopusResult.class);
    }

    private CanopusWebJJob buildAndSubmitRemote(@NotNull final FormulaResult ir) {
        try {
            return NetUtils.tryAndWait(() -> ApplicationCore.WEB_API.submitCanopusJob(
                    ir.getId().getMolecularFormula(), ir.getId().getIonType().getCharge(), ir.getAnnotationOrThrow(FingerprintResult.class).fingerprint
                    ), this::checkForInterruption
            );
        } catch (TimeoutException | InterruptedException e) {
            throw new RuntimeException(e);
        }
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
