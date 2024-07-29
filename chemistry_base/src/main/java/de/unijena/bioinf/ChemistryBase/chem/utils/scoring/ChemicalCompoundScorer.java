
/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
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

package de.unijena.bioinf.ChemistryBase.chem.utils.scoring;


import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.algorithm.Parameterized;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.MolecularFormulaScorer;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.math.DensityFunction;

/**
 * Factory for chemical scorers
 */
public final class ChemicalCompoundScorer {

    private ChemicalCompoundScorer() {
    }


    public MolecularFormulaScorer getTrainedSupportVectorDecisionFunction() {
        return new SupportVectorMolecularFormulaScorer();
    }

    /**
     * creates a scorer for molecular formulas of the compound. This scorer should not be used
     * for fragments! Currently, the following scorings are used:
     * 1. Hetero-to-Carbon ratio: Heteroatoms are all atoms which are not carbon, hydrogen or oxygen(!)
     * 2. Some compounds have an oxygen+hetero backbone. For this compound, the SpecialMoleculeScorer is used:
     * It scores the Oxygen-to-hetero ratio as well as the RDBE value
     * <p>
     * From both scorings the maximum is used
     * Parameters: All distributions are Uniform+Pareto-Distributions. For all x0{@literal <}x{@literal <}xmin the probability is uniform and
     * maximal. For all x{@literal >}xmin the probability decreases according to a pareto distribution by parameter k
     * Hetero-to-carbon: x0=0, xmin=1, k=3
     * Oxygen-to-hetero: x0=0, xmin=0.75, k=5
     * RDBE: x0=0, xmin=2, k=2
     *
     * @param special if true, then oxygen/hetero backbones are considered in the scoring
     * @return A MolecularFormulaScorer with default parameters
     */
    public static MolecularFormulaScorer createDefaultCompoundScorer(boolean special) {
        return special ? new DefaultScorer() : new ImprovedHetero2CarbonScorer();
    }

    /**
     * @see #createDefaultCompoundScorer(boolean)
     */
    public static MolecularFormulaScorer createDefaultCompoundScorer() {
        return createDefaultCompoundScorer(true);
    }

    public static class DefaultScorer implements MolecularFormulaScorer, Parameterized {
        private ImprovedHetero2CarbonScorer heteroAtom2CarbonScorer;
        private RDBEMassScorer rdbeScorer;
        private SpecialMoleculeScorer oxygenBackboneScorer;

        public DefaultScorer() {
            this.heteroAtom2CarbonScorer = new ImprovedHetero2CarbonScorer();
            this.rdbeScorer = new RDBEMassScorer();
            this.oxygenBackboneScorer = new SpecialMoleculeScorer();
        }

        @Override
        public double score(MolecularFormula formula) {
            return Math.max(heteroAtom2CarbonScorer.score(formula) + rdbeScorer.score(formula), oxygenBackboneScorer.score(formula));
        }

        public ImprovedHetero2CarbonScorer getHeteroAtom2CarbonScorer() {
            return heteroAtom2CarbonScorer;
        }

        public void setHeteroAtom2CarbonScorer(ImprovedHetero2CarbonScorer heteroAtom2CarbonScorer) {
            this.heteroAtom2CarbonScorer = heteroAtom2CarbonScorer;
        }

        public RDBEMassScorer getRdbeScorer() {
            return rdbeScorer;
        }

        public void setRdbeScorer(RDBEMassScorer rdbeScorer) {
            this.rdbeScorer = rdbeScorer;
        }

        public SpecialMoleculeScorer getOxygenBackboneScorer() {
            return oxygenBackboneScorer;
        }

        public void setOxygenBackboneScorer(SpecialMoleculeScorer oxygenBackboneScorer) {
            this.oxygenBackboneScorer = oxygenBackboneScorer;
        }

        public void disableRDBEScorer() {
            rdbeScorer = new RDBEMassScorer(new DensityFunction() {
                @Override
                public double getDensity(double x) {
                    return 0d;
                }
            });
        }

        @Override
        public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
            if (document.hasKeyInDictionary(dictionary, "standardScorer")) {
                // LEGACY PATH
                heteroAtom2CarbonScorer = (ImprovedHetero2CarbonScorer) helper.unwrap(document, document.getFromDictionary(dictionary, "heteroAtom2CarbonScorer"));
                disableRDBEScorer();
            } else {
                heteroAtom2CarbonScorer = (ImprovedHetero2CarbonScorer) helper.unwrap(document, document.getFromDictionary(dictionary, "heteroAtom2CarbonScorer"));
                rdbeScorer = (RDBEMassScorer) helper.unwrap(document, document.getFromDictionary(dictionary, "rdbeScorer"));
            }
            oxygenBackboneScorer = (SpecialMoleculeScorer) helper.unwrap(document, document.getFromDictionary(dictionary, "specialMoleculeScorer"));
        }

        @Override
        public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
            document.addToDictionary(dictionary, "heteroAtom2CarbonScorer", helper.wrap(document, heteroAtom2CarbonScorer));
            document.addToDictionary(dictionary, "rdbeScorer", helper.wrap(document, rdbeScorer));
            document.addToDictionary(dictionary, "specialMoleculeScorer", helper.wrap(document, oxygenBackboneScorer));
        }
    }


}
