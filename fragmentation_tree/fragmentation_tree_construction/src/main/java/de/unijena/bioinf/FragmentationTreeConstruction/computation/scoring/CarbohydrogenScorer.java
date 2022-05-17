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

import de.unijena.bioinf.ChemistryBase.algorithm.Called;
import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.math.ParetoDistribution;
import de.unijena.bioinf.ChemistryBase.ms.ft.AbstractFragmentationGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;

/**
 * Gives some score bonus to formulas consist only of C,H,O, because it seems that they are very frequent and
 * our model wrongly favours CHNO compounds instead, because they will result in more possibilities and lower mass
 * errors.
 */
public abstract class CarbohydrogenScorer {

    public static void main(String[] args) {
        for (double p : new double[]{0.01,0.015, 0.02, 0.05,0.1,0.15, 0.2,0.5,1.0}) {
            System.out.println(p + "\t" + (new ParetoDistribution.EstimateByMedian(0.02).extimateByMedian(0.5).getCumulativeProbability(p)));
        }
    }

    @Called("CarbohydrogenCompound")
    public static class CarbohydrogenRootScorer implements DecompositionScorer<Object> {

        @Override
        public Object prepare(ProcessedInput input) {
            return null;
        }

        @Override
        public double score(MolecularFormula formula, Ionization ion, ProcessedPeak peak, ProcessedInput input, Object precomputed) {
            return formula.isCHO() ? 2.5 : 0d;
        }

        @Override
        public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

        }

        @Override
        public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

        }
    }

    @Called("CarbohydrogenFragment")
    public static class CarbohydrogenFragmentScorer implements DecompositionScorer<ParetoDistribution> {

        @Override
        public ParetoDistribution prepare(ProcessedInput input) {
            return new ParetoDistribution.EstimateByMedian(0.02).extimateByMedian(0.5);
        }

        @Override
        public double score(MolecularFormula formula, Ionization ion, ProcessedPeak peak, ProcessedInput input, ParetoDistribution precomputed) {
            return peak.getRelativeIntensity()>0.02 && formula.isCHO() ? precomputed.getCumulativeProbability(peak.getRelativeIntensity()) : 0d;
        }

        @Override
        public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

        }

        @Override
        public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

        }
    }
    @Called("CarbohydrogenLoss")
    public static class CarbohydrogenLossScorer implements LossScorer<Object> {

        @Override
        public Object prepare(ProcessedInput input, AbstractFragmentationGraph graph) {
            return null;
        }

        @Override
        public double score(Loss loss, ProcessedInput input, Object precomputed) {
            if (loss.getFormula().isCHO() && loss.getFormula().getMass() > Math.exp(LossSizeScorer.LEARNED_MEAN) && loss.getFormula().numberOfCarbons()>0 && loss.getFormula().numberOfHydrogens()>0) {
                // reduce loss size penalty by 50%
                return -Math.min(0, new LossSizeScorer().score(loss.getFormula()))*0.5;
            } else return 0d;
        }

        @Override
        public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

        }

        @Override
        public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

        }
    }

}
