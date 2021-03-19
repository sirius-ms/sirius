/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.frontend.subtools.decomp;

import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.MassDecomposer.Chemistry.MassToFormulaDecomposer;

import java.util.List;

/**
 * Created by kaidu on 27.07.16.
 */
public class Test {

    public static void main(String[] args) {
        final PeriodicTable P = PeriodicTable.getInstance();
        String allowedElements = "CHNOPS";
        int allowedPPM = 0;
        double allowedMz = 0.003;

        final FormulaConstraints constraints = new FormulaConstraints("CHNOPS");
        MassToFormulaDecomposer decomposer = new MassToFormulaDecomposer(constraints.getChemicalAlphabet());

// you might want to remove RDBE >=0 condition which is enabled by default
        constraints.getFilters().clear(); // remove RDBE>=0
        for (double mass : new double[]{10000d}) {
            final long time = System.currentTimeMillis();
            List<MolecularFormula> formulas = decomposer.decomposeToFormulas(mass, PeriodicTable.getInstance().neutralIonization(), new Deviation(allowedPPM, allowedMz), constraints);
            final long time2 = System.currentTimeMillis();
            System.out.println(time2-time);
            System.out.println(formulas.size());
        }
    }

}
