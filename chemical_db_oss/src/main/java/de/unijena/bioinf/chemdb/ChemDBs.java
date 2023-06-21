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

package de.unijena.bioinf.chemdb;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;

import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Function;
import java.util.function.Predicate;

public class ChemDBs {
    public static boolean notInFilter(long entityBits, long filterBits) {
        return !inFilter(entityBits,filterBits);
    }

    public static boolean inFilter(long entityBits, long filterBits) {
        return filterBits == 0 || (entityBits & filterBits) != 0;
    }

    public static <T> Predicate<T> inFilter(Function<T, Long> bitProvider, long filterBits) {
        return t -> inFilter(bitProvider.apply(t),filterBits);
    }

    public static <T> Predicate<T> notInFilter(Function<T, Long> bitProvider, long filterBits) {
        return t -> notInFilter(bitProvider.apply(t),filterBits);
    }

    public static boolean containsFormula(MolecularFormula[] sortedByMass, MolecularFormula query){
        final int formulaIndex = Arrays.binarySearch(sortedByMass, query, Comparator.comparingDouble(MolecularFormula::getMass));
        if (formulaIndex < 0)
            return false;

        if (sortedByMass[formulaIndex].equals(query))
            return true;

        //search to the right
        for (int i = formulaIndex + 1; i < sortedByMass.length; i++) {
            MolecularFormula fc = sortedByMass[i];
            if (Double.compare(fc.getMass(), query.getMass()) != 0)
                break;
            if (fc.equals(query))
                return true;
        }

        //search to the left
        for (int i = formulaIndex - 1; i >= 0; i--) {
            MolecularFormula fc = sortedByMass[i];
            if (Double.compare(fc.getMass(), query.getMass()) != 0)
                break;
            if (fc.equals(query))
                return true;
        }

        return false;
    }
}