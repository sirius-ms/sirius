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
import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.ms.PossibleAdducts;
import gnu.trove.map.hash.TObjectDoubleHashMap;

import java.util.Set;

/**
 * A formula passes this filter, if its RDBE value is greater or equal to the given limit
 */
@HasParameters
public class ValenceFilter implements FormulaFilter {

    private final int minValenceInt;
    private final double minValence;

    private final PossibleAdducts possibleAdducts;

    private static final double MIN_VALENCE_DEFAULT  = -0.5d;

    public ValenceFilter() {
        this(MIN_VALENCE_DEFAULT);
    }

    public ValenceFilter(@Parameter("minValence") double minValence) {
        this(minValence, new PossibleAdducts(PeriodicTable.getInstance().getAdducts()));

    }

    //todo what about the parameter annotation?
    public ValenceFilter(@Parameter("minValence") double minValence, PossibleAdducts possibleAdducts) {
        this.minValenceInt = (int)(2*minValence);
        this.minValence = minValence;
        this.possibleAdducts = possibleAdducts;
    }

    public ValenceFilter filterWithoutAdducts(){
        return new ValenceFilter(MIN_VALENCE_DEFAULT, new PossibleAdducts(PeriodicTable.getInstance().getIonizations()));
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
        Set<PrecursorIonType> adducts = possibleAdducts.getAdducts(ionization);
        if (adducts.size()==0) {
            throw new RuntimeException("Ionization not known in ValenceFilter: "+ionization);
        }
        for (PrecursorIonType ionType : adducts) {
            if (isValid(measuredNeutralFormula, ionType)){
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isValid(MolecularFormula measuredNeutralFormula, PrecursorIonType ionType) {
        MolecularFormula compoundMF = ionType.measuredNeutralMoleculeToNeutralMolecule(measuredNeutralFormula);
        if (!compoundMF.isAllPositiveOrZero()) return false;
        return compoundMF.doubledRDBE() >= minValenceInt;
    }
}
