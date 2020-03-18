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
package de.unijena.bioinf.ChemistryBase.chem.utils;

import de.unijena.bioinf.ChemistryBase.algorithm.HasParameters;
import de.unijena.bioinf.ChemistryBase.algorithm.Parameter;
import de.unijena.bioinf.ChemistryBase.chem.FormulaFilter;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.PossibleAdducts;
import gnu.trove.map.hash.TObjectDoubleHashMap;

/**
 * A formula passes this filter, if its RDBE value is greater or equal to the given limit
 */
@HasParameters
public class ValenceFilter implements FormulaFilter {

    private final int minValenceInt;
    private final double minValence;

    private final PossibleAdducts possibleAdducts;

    /*
    lowest rdbe share ignoring the +2 offset for the 'start' of a molecule
     */
    private final TObjectDoubleHashMap<Ionization> ionizationToLowestAdductRDBE;

    public ValenceFilter() {
        this(-0.5d);
    }

    public ValenceFilter(@Parameter("minValence") double minValence) {
        this(minValence, null);

    }

    public ValenceFilter(@Parameter("minValence") double minValence, PossibleAdducts possibleAdducts) {
        this.minValenceInt = (int)(2*minValence);
        this.minValence = minValence;
        this.possibleAdducts = possibleAdducts;
        this.ionizationToLowestAdductRDBE = new TObjectDoubleHashMap<>(10, 0.75f, Double.NaN);
        if (possibleAdducts != null) {
            for (PrecursorIonType ionType : possibleAdducts) {
                Ionization ionization = ionType.getIonization();
                MolecularFormula adduct = ionType.getAdduct();
                //-2 = RDBE share for not starting a new molecule
                double doubleRDBE = adduct.doubledRDBE()-2;
                double current = ionizationToLowestAdductRDBE.get(ionization);
                if (Double.isNaN(current) || (doubleRDBE<current)) {
                    ionizationToLowestAdductRDBE.put(ionization, doubleRDBE);
                }
            }
        }
    }

//    @Override
//    public boolean isValid(MolecularFormula formula) {
//        return formula.doubledRDBE() >= minValenceInt;
//    }



    public double getMinValence() {
        return minValence;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ValenceFilter && obj.getClass().equals(this.getClass())) {
            return ((ValenceFilter) obj).minValenceInt == minValenceInt;
        } else return false;
    }

    @Override
    public boolean isValid(MolecularFormula measuredNeutralFormula, Ionization ionization) {
        final double drdbe = measuredNeutralFormula.doubledRDBE();
        double adjustment = ionizationToLowestAdductRDBE.get(ionization);
        if (Double.isNaN(adjustment)){
            return drdbe >= minValenceInt;
        } else {
            return drdbe >= minValenceInt-adjustment;
        }
    }

    @Override
    public boolean isValid(MolecularFormula measuredNeutralFormula, PrecursorIonType ionType) {
        MolecularFormula compoundMF = ionType.measuredNeutralMoleculeToNeutralMolecule(measuredNeutralFormula);
        if (!compoundMF.isAllPositiveOrZero()) return false;
        return compoundMF.doubledRDBE() >= minValenceInt;
    }
}
