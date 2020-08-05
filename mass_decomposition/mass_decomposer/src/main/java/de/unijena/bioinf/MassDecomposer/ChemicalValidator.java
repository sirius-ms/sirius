
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

package de.unijena.bioinf.MassDecomposer;

import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.MassDecomposer.Chemistry.ChemicalAlphabetWrapper;

public class ChemicalValidator implements DecompositionValidator<Element>, FormulaFilter {

    private final double heteroToCarbonThreshold, hydrogenToCarbonThreshold,rdbeThreshold, rdbeLowerbound;
    private final double h2cto, hy2cto, rdbeo, rdbelo;

    public ChemicalValidator(double rdbeThreshold, double rdbeLowerbound, double heteroToCarbonThreshold, double hydrogenToCarbonThreshold) {
        this.heteroToCarbonThreshold = (h2cto=heteroToCarbonThreshold)+1e-12;
        this.hydrogenToCarbonThreshold = (hy2cto=hydrogenToCarbonThreshold)+1e-12;
        this.rdbeThreshold = (rdbeo=rdbeThreshold)*2+1e-12;
        this.rdbeLowerbound = (rdbelo=rdbeLowerbound)*2-1e-12;
    }

    public static ChemicalValidator getStrictThreshold() {
        return new ChemicalValidator(40, -0.5, 3, 3);
    }
    public static ChemicalValidator getCommonThreshold() {
        return new ChemicalValidator(50, -0.5, 3, 6);
    }
    public static ChemicalValidator getPermissiveThreshold() {
        return new ChemicalValidator(60, -2.5, 4, 9);
    }

    public static ChemicalValidator getRDBEOnlyThreshold() {
        return new ChemicalValidator(Double.MAX_VALUE, -0.5, Double.MAX_VALUE, Double.MAX_VALUE);
    }
    public static ChemicalValidator getRDBEOnlyThreshold(double minrdbe) {
        return new ChemicalValidator(Double.MAX_VALUE, minrdbe, Double.MAX_VALUE, Double.MAX_VALUE);
    }

    public double getHeteroToCarbonThreshold() {
        return h2cto;
    }

    public double getHydrogenToCarbonThreshold() {
        return hy2cto;
    }

    public double getRdbeThreshold() {
        return rdbeo;
    }

    public double getRdbeLowerbound() {
        return rdbelo;
    }

    @Override
    public boolean validate(int[] compomere, int[] characterIds, Alphabet<Element> alphabet) {
        if (alphabet instanceof ChemicalAlphabetWrapper) return validate(compomere, characterIds, ((ChemicalAlphabetWrapper)alphabet).getAlphabet());
        else throw new UnsupportedOperationException(); // TODO: Implement
    }

    public boolean validate(int[] compomere, int[] characterIds, ChemicalAlphabet alphabet) {
        int rdbe=2, numOfAtoms=0;
        for (int i=0; i < compomere.length; ++i) {
            final Element e = alphabet.get(characterIds[i]);
            rdbe += (e.getValence()-2)*compomere[i];
            numOfAtoms += compomere[i];
        }
        if (rdbe < rdbeLowerbound) return false;
        final TableSelection sel = alphabet.getTableSelection();
        double c = compomere[characterIds[sel.carbonIndex()]];
        if (c == 0) c = 0.8;
        final int h = compomere[characterIds[sel.hydrogenIndex()]];
        return rdbe < rdbeThreshold && (numOfAtoms-c-h)/c <= heteroToCarbonThreshold && (c/h) <= hydrogenToCarbonThreshold;

    }

//    @Override
    public boolean isValid(MolecularFormula formula) {
        final double rdbe = formula.rdbe(); //todo shouldn't this be doubleRDBE? compare to ValenceFilter
        return rdbe >= rdbeLowerbound && rdbe < rdbeThreshold && formula.hetero2CarbonRatio() < heteroToCarbonThreshold &&
                formula.hydrogen2CarbonRatio() < hydrogenToCarbonThreshold;
    }

    @Override
    public boolean isValid(MolecularFormula measuredNeutralFormula, Ionization ionization) {
        //todo currently does not account for adducts as ValenceFilter does.
        return isValid(measuredNeutralFormula);
    }

    @Override
    public boolean isValid(MolecularFormula measuredNeutralFormula, PrecursorIonType ionType) {
        MolecularFormula compoundMF = ionType.measuredNeutralMoleculeToNeutralMolecule(measuredNeutralFormula);
        if (!compoundMF.isAllPositiveOrZero()) return false;
        return isValid(compoundMF);
    }
}
