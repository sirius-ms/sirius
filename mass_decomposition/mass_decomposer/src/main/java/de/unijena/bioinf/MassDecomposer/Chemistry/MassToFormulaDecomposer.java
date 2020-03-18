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
import de.unijena.bioinf.MassDecomposer.DecompIterator;
import de.unijena.bioinf.MassDecomposer.Interval;
import de.unijena.bioinf.MassDecomposer.RangeMassDecomposer;
import de.unijena.bioinf.MassDecomposer.ValencyAlphabet;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MassToFormulaDecomposer extends RangeMassDecomposer<Element> {

    protected final ChemicalAlphabet alphabet;

    public MassToFormulaDecomposer() {
        this(new ChemicalAlphabet());
    }

    public MassToFormulaDecomposer(ChemicalAlphabet alphabet) {
        super(new ChemicalAlphabetWrapper(alphabet));
        this.alphabet = alphabet;
    }

    public int[] getOrderedCharacterIds() {
        return orderedCharacterIds;
    }

    public Iterator<MolecularFormula> neutralMassFormulaIterator(double measuredMass,  Deviation deviation, final FormulaConstraints constraints) {
        return formulaIterator(measuredMass, PeriodicTable.getInstance().neutralIonization(), deviation, constraints);
    }

    public Iterator<MolecularFormula> formulaIterator(double measuredMass, Ionization ionization,  Deviation deviation, final FormulaConstraints constraints) {
        final Map<Element, Interval> boundaries = getBoundaries(constraints);
        final double neutralMass = ionization.subtractFromMass(measuredMass);
        final DecompIterator<Element> decompIterator = decomposeIterator(neutralMass, deviation, boundaries);
        return new Iterator<MolecularFormula>() {

            MolecularFormula current = fetchNextFormula();

            @Override
            public boolean hasNext() {
                return current!=null;
            }

            @Override
            public MolecularFormula next() {
                final MolecularFormula now = current;
                current = fetchNextFormula();
                return now;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            private MolecularFormula fetchNextFormula() {
                outerLoop:
                while (decompIterator.next()) {
                    final MolecularFormula formula = alphabet.decompositionToFormula(decompIterator.getCurrentCompomere());
                    for (FormulaFilter g : constraints.getFilters()) {
                        if (!g.isValid(formula, ionization))
                            continue outerLoop;
                    }
                    return formula;
                }
                return null;
            }
        };
    }

    public List<MolecularFormula> decomposeNeutralMassToFormulas(double mass, double massTolerance, FormulaConstraints constraints) {
        final Ionization ionization = PeriodicTable.getInstance().neutralIonization();
        return decomposeToFormulas(mass, ionization, massTolerance, getBoundaries(constraints), FormulaFilterList.create(constraints.getFilters()));
    }

    public List<MolecularFormula> decomposeNeutralMassToFormulas(double mass, Deviation deviation, FormulaConstraints constraints) {
        final Ionization ionization = PeriodicTable.getInstance().neutralIonization();
        return decomposeToFormulas(mass, ionization, deviation, getBoundaries(constraints), FormulaFilterList.create(constraints.getFilters()));
    }

    public List<MolecularFormula> decomposeToFormulas(double mass, Ionization ionization, double massTolerance, FormulaConstraints constraints) {

        return decomposeToFormulas(mass, ionization, massTolerance, getBoundaries(constraints), FormulaFilterList.create(constraints.getFilters()));
    }

    public List<MolecularFormula> decomposeToFormulas(double mass, Ionization ionization, Deviation deviation, FormulaConstraints constraints) {

        return decomposeToFormulas(mass, ionization, deviation, getBoundaries(constraints), FormulaFilterList.create(constraints.getFilters()));
    }

    private Map<Element, Interval> getBoundaries(FormulaConstraints constraints) {
        final Map<Element, Interval> boundaries = alphabet.toMap();
        if (!constraints.getChemicalAlphabet().equals(alphabet)) {
            for (Element e : constraints.getChemicalAlphabet()) {
                if (constraints.hasElement(e) && constraints.getLowerbound(e)>0 && alphabet.indexOf(e)<0) {
                    throw new IllegalArgumentException("Incompatible alphabet: " + alphabet +  " vs " + constraints);
                }
            }
            for (Element e : alphabet) {
                boundaries.put(e, new Interval(constraints.getLowerbound(e), constraints.getUpperbound(e)));
            }
        } else {
            final int[] upperbounds = constraints.getUpperbounds();
            final int[] lowerbounds = constraints.getLowerbounds();
            for (int i=0; i < alphabet.size(); ++i) {
                boundaries.put(alphabet.get(i), new Interval(lowerbounds[i], upperbounds[i]));
            }
        }
        return boundaries;
    }


    public List<MolecularFormula> decomposeNeutralMassToFormulas(double neutralMass, Deviation deviation) {
        final Ionization ionization = PeriodicTable.getInstance().neutralIonization();
        return decomposeToFormulas(neutralMass, ionization, deviation, null, null);
    }

    public List<MolecularFormula> decomposeNeutralMassToFormulas(double neutralMass, Deviation deviation, Map<Element, Interval> boundaries) {
        final Ionization ionization = PeriodicTable.getInstance().neutralIonization();
        return decomposeToFormulas(neutralMass, ionization, deviation, boundaries, null);
    }

    public List<MolecularFormula> decomposeToFormulas(double mass, Ionization ionization, Deviation deviation) {
        return decomposeToFormulas(mass, ionization, deviation, null, null);
    }

    public List<MolecularFormula> decomposeToFormulas(double mass, Ionization ionization, Deviation deviation, Map<Element, Interval> boundaries) {
        return decomposeToFormulas(mass, ionization, deviation, boundaries, null);
    }

    public List<MolecularFormula> decomposeToFormulas(double measuredMass, Ionization ionization, Deviation deviation, Map<Element, Interval> boundaries, final FormulaFilter filter) {
        final Map<Element, Interval> boundaryMap;
        boundaryMap = boundaries;
        double neutralMass = ionization.subtractFromMass(measuredMass);
        final List<int[]> decompositions = super.decompose(neutralMass, deviation, boundaryMap);
        final ArrayList<MolecularFormula> formulas = new ArrayList<MolecularFormula>(decompositions.size());
        for (int[] ary : decompositions) {
            final MolecularFormula formula = alphabet.decompositionToFormula(ary);
            if (filter!=null && !filter.isValid(formula, ionization)) continue;
            formulas.add(formula);
        }
        return formulas;
    }

    public List<MolecularFormula> decomposeToFormulas(double measuredMass, Ionization ionization, double massTolerance, Map<Element, Interval> boundaries, final FormulaFilter filter) {
        if (measuredMass < 0d)
            throw new IllegalArgumentException("Expect positive mass for decomposition: " + measuredMass);
        final Map<Element, Interval> boundaryMap;
        boundaryMap = boundaries;
        double neutralMass = ionization.subtractFromMass(measuredMass);
        final List<int[]> decompositions = super.decompose(Math.max(0,neutralMass-massTolerance), neutralMass+massTolerance, boundaryMap);
        final ArrayList<MolecularFormula> formulas = new ArrayList<MolecularFormula>(decompositions.size());
        for (int[] ary : decompositions) {
            final MolecularFormula formula = alphabet.decompositionToFormula(ary);
            if (filter!=null && !filter.isValid(formula, ionization)) continue;
            formulas.add(formula);
        }
        return formulas;
    }

    public ChemicalAlphabet getChemicalAlphabet() {
        return alphabet;
    }

    @Override
    public ValencyAlphabet<Element> getAlphabet() {
        return new ChemicalAlphabetWrapper(alphabet);
    }
}
