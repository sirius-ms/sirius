package de.unijena.bioinf.ms.frontend.subtools.passatutto;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ms.frontend.subtools.Instance;
import de.unijena.bioinf.ms.frontend.subtools.InstanceJob;
import de.unijena.bioinf.passatutto.Decoy;
import de.unijena.bioinf.passatutto.Passatutto;
import de.unijena.bioinf.projectspace.FormulaScoring;
import de.unijena.bioinf.projectspace.sirius.FormulaResult;
import de.unijena.bioinf.sirius.scores.SiriusScore;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PassatuttoSubToolJob extends InstanceJob {
    @Override
    protected void computeAndAnnotateResult(@NotNull Instance inst) throws Exception {
        final List<? extends SScored<FormulaResult, ? extends FormulaScore>> intput = inst.loadFormulaResults(
                SiriusScore.class,
                FormulaScoring.class, FTree.class);
        if (intput == null || intput.isEmpty())
            throw new IllegalArgumentException("No fragmentation trees are computed yet. Run SIRIUS first and provide the correct molecular formula in the input files before calling passatutto.");

        final FormulaResult best = intput.get(0).getCandidate();
        best.getAnnotation(FTree.class).ifPresent(tree -> {
            final Decoy decoyByRerootingTree = SiriusJobs.getGlobalJobManager().submitJob(
                    Passatutto.makePassatuttoJob(tree, tree.getAnnotationOrThrow(PrecursorIonType.class)))
                    .takeResult();
            best.setAnnotation(Decoy.class, decoyByRerootingTree);
        });

        //write passatuto results
        inst.getProjectSpace().updateFormulaResult(best, Decoy.class);
    }
}
