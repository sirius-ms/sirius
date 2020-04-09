package de.unijena.bioinf.ms.frontend.subtools.passatutto;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.GibbsSampling.ZodiacScore;
import de.unijena.bioinf.jjobs.JobSubmitter;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.ms.frontend.subtools.InstanceJob;
import de.unijena.bioinf.ms.frontend.utils.PicoUtils;
import de.unijena.bioinf.passatutto.Decoy;
import de.unijena.bioinf.passatutto.Passatutto;
import de.unijena.bioinf.projectspace.FormulaScoring;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.projectspace.sirius.FormulaResult;
import de.unijena.bioinf.sirius.scores.SiriusScore;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PassatuttoSubToolJob extends InstanceJob {
    public PassatuttoSubToolJob(JobSubmitter submitter) {
        super(submitter);
    }

    @Override
    protected void computeAndAnnotateResult(@NotNull Instance inst) {
        final FormulaResult best = inst.loadTopFormulaResult(List.of(ZodiacScore.class, SiriusScore.class), FormulaScoring.class, FTree.class)
                .orElse(null);

        if (best == null || best.getAnnotation(FTree.class).isEmpty()) {
            logInfo("Skipping instance \"" + inst.getExperiment().getName() + "\" because there are no trees computed. No fragmentation trees are computed yet. Run SIRIUS first and provide the correct molecular formula in the input files before calling Passatutto.");
            return;
        }

        // We do not have to invalidate results because there is no method that builds on top of Passatutto
        if (!isRecompute(inst) && best.hasAnnotation(Decoy.class)) {
            logInfo("Skipping CSI:FingerID for Instance \"" + inst.getExperiment().getName() + "\" because results already exist.");
            return;
        }

        final FTree tree = best.getAnnotationOrThrow(FTree.class);

        if (tree.getFragments().size() < 3){
            logInfo("Skipping instance \"" + inst.getExperiment().getName() + "\" because tree contains less than 3 Fragments!");
            return;
        }


        final Decoy decoyByRerootingTree = submitJob(
                Passatutto.makePassatuttoJob(tree, tree.getAnnotationOrThrow(PrecursorIonType.class)))
                .takeResult();
        best.setAnnotation(Decoy.class, decoyByRerootingTree);


        //write Passatutto results
        inst.updateFormulaResult(best, Decoy.class);
    }

    @Override
    protected Class<? extends DataAnnotation>[] formulaResultComponentsToClear() {
        return new Class[]{Decoy.class};
    }

    @Override
    public String getToolName() {
        return PicoUtils.getCommand(PassatuttoOptions.class).name();
    }
}
