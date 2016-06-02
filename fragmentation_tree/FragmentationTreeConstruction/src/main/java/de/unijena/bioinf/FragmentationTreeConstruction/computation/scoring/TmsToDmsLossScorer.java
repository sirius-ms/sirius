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

import de.unijena.bioinf.ChemistryBase.algorithm.Called;
import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;

/**
 * If there's a conversion from head to tail of TMS into DMS this LossScorer punishes Losses which consist of more then
 * a single CH3. Because the CH3 group of TMS is not directly connected to other groups but the DMS part.
 */
@Called("TmsToDmsLossScorer")
public class TmsToDmsLossScorer implements LossScorer {
    @Override
    public Object prepare(ProcessedInput inputh) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public double score(Loss loss, ProcessedInput input, Object precomputed) {
        final MolecularFormula lossFormula = loss.getFormula();
        final MolecularFormula headFormula = loss.getSource().getFormula();
        final MolecularFormula tailFormula = loss.getTarget().getFormula();
        final PeriodicTable periodicTable = PeriodicTable.getInstance();
//        int tmsHead = 0;
//        int tmsTail = 0;
        int dmsTail = 0;
        int dmsHead = 0;

//        final Element tmsElement = periodicTable.getByName("Tms");
//        if (tmsElement != null){
//            tmsHead = headFormula.numberOf(tmsElement);
//            tmsTail = tailFormula.numberOf(tmsElement);
//        }
        final Element dmsElement = periodicTable.getByName("Dms");
        if (dmsElement != null) {
            dmsTail = tailFormula.numberOf(dmsElement);
            dmsHead = headFormula.numberOf(dmsElement);
        }


        double score = 0;
        //if (tmsHead>0 && dmsTail>0){
        if (dmsTail - dmsHead > 0) {
            score -= 0.1;//todo is it uncommon, that TMS looses its CH3 group? If yes, does this scoring of -0.1 influence anything if CH3 as common loss scores log(100)~=4,6..
            //if loss != CH3 score -10
            if (lossFormula.elements().size() != 2 || lossFormula.numberOfCarbons() != 1 || lossFormula.numberOfHydrogens() != 3) {
                score -= 10;
            }
        }
        return score;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
