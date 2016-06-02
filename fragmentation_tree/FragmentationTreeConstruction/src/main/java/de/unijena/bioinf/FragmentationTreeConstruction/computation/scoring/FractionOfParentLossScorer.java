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
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

import java.util.List;

/**
 * compares the mass of a loss to the parent mass or if unknown to the mass of the heaviest peak.
 * Different to RelativeLossSizeScorer because it don't punishes if loss sizes don't grow with parent mass (?)
 */
@Called("FractionOfParent")
public class FractionOfParentLossScorer implements LossScorer {
    //todo implement as PeakPairScorer?...
    @Override
    public Double prepare(ProcessedInput inputh) {
        //if ion mass known take this
        double comparableMass = (inputh.getParentPeak() == null ? 0 : inputh.getParentPeak().getMz());
        //else find largest mass, Double.MAX_VALUE means existing dummy node
        if (comparableMass == 0d || comparableMass == Double.MAX_VALUE) {
            comparableMass = 0d;
            final List<ProcessedPeak> peaks = inputh.getMergedPeaks();
            for (ProcessedPeak peak : peaks) {
                if (peak.getMz() > comparableMass && peak.getMz() < Double.MAX_VALUE) comparableMass = peak.getMz();
            }
        }
        return comparableMass;
    }


    @Override
    public double score(Loss loss, ProcessedInput input, Object precomputed) {
        double comparableMass = (Double) precomputed;
        // Score with fraction of the parentmass or largest mass in spectrum.
        return Math.log(1 - (loss.getFormula().getMass() / comparableMass));
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
