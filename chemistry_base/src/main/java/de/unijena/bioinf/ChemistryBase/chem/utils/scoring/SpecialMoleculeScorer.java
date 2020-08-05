
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
import de.unijena.bioinf.ChemistryBase.math.PartialParetoDistribution;


@HasParameters
public class SpecialMoleculeScorer implements MolecularFormulaScorer {


    private final static PartialParetoDistribution oxygenToHeteroKegg = new PartialParetoDistribution(0, 0.75, 5);
    private final static PartialParetoDistribution rdbeKegg = new PartialParetoDistribution(0, 2, 2);

    private DensityFunction oxygenToHeteroDistribution;
    private DensityFunction rdbeDistribution;

    public SpecialMoleculeScorer() {
        this(oxygenToHeteroKegg, rdbeKegg);
    }

    public SpecialMoleculeScorer(@Parameter("oxygenToHeteroDistribution") DensityFunction oxygenToHeteroDistribution, @Parameter("rdbeDistribution") DensityFunction rdbeDistribution) {
        this.oxygenToHeteroDistribution = oxygenToHeteroDistribution;
        this.rdbeDistribution = rdbeDistribution;
    }

    public void setOxygenToHeteroDistribution(DensityFunction oxygenToHeteroDistribution) {
        this.oxygenToHeteroDistribution = oxygenToHeteroDistribution;
    }

    public void setRdbeDistribution(DensityFunction rdbeDistribution) {
        this.rdbeDistribution = rdbeDistribution;
    }

    public DensityFunction getRdbeDistribution() {
        return rdbeDistribution;
    }

    public DensityFunction getOxygenToHeteroDistribution() {
        return oxygenToHeteroDistribution;
    }

    @Override
    public double score(MolecularFormula formula) {
        return Math.log(oxygenToHeteroDistribution.getDensity(formula.hetero2OxygenRatio())) + Math.log(rdbeDistribution.getDensity(formula.rdbe()));
    }

    public double score(double hetero2oxygen, double rdbe) {
        return Math.log(oxygenToHeteroDistribution.getDensity(hetero2oxygen)) + Math.log(rdbeDistribution.getDensity(rdbe));
    }
}
