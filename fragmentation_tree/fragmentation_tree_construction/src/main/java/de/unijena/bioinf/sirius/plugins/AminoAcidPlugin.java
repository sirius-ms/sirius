/*
 * This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 * Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 * Chair of Bioinformatics, Friedrich-Schilller University.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.sirius.plugins;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.SiriusPlugin;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.CommonFragmentsScore;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.CommonLossEdgeScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.LossSizeScorer;

public class AminoAcidPlugin extends SiriusPlugin {
    public final static String[] AMINO_ACID_RESIDUES = new String[]{
            "C3H5N1O1", "C3H5N1O1S1", "C4H5N1O3", "C5H7N1O3", "C9H9N1O1", "C2H3N1O1", "C6H7N3O1", "C6H11N1O1", "C6H12N2O1", "C6H11N1O1", "C5H9N1O1S1", "C4H6N2O2", "C5H7N1O1", "C5H8N2O2", "C6H12N4O1", "C3H5N1O2", "C4H7N1O2", "C5H9N1O1", "C11H10N2O1", "C9H9N1O2"
    };
    @Override
    public void initializePlugin(PluginInitializer initializer) {
        final CommonLossEdgeScorer commonLossEdgeScorer = FragmentationPatternAnalysis.getByClassName(CommonLossEdgeScorer.class, initializer.getAnalysis().getLossScorers());
        final CommonFragmentsScore commonFragmentsScore = FragmentationPatternAnalysis.getByClassName(CommonFragmentsScore.class, initializer.getAnalysis().getDecompositionScorers());
        final LossSizeScorer lossSizeScorer = FragmentationPatternAnalysis.getByClassName(LossSizeScorer.class, initializer.getAnalysis().getPeakPairScorers());
        final MolecularFormula h20 = MolecularFormula.parseOrThrow("H2O");
        if (commonLossEdgeScorer!=null && commonFragmentsScore!=null && lossSizeScorer!=null) {
            for (String acid : AMINO_ACID_RESIDUES) {
                final MolecularFormula residue = MolecularFormula.parseOrThrow(acid);
                double penalty = lossSizeScorer.score(residue);
                final double score = 1.0 - Math.min(0,penalty);
                if (commonLossEdgeScorer.score(residue) < score) {
                    commonLossEdgeScorer.addCommonLoss(residue, score);
                }
                final double fragScore = 0.5;
                if (fragScore > commonFragmentsScore.score(residue)) {
                    commonFragmentsScore.addCommonFragment(residue, fragScore);
                }
                final MolecularFormula res = residue.add(h20);
                if (fragScore > commonFragmentsScore.score(res)) {
                    commonFragmentsScore.addCommonFragment(res, fragScore);
                }
            }
        }
    }
}
