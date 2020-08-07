/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

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
    public boolean isAlreadyComputed(@NotNull Instance inst) {
        // We do not have to invalidate results because there is no method that builds on top of Passatutto
        return inst.loadCompoundContainer().hasResult() && inst.loadFormulaResults(Decoy.class).stream().anyMatch(it -> it.getCandidate().hasAnnotation(Decoy.class));
//            logInfo("Skipping CSI:FingerID for Instance \"" + inst.getExperiment().getName() + "\" because results already exist.");
    }

    @Override
    protected void computeAndAnnotateResult(@NotNull Instance inst) {
        final FormulaResult best = inst.loadTopFormulaResult(List.of(ZodiacScore.class, SiriusScore.class), FormulaScoring.class, FTree.class)
                .orElse(null);

        if (best == null || best.getAnnotation(FTree.class).isEmpty()) {
            logInfo("Skipping instance \"" + inst.getExperiment().getName() + "\" because there are no trees computed. No fragmentation trees are computed yet. Run SIRIUS first and provide the correct molecular formula in the input files before calling Passatutto.");
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
