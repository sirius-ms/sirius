/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;

/**
 * Na+ and K+ might be typical adducts in MS/MS. If a compound is ionized with such an adduct, it shouldn't loose it
 * during fragmentation. I exclude Cl as this element might also occur in organic compounds.
 */
public class AdductFragmentScorer implements LossScorer<Element[]> {

    protected double penalty = Math.log(0.05);

    @Override
    public Element[] prepare(ProcessedInput input) {
        final PeriodicTable pt = PeriodicTable.getInstance();
        if (input.getExperimentInformation().getIonization().getCharge() < 0) {
            return new Element[]{};
        } else {
            return new Element[]{pt.getByName("Na"), pt.getByName("K")};
        }
    }

    @Override
    public double score(Loss loss, ProcessedInput input, Element[] precomputed) {
        for (Element e : precomputed)
            if (loss.getFormula().numberOf(e) > 0) {
                return penalty;
            }
        return 0d;
    }


    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        this.penalty = document.getDoubleFromDictionary(dictionary, "score");
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "score", penalty);
    }
}
