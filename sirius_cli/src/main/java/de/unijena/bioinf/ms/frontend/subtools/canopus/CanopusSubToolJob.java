package de.unijena.bioinf.ms.frontend.subtools.canopus;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.fingerid.CanopusJJob;
import de.unijena.bioinf.fingerid.CanopusResult;
import de.unijena.bioinf.fingerid.FingerprintResult;
import de.unijena.bioinf.fingerid.annotations.FormulaResultRankingScore;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.Instance;
import de.unijena.bioinf.ms.frontend.subtools.InstanceJob;
import de.unijena.bioinf.projectspace.FormulaScoring;
import de.unijena.bioinf.projectspace.sirius.FormulaResult;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CanopusSubToolJob extends InstanceJob {

    @Override
    protected void computeAndAnnotateResult(final @NotNull Instance inst) throws Exception {
        System.out.println("I am Canopus on Experiment " + inst);
        List<? extends SScored<FormulaResult, ? extends FormulaScore>> input = inst.loadFormulaResults(
                inst.getExperiment().getAnnotationOrThrow(FormulaResultRankingScore.class).value,
                FormulaScoring.class, FTree.class, FingerprintResult.class, CanopusResult.class);

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
        Map<FormulaResult, CanopusJJob> jobs = res.stream().collect(Collectors.toMap(r -> r, this::buildAndSubmit));

        jobs.forEach((k, v) -> k.setAnnotation(CanopusResult.class, v.takeResult()));

        for (FormulaResult r : res)
            inst.updateFormulaResult(r, CanopusResult.class);
    }

    private CanopusJJob buildAndSubmit(@NotNull final FormulaResult ir) {
        final CanopusJJob canopusJob = new CanopusJJob(ApplicationCore.CANOPUS);
        canopusJob.setFormula(ir.getAnnotationOrThrow(FTree.class).getRoot().getFormula())
                .setFingerprint(ir.getAnnotationOrThrow(FingerprintResult.class).fingerprint);
        return SiriusJobs.getGlobalJobManager().submitJob(canopusJob);
    }


}
