package de.unijena.bioinf.ms.frontend.subtools.canopus;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.fingerid.CanopusResult;
import de.unijena.bioinf.fingerid.CanopusWebJJob;
import de.unijena.bioinf.fingerid.FingerprintResult;
import de.unijena.bioinf.fingerid.predictor_types.UserDefineablePredictorType;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.io.projectspace.Instance;
import de.unijena.bioinf.ms.frontend.subtools.InstanceJob;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusData;
import de.unijena.bioinf.projectspace.FormulaScoring;
import de.unijena.bioinf.projectspace.sirius.FormulaResult;
import de.unijena.bioinf.sirius.scores.SiriusScore;
import de.unijena.bioinf.utils.NetUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class CanopusSubToolJob extends InstanceJob {

    @Override
    protected void computeAndAnnotateResult(final @NotNull Instance inst) throws Exception {
//        System.out.println("I am Canopus on Experiment " + inst);
        List<? extends SScored<FormulaResult, ? extends FormulaScore>> input = inst.loadFormulaResults(
                SiriusScore.class,
                FormulaScoring.class, FingerprintResult.class, CanopusResult.class);

        // check if we need to skip
        if (!isRecompute(inst) && input.stream().anyMatch((it -> it.getCandidate().hasAnnotation(CanopusResult.class)))) {
            LOG().info("Skipping Canopus for Instance \"" + inst.getExperiment().getName() + "\" because results already exist.");
            return;
        }

        //invalidate result for following computation
        invalidateResults(inst);

        // create input
        List<FormulaResult> res = input.stream().map(SScored::getCandidate)
                .filter(ir -> ir.hasAnnotation(FingerprintResult.class)).collect(Collectors.toList());

        // check for valid input
        if (res.size() < 1)
            return; // nothing to do
        //throw new IllegalArgumentException("No FingerID Result available for compound class prediction");

        // submit canopus jobs for Identification results that contain CSI:FingerID results
//        Map<FormulaResult, CanopusJJob> jobs = res.stream().collect(Collectors.toMap(r -> r, this::buildAndSubmit));
        Map<FormulaResult, CanopusWebJJob> jobs = res.stream().collect(Collectors.toMap(r -> r, this::buildAndSubmitRemote));

        jobs.forEach((k, v) -> k.setAnnotation(CanopusResult.class, v.takeResult()));

        // write Canopus client data
        if (inst.getProjectSpaceManager().getProjectSpaceProperty(CanopusData.class).isEmpty())
            inst.getProjectSpaceManager().setProjectSpaceProperty(CanopusData.class, ApplicationCore.WEB_API.getCanopusdData(UserDefineablePredictorType.CSI_FINGERID.toPredictorType(inst.getID().getIonType().get().getCharge())));

        // write canopus results
        for (FormulaResult r : res)
            inst.updateFormulaResult(r, CanopusResult.class);
    }

    /*private CanopusJJob buildAndSubmit(@NotNull final FormulaResult ir) {
        final CanopusJJob canopusJob = new CanopusJJob(ApplicationCore.CANOPUS);
        canopusJob.setFormula(ir.getId().getMolecularFormula())
                .setFingerprint(ir.getAnnotationOrThrow(FingerprintResult.class).fingerprint);
        return SiriusJobs.getGlobalJobManager().submitJob(canopusJob);
    }*/

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
}
