
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
 * Prior based on ring double bond equation (RDBE) value of the molecule. In contrast to RDBENormal Scorer this
 * scorer does not penalize low RDBE values. Although molecules with low rdbe values are rare, a low rdbe is not
 * strange nor a signal for impossible formulas. Because the RDBE depends on the molecule size (as more atoms as more
 * possible double bonds and rings) this scorer divides the rdbe through the square root of the molecules mass.
 * The square root is used, because the pearson correlation coefficient between rdbe and sqrt(mass) is slightly higher than between
 * rdbe and mass.
 * Molecules with an rdbe/sqrt(mass) lower than 0.75 are scored by an uniform distribution with the maximal probability.
 * Molecules with higher rdbe are "rare/strange" and are penalized using a Pareto Distribution. The parameter
 * xmin=0.75 is the 95% quantile of the rdbe/sqrt(mass) values in kegg. The parameter k=9 is chosen by an educated
 * guess. While it fits the rdbe/sqrt(mass) distribution in kegg very well, it is not optimized using e.g. least-square.
 * <p>
 * The main idea behind this scorer is to filter out molecular formulas that do not exist due to their strange elemental
 * composition. For example: C54H18OP2 which has so much carbon that it have to consists of many rings and double bonds.
 * It is not the idea of this scorer to score the frequency of a formula in a database. Therefore, the RDBENormalScorer
 * may result in better scores, but only if the input dataset has the same formula distribution as the training set. While
 * this scorer should work for all data as long as there are no "extremely strange" molecules in there.
 */
@HasParameters
public class RDBEMassScorer implements MolecularFormulaScorer {

    private final static PartialParetoDistribution keggParetoDistribution = new PartialParetoDistribution(-0.5, 0.75, 9);

    private final static NormalDistribution keggNormalDistribution = new NormalDistribution(0.1481998, 0.07341486);
    private DensityFunction distribution;

    public RDBEMassScorer(@Parameter("distribution") DensityFunction distribution) {
        this.distribution = distribution;
    }

    public RDBEMassScorer() {
        this(keggNormalDistribution);
    }

    public static NormalDistribution getRDBEDistributionFromPubchemSubset() {
        return new NormalDistribution(0.4712924, Math.pow(0.2245154, 2));
    }

    public static PartialParetoDistribution getRDBEDistributionFromKEGG() {
        return keggParetoDistribution;
    }

    public final double getRDBEMassValue(MolecularFormula formula) {
        return getRDBEMassValue(formula.rdbe(), formula.getMass());
    }

    public final double getRDBEMassValue(double rdbe, double mass) {
        return rdbe / Math.pow(mass, 2d / 3d);
    }

    @Override
    public double score(MolecularFormula formula) {
        return Math.log(distribution.getDensity(getRDBEMassValue(formula)));
    }

    public double score(double rdbe, double mass) {
        return Math.log(distribution.getDensity(getRDBEMassValue(rdbe, mass)));
    }

    public DensityFunction getDistribution() {
        return distribution;
    }
}
