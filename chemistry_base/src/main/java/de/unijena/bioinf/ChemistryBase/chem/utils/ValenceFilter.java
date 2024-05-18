
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

package de.unijena.bioinf.ChemistryBase.chem.utils;

import de.unijena.bioinf.ChemistryBase.algorithm.HasParameters;
import de.unijena.bioinf.ChemistryBase.algorithm.Parameter;
import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.ms.PossibleAdducts;

import java.util.Set;
import java.util.stream.Collectors;

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
        this(minValence, PeriodicTable.getInstance().getAdducts());

    }

    //todo what about the parameter annotation?
    public ValenceFilter(@Parameter("minValence") double minValence, Set<PrecursorIonType> possibleAdducts) {
        this.minValenceInt = (int)(2*minValence);
        this.minValence = minValence;
        this.possibleAdducts = new PossibleAdducts(possibleAdducts);
    }

    public ValenceFilter filterWithoutAdducts(){
        //todo or get AdductSettings from Somewhere
        return new ValenceFilter(MIN_VALENCE_DEFAULT, new PossibleAdducts(PeriodicTable.getInstance().getIonizations()).getAdducts());
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
    public boolean isValid(MolecularFormula measuredNeutralFormula) {
        return measuredNeutralFormula.doubledRDBE()>=MIN_VALENCE_DEFAULT; //todo ElementFilter: this seems wrong
    }

    @Override
    public boolean isValid(MolecularFormula measuredNeutralFormula, Ionization ionization) {
        if (ionization==PeriodicTable.getInstance().neutralIonization()) return isValid(measuredNeutralFormula);
        Set<PrecursorIonType> adducts = possibleAdducts.getAdducts(ionization).stream().filter(PrecursorIonType::isSupportedForFragmentationTreeComputation).collect(Collectors.toSet());
        if (adducts.size() == 0)
           adducts.add(PrecursorIonType.fromString(ionization.toString()));

        for (PrecursorIonType ionType : adducts) {
            if (isValid(measuredNeutralFormula, ionType)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isValid(MolecularFormula measuredNeutralFormula, PrecursorIonType ionType) {
        if (!ionType.isSupportedForFragmentationTreeComputation()) return false;
        MolecularFormula compoundMF = ionType.measuredNeutralMoleculeToNeutralMolecule(measuredNeutralFormula);
        if (!compoundMF.isAllPositiveOrZero()) return false;
        return compoundMF.doubledRDBE() >= minValenceInt;
    }
}
