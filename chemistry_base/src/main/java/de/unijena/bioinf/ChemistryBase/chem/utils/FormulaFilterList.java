
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

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.algorithm.Parameterized;
import de.unijena.bioinf.ChemistryBase.chem.FormulaFilter;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Bundles a list of filters into a single filter object. A formula pass this filter iff it passes all of its filters
 */
public class FormulaFilterList implements FormulaFilter, Parameterized {

    private final List<FormulaFilter> filters;

    public static FormulaFilter create(List<FormulaFilter> filters) {
        if (filters.size()==1) return filters.get(0);
        else return new FormulaFilterList(filters);
    }

    public FormulaFilterList(List<FormulaFilter> filters) {
        this.filters = new ArrayList<FormulaFilter>(filters);
    }

    public List<FormulaFilter> getFilters() {
        return filters;
    }

//    @Override
//    public boolean isValid(MolecularFormula formula) {
//        for (FormulaFilter filter : filters)
//            if (!filter.isValid(formula)) return false;
//        return true;
//    }

    @Override
    public boolean isValid(MolecularFormula measuredNeutralFormula) {
        for (FormulaFilter filter : filters)
            if (!filter.isValid(measuredNeutralFormula)) return false;
        return true;
    }

    @Override
    public boolean isValid(MolecularFormula measuredNeutralFormula, Ionization ionization) {
        for (FormulaFilter filter : filters)
            if (!filter.isValid(measuredNeutralFormula, ionization)) return false;
        return true;
    }

    @Override
    public boolean isValid(MolecularFormula measuredNeutralFormula, PrecursorIonType ionType) {
        for (FormulaFilter filter : filters)
            if (!filter.isValid(measuredNeutralFormula, ionType)) return false;
        return true;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        filters.clear();
        final Iterator<G> iter = document.iteratorOfList(document.getListFromDictionary(dictionary, "filters"));
        while (iter.hasNext()) {
            filters.add((FormulaFilter)helper.unwrap(document, iter.next()));
        }
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        final L list = document.newList();
        for (FormulaFilter f : filters) {
            document.addToList(list, helper.wrap(document, f));
        }
        document.addListToDictionary(dictionary, "filters", list);
    }
}
