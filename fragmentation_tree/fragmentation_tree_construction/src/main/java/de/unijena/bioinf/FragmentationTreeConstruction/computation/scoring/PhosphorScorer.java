
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.ft.AbstractFragmentationGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;

public class PhosphorScorer implements DecompositionScorer<Element[]>, LossScorer<Element[]> {
    @Override
    public Element[] prepare(ProcessedInput input, AbstractFragmentationGraph graph) {
        return new Element[]{
                PeriodicTable.getInstance().getByName("P"),
                PeriodicTable.getInstance().getByName("S")
        };
    }
    @Override
    public Element[] prepare(ProcessedInput input) {
        return prepare(input,null);
    }

    @Override
    public double score(Loss loss, ProcessedInput input, Element[] phosphorAndSulfur) {
        final int pnum = loss.getFormula().numberOf(phosphorAndSulfur[0]);
        if (pnum > 0) {
            // expect either one Sulfur or one Oxygen for each phosphor loss
            if (loss.getFormula().numberOfOxygens() < pnum && loss.getFormula().numberOf(phosphorAndSulfur[1]) < pnum)
                return Math.log(0.25d);
            else return 0d;
        } else return 0d;
    }

    @Override
    public double score(MolecularFormula formula, Ionization ion, ProcessedPeak peak, ProcessedInput input, Element[] phosphorAndSulfur) {
        final int pnum = formula.numberOf(phosphorAndSulfur[0]);
        if (peak == input.getParentPeak()) {
            // expect 2 oxygen/sulfur for each phosphor losses
            if ((formula.numberOf(phosphorAndSulfur[1]) + formula.numberOfOxygens()) < (pnum * 2))
                return Math.log(0.05d);
            else return 0d;
        }
        if (pnum > 0) {
            // expect either one Sulfur or one Oxygen for each phosphor loss
            if (formula.numberOfOxygens() < pnum && formula.numberOf(phosphorAndSulfur[1]) < pnum)
                return Math.log(0.25d);
            else return 0d;
        } else return 0d;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

    }
}
