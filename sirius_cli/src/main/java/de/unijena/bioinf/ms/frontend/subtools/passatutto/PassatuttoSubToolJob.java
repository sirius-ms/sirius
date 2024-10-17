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

package de.unijena.bioinf.ms.frontend.subtools.passatutto;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.jjobs.JobSubmitter;
import de.unijena.bioinf.ms.frontend.subtools.InstanceJob;
import de.unijena.bioinf.ms.frontend.utils.PicoUtils;
import de.unijena.bioinf.passatutto.Decoy;
import de.unijena.bioinf.passatutto.Passatutto;
import de.unijena.bioinf.projectspace.FCandidate;
import de.unijena.bioinf.projectspace.Instance;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class PassatuttoSubToolJob extends InstanceJob {
    public PassatuttoSubToolJob(JobSubmitter submitter) {
        super(submitter);
    }

    @Override
    public boolean isAlreadyComputed(@NotNull Instance inst) {
        // We do not have to invalidate results because there is no method that builds on top of Passatutto
        return inst.hasPassatuttoResult();
    }

    @Override
    protected void computeAndAnnotateResult(@NotNull Instance inst) throws InterruptedException {
        Optional<FCandidate<?>> best = inst.getTopFTree();

        if (best.map(f -> f.hasAnnotation(FTree.class)).orElse(false)) {
            logInfo("Skipping instance \"" + inst.getName() + "\" because there are no trees computed. No fragmentation trees are computed yet. Run SIRIUS first and provide the correct molecular formula in the input files before calling Passatutto.");
            return;
        }

        final FTree tree = best.flatMap(f -> f.getAnnotation(FTree.class)).orElseThrow();

        if (tree.getFragments().size() < 3){
            logInfo("Skipping instance \"" + inst.getName() + "\" because tree contains less than 3 Fragments!");
            return;
        }

        checkForInterruption();

        final Decoy decoyByRerootingTree = submitSubJob(
                Passatutto.makePassatuttoJob(tree, tree.getAnnotationOrThrow(PrecursorIonType.class)))
                .takeResult();
       inst.savePassatuttoResult(best.get(), decoyByRerootingTree);
    }

    @Override
    public String getToolName() {
        return PicoUtils.getCommand(PassatuttoOptions.class).name();
    }
}
