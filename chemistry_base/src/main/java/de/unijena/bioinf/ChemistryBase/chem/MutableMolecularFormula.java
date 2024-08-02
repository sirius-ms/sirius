
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

package de.unijena.bioinf.ChemistryBase.chem;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import de.unijena.bioinf.ChemistryBase.utils.SimpleSerializers;

import java.util.Arrays;
import java.util.BitSet;

/**
 * A mutable molecular formula which enables the programmer to change the amount of the elements
 * as well as to add new elements into the formula.
 * <p>
 * A mutable formula is created by calling the constructor with another (usually immutable) molecular
 * formula.
 * <p>
 * {{@code
 * MutableMolecularFormula m = new MutableMolecularFormula(MolecularFormula.parse("CH4"));
 * m.setByName("C", 2); // => C2H4
 * MolecularFormula f = MolecularFormula.from(m); // immutable form
 * }}
 */
@JsonSerialize(using = ToStringSerializer.class)
@JsonDeserialize(using = SimpleSerializers.MolecularFormulaDeserializer.class)
public class MutableMolecularFormula extends MolecularFormula {

    private short[] amounts;
    private TableSelection selection;

    /**
     * Build a new ImmutableMolecularFormula using the same formula as the given molecular
     * formula. Of course: Changes in self does not effect the parameter.
     */
    public MutableMolecularFormula(MolecularFormula formula) {
        this.amounts = formula.buffer().clone();
        this.selection = formula.getTableSelection();
    }

    MutableMolecularFormula(TableSelection selection, short[] buffer) {
        this.amounts = buffer.clone();
        this.selection = selection;
    }

    /**
     * Changes the amount of the element given as index. The index have to be valid, which means,
     * there have to be a mapping in the table selection from the index to an element. To add
     * an element which is not contained in the selection, use {@link de.unijena.bioinf.ChemistryBase.chem.MutableMolecularFormula#set(Element, int)} or
     * {@link de.unijena.bioinf.ChemistryBase.chem.MutableMolecularFormula#setByName(String, int)}
     */
    public void setAt(int index, int amount) {
        if (amount < Short.MIN_VALUE || amount > Short.MAX_VALUE)
            throw new RuntimeException("Element number exceeds formula space: " + amount);
        if (index >= amounts.length && index < selection.size()) {
            this.amounts = Arrays.copyOf(amounts, index + 1);
        }
        this.amounts[index] = (short) amount;
    }

    /**
     * Set the amount of the given element. If the element does not exist in the table selection,
     * the selection is either extended or a new selection is choosed.
     */
    public void set(Element element, int amount) {
        final int index = selection.getIndexIfExist(element);
        if (index >= 0) setAt(index, amount);
        else if (amount != 0) extendByElement(element, amount);
    }

    private void extendByElement(Element element, int amount) {
        if (selection.numberOfElements() >= TableSelectionCache.DEFAULT_MAX_COMPOMERE_SIZE) {
            final BitSet set = selection.getBitMask();
            set.set(element.getId());
            this.selection = selection.getPeriodicTable().cache.getSelectionFor(set);
        }
        setAt(selection.indexOf(element), amount);
    }

    /**
     * Set the amount of the element with the given symbol.
     * If the element does not exist in the table selection,
     * the selection is either extended or a new selection is choosed.
     */
    public void setByName(String element, int amount) {
        set(selection.getPeriodicTable().getByName(element), amount);
    }

    @Override
    public TableSelection getTableSelection() {
        return selection;
    }

    @Override
    protected short[] buffer() {
        return amounts;
    }

    public MutableMolecularFormula clone() {
        return new MutableMolecularFormula(this);
    }
}
