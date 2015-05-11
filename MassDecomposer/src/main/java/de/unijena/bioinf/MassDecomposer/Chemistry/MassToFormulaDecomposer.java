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
package de.unijena.bioinf.MassDecomposer.Chemistry;

import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.chem.utils.FormulaFilterList;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.MassDecomposer.Interval;
import de.unijena.bioinf.MassDecomposer.MassDecomposerFast;
import de.unijena.bioinf.MassDecomposer.ValencyAlphabet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MassToFormulaDecomposer extends MassDecomposerFast<Element> {

    protected final ChemicalAlphabet alphabet;

    public MassToFormulaDecomposer() {
        this(new ChemicalAlphabet());
    }

    public MassToFormulaDecomposer(ChemicalAlphabet alphabet) {
        super(new ChemicalAlphabetWrapper(alphabet));
        this.alphabet = alphabet;
    }

    public List<MolecularFormula> decomposeToFormulas(double mass, Deviation deviation, FormulaConstraints constraints) {
        if (!constraints.getChemicalAlphabet().equals(alphabet)) throw new IllegalArgumentException("Incompatible alphabet");
        final Map<Element, Interval> boundaries = alphabet.toMap();
        final int[] upperbounds = constraints.getUpperbounds();
        for (int i=0; i < alphabet.size(); ++i) {
            boundaries.put(alphabet.get(i), new Interval(0, upperbounds[i]));
        }
        return decomposeToFormulas(mass, deviation, boundaries, FormulaFilterList.create(constraints.getFilters()));
    }

    public List<MolecularFormula> decomposeToFormulas(double mass, Deviation deviation) {
        return decomposeToFormulas(mass, deviation, null, null);
    }

    public List<MolecularFormula> decomposeToFormulas(double mass, Deviation deviation, Map<Element, Interval> boundaries) {
        return decomposeToFormulas(mass, deviation, boundaries, null);
    }

    public List<MolecularFormula> decomposeToFormulas(double mass, Deviation deviation, Map<Element, Interval> boundaries, final FormulaFilter filter) {
        final Map<Element, Interval> boundaryMap;
        boundaryMap = boundaries;
        final List<int[]> decompositions = super.decompose(mass, deviation, boundaryMap);
        final ArrayList<MolecularFormula> formulas = new ArrayList<MolecularFormula>(decompositions.size());
        for (int[] ary : decompositions) {
            final MolecularFormula formula = alphabet.decompositionToFormula(ary);
            if (filter!=null && !filter.isValid(formula)) continue;
            formulas.add(formula);
        }
        return formulas;
    }

    @Override
    public ValencyAlphabet<Element> getAlphabet() {
        return new ChemicalAlphabetWrapper(alphabet);
    }
}
