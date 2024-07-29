/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.fragmenter;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.jjobs.BasicJJob;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import java.util.HashSet;

/**
 * Class to provide background computation functionality to create substructure annotations of fragments (Ftree/Spectrum)
 * based on a given molecular structure. This can be seen as de default computation method provider.
 */
public class InsilicoFragmentationPeakAnnotator {


    @Setter
    @Getter
    private int nodeLimit;

    public InsilicoFragmentationPeakAnnotator() {
        this(50000);
    }

    public InsilicoFragmentationPeakAnnotator(int nodeLimit) {
        this.nodeLimit = nodeLimit;
    }

    public InsilicoFragmentationResult fragmentAndAnnotate(@NotNull FTree tree, @NotNull String structureSmiles) {
        return SiriusJobs.getGlobalJobManager().submitJob(makeJJob(tree, structureSmiles)).takeResult();
    }

    public InsilicoFragmentationResult fragmentAndAnnotate(@NotNull FTree tree, @NotNull MolecularGraph structure) {
        return SiriusJobs.getGlobalJobManager().submitJob(makeJJob(tree, structure)).takeResult();
    }

    public Job makeJJob(@NotNull FTree tree, @NotNull String structureSmiles) {
        try {
            return new Job(tree, new MolecularGraph(new SmilesParser(SilentChemObjectBuilder.getInstance())
                    .parseSmiles(structureSmiles)));
        } catch (InvalidSmilesException e) {
            throw new IllegalArgumentException("Could not parse input smiles. Computation not started!", e);
        }
    }

    public Job makeJJob(@NotNull FTree tree, @NotNull MolecularGraph structure) {
        return new Job(tree, structure);
    }


    public class Job extends BasicJJob<InsilicoFragmentationResult> {

        @Getter final FTree tree;
        @Getter final MolecularGraph molecule;

        private Job(@NotNull FTree tree, @NotNull MolecularGraph molecule) {
            super(JobType.CPU);
            this.tree = tree;
            this.molecule = molecule;
        }

        @Override
        protected InsilicoFragmentationResult compute() throws Exception {
            checkForInterruption();

            final EMFragmenterScoring2 scoring = new EMFragmenterScoring2(molecule, tree);
            final CriticalPathSubtreeCalculator subtreeCalculator = new CriticalPathSubtreeCalculator(tree, molecule, scoring, true);
            subtreeCalculator.setMaxNumberOfNodes(nodeLimit);
            final HashSet<MolecularFormula> fset = new HashSet<>();

            checkForInterruption();

            for (Fragment ft : tree.getFragmentsWithoutRoot()) {
                fset.add(ft.getFormula());
                fset.add(ft.getFormula().add(MolecularFormula.getHydrogen()));
                fset.add(ft.getFormula().add(MolecularFormula.getHydrogen().multiply(2)));
                if (ft.getFormula().numberOfHydrogens() > 0)
                    fset.add(ft.getFormula().subtract(MolecularFormula.getHydrogen()));
                if (ft.getFormula().numberOfHydrogens() > 1)
                    fset.add(ft.getFormula().subtract(MolecularFormula.getHydrogen().multiply(2)));
            }

            checkForInterruption();

            try {
                subtreeCalculator.initialize((node, nnodes, nedges) -> {
                    if (fset.contains(node.getFragment().getFormula())) return true;
                    return (node.getTotalScore() > -5f);
                });
            } catch (RuntimeException e) {
                if (e.getCause() instanceof InterruptedException) {
                    throw (InterruptedException) e.getCause();
                } else throw e;
            }
            checkForInterruption();
            subtreeCalculator.computeSubtree();
            checkForInterruption();
            subtreeCalculator.computeMapping();
            checkForInterruption();

            return InsilicoFragmentationResult.of(subtreeCalculator);
        }
    }
}
