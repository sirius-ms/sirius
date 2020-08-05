
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

import de.unijena.bioinf.ChemistryBase.algorithm.HasParameters;
import de.unijena.bioinf.ChemistryBase.algorithm.Parameter;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.MolecularFormulaScorer;
import de.unijena.bioinf.ChemistryBase.math.DensityFunction;
import de.unijena.bioinf.ChemistryBase.math.NormalDistribution;
import de.unijena.bioinf.ChemistryBase.math.PartialParetoDistribution;

/**
 * Scores the hetero-to-carbon (h2c) ratio of a molecule. The keggDistribution is learned from kegg and modelled
 * as normal distribution. While there are fewer molecules with low h2c values in databases, this says nothing about
 * the reasonability of the formula (obviously, formulas consisting only of carbon and hydrogen are possible!).
 * Therefore, a pareto distribution might be a better decision for application: We score all molecules with
 * h2c {@literal <} 1 by an uniform distribution with maximal score. Molecules with h2c worse than 1 are than scored by a
 * pareto distribution which penalize them but "allows" outliers.
 *
 * Remarkt that there are special compounds with very high h2c. This happens when a compound consist of a oxygen+(P,S,?)
 * backbone. Such compounds usually have lower rdbe values, because oxygen can not create much double bonds and rings.
 */
@HasParameters
public class Hetero2CarbonScorer implements MolecularFormulaScorer {

    private final static NormalDistribution keggDistribution = new NormalDistribution(0.5886335, 0.5550574);

    private final static PartialParetoDistribution keggParetoDistribution = new PartialParetoDistribution(0, 1, 3);

    public static NormalDistribution getHeteroToCarbonDistributionFromKEGG() {
        return keggDistribution;
    }
    public static PartialParetoDistribution getHeteroToCarbonParetoDistributionFromKEGG() {
        return keggParetoDistribution;
    }

    private DensityFunction distribution;

    public Hetero2CarbonScorer(@Parameter("distribution") DensityFunction distribution) {
        this.distribution = distribution;
    }

    public Hetero2CarbonScorer() {
        this(keggParetoDistribution);
    }

    @Override
    public double score(MolecularFormula formula) {
        return Math.log(distribution.getDensity(formula.hetero2CarbonRatio()));
    }

    public double score(double hetero2carbon) {
        return Math.log(distribution.getDensity(hetero2carbon));
    }

    public DensityFunction getDistribution() {
        return distribution;
    }
}
