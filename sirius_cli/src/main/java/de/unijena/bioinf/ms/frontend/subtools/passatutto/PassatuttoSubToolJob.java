package de.unijena.bioinf.ms.frontend.subtools.passatutto;

import de.unijena.bioinf.ChemistryBase.ms.AnnotatedSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ms.frontend.subtools.InstanceJob;
import de.unijena.bioinf.passatutto.Decoy;
import de.unijena.bioinf.passatutto.Passatutto;
import de.unijena.bioinf.passatutto.RerootedTree;
import de.unijena.bioinf.passatutto.RerootingTreeMethod;
import de.unijena.bioinf.sirius.ExperimentResult;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.IdentificationResults;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class PassatuttoSubToolJob extends InstanceJob {
    @Override
    protected void computeAndAnnotateResult(@NotNull ExperimentResult expRes) throws Exception {
        if (!expRes.hasAnnotation(IdentificationResults.class))
            throw new IllegalArgumentException("No fragmentation trees are computed yet. Run SIRIUS first and provide the correct molecular formula in the input files before calling passatutto.");
        Optional<IdentificationResult> best = expRes.getResults().getBest();
        if (best.isPresent()) {
            final FTree tree = best.get().getTree();
            // should be a CPU job?
            final Decoy decoyByRerootingTree = new Passatutto().createDecoyByRerootingTree(tree, best.get().getPrecursorIonType());
            best.get().setAnnotation(Decoy.class, decoyByRerootingTree);
        }

    }
}
