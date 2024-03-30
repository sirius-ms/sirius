
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
import de.unijena.bioinf.ChemistryBase.chem.utils.FormulaVisitor;
import de.unijena.bioinf.ChemistryBase.chem.utils.UnknownElementException;
import de.unijena.bioinf.ChemistryBase.utils.SimpleSerializers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.nio.ShortBuffer;
import java.util.*;
import java.util.function.Consumer;

/**
 * Basic class for molecular formulas.
 * The concrete implementation is handled by its subclass. User should use the factory methods
 * instead of accessing the subclasses directly.
 * <pre>
 *     MolecularFormula f = MolecularFormula.parse("C6H12O6");
 * </pre>
 * <p>
 * A molecular formula describes a sum formula, which is a multiset or compomere containing
 * elements and their amount.
 * @author Kai Dührkop
 */
@JsonSerialize(using = ToStringSerializer.class)
@JsonDeserialize(using = SimpleSerializers.MolecularFormulaDeserializer.class)
public abstract class MolecularFormula implements Cloneable, Iterable<Element>, Comparable<MolecularFormula> {

    private static MolecularFormula Hydrogen;

    public static MolecularFormula getHydrogen() {
        if (Hydrogen == null) {
            try {
                Hydrogen = parse("H");
            } catch (UnknownElementException e) {
                throw new RuntimeException(e);
            }
        }
        return Hydrogen;

    }

    /**
     * returns a new immutable molecular formula of the given formula
     */
    public static MolecularFormula from(MolecularFormula formula) {
        return new ImmutableMolecularFormula(formula);
    }

    public static MolecularFormula emptyFormula() {
        return PeriodicTable.getInstance().emptyFormula();
    }

    /**
     * Converts a map of elements to amounts into a molecular formula
     *
     * @param map
     * @return
     */
    public static MolecularFormula fromElementMap(Map<Element, Integer> map) {
        final PeriodicTable table = PeriodicTable.getInstance();
        int maxid = 0;
        for (Element e : map.keySet()) maxid = Math.max(maxid, e.getId());
        final BitSet bitset = new BitSet(maxid + 1);
        for (Element e : map.keySet()) bitset.set(e.getId());
        final TableSelection selection = table.getSelectionFor(bitset);
        final short[] buffer = new short[selection.size()];
        for (int i = 0; i < buffer.length; ++i) {
            final Element e = selection.get(i);
            final Integer amount = map.get(e);
            if (amount != null) {
                if (amount > Short.MAX_VALUE) {
                    throw new IllegalArgumentException("Cannot represent a molecular formula with " + amount + " number of atoms per element");
                }
                buffer[i] = amount.shortValue();
            }
        }
        return new ImmutableMolecularFormula(selection, buffer);
    }

    /**
     * build a new molecular formula from an array and a table selection. The array is copied during the
     * allocation.
     */
    public static MolecularFormula fromCompomer(TableSelection selection, short[] compomer) {
        if (compomer.length > selection.size())
            throw new IllegalArgumentException("Compomer is not compatible to table selection.");
        return new ImmutableMolecularFormula(selection, compomer);
    }

    /**
     * build a new molecular formula from an array and a table selection. A new short array
     * is created during allocation.
     */
    public static MolecularFormula fromCompomer(TableSelection selection, int[] compomer) {
        final short[] buffer = new short[compomer.length];
        for (int i = 0; i < buffer.length; ++i) {
            final int x = compomer[i];
            if (x < Short.MIN_VALUE || x > Short.MAX_VALUE) {
                throw new IllegalArgumentException();
            }
            buffer[i] = (short) x;
        }
        return fromCompomer(selection, buffer);
    }

    public static MolecularFormula singleElement(Element e, int amount) {
        if (amount > Short.MAX_VALUE)
            throw new IllegalArgumentException("amount of " + e.getName() + " have to be smaller or equal to " + Short.MAX_VALUE + ", but " + amount + " given.");
        final PeriodicTable t = PeriodicTable.getInstance();
        final BitSet set = new BitSet(e.getId() + 1);
        set.set(e.getId());
        final TableSelection sel = t.getSelectionFor(set);
        final short[] buffer = new short[sel.indexOf(e) + 1];
        buffer[sel.indexOf(e)] = (short) amount;
        return new ImmutableMolecularFormula(sel, buffer);
    }

    public static MolecularFormula singleElement(Element e) {
        return singleElement(e, 1);
    }

    /**
     * creates a new molecular formula from a given string. This should be the preferred way
     * to create molecular formulas. Typical strings which are recognized are
     * "CH4", "NOH(CH2)4COOH", "CH4(C(H)2)8CH4", ""
     * Modifiers as iondetection or isotopes are not recognized. For example "Fe+3" or "13C" are no valid
     * molecular formulas.
     */
    public static MolecularFormula parse(String text) throws UnknownElementException {
        return parse(text, PeriodicTable.getInstance());
    }

    public static MolecularFormula parseOrThrow(String text) {
        try {
            return parse(text);
        } catch (UnknownElementException e) {
            throw new RuntimeException(e);
        }
    }

    public static MolecularFormula parseOrNull(@NotNull final String textFormula) {
        try {
            return parse(textFormula);
        } catch (UnknownElementException e) {
            LoggerFactory.getLogger(MolecularFormula.class).warn("Cannot parse Formula `" + textFormula + "`.", e);
            return null;
        }
    }


    /**
     * parse and executors return false if parsing failed and no execution happend
     */
    public static boolean parseAndExecute(@NotNull final String textFormula, @Nullable Consumer<MolecularFormula> executeOrSkip) {
        try {
            final MolecularFormula formula = parse(textFormula);
            if (executeOrSkip != null)
                executeOrSkip.accept(formula);
            return true;
        } catch (UnknownElementException e) {
            LoggerFactory.getLogger(MolecularFormula.class).warn("Cannot parse Formula `" + textFormula + "`. Skipping this entry!", e);
        }
        return false;
    }


    static MolecularFormula parse(String text, PeriodicTable pt) throws UnknownElementException {
        final ArrayList<Pair> pairs = new ArrayList<Pair>();
        pt.parse(text, (element, amount) -> {
            pairs.add(new Pair(element, amount));
            return null;
        });
        final BitSet bitset = new BitSet(pairs.size());
        for (Pair e : pairs) bitset.set(e.element.getId());
        final TableSelection sel = pt.cache.getSelectionFor(bitset);
        final short[] buffer = new short[sel.size()];
        for (Pair e : pairs) {
            buffer[sel.indexOf(e.element)] += e.amount;
        }
        return new ImmutableMolecularFormula(sel, buffer);
    }

    /**
     * returns the table selection which gives information about the memory structure of the formula
     */
    public abstract TableSelection getTableSelection();

    /**
     * the array with the amounts of each element. The mapping between the array indizes and
     * the element is done by the table selection.
     */
    protected abstract short[] buffer();

    /**
     * returns the monoisotopic mass of the formula. NOT THE AVERAGE MASS!
     */
    public double getMass() {
        return calcMass();
    }

    /**
     * rounds the mass to an integer value
     */
    public int getIntMass() {
        return calcIntMass();
    }

    protected final int calcIntMass() {
        int sum = 0;
        final short[] amounts = buffer();
        final TableSelection selection = getTableSelection();
        for (int i = 0; i < amounts.length; i++) {
            sum += selection.get(i).getIntegerMass() * amounts[i];
        }
        return sum;
    }

    protected final double calcMass() {
        double sum = 0d;
        final short[] amounts = buffer();
        final TableSelection selection = getTableSelection();
        for (int i = 0; i < amounts.length; ++i) {
            sum += selection.weightOf(i) * amounts[i];
        }
        return sum;
    }

    /**
     * build a list of elements this formula contains. Each call of this method builds a new list.
     */
    public List<Element> elements() {
        final TableSelection selection = getTableSelection();
        final ArrayList<Element> elements = new ArrayList<Element>(selection.size());
        final short[] buffer = buffer();
        for (int i = 0; i < buffer.length; ++i) {
            if (buffer[i] > 0) elements.add(selection.get(i));
        }
        return elements;
    }

    /**
     * @return number of different elements in this molecular formula
     */
    public int getNumberOfElements() {
        int i=0;
        for (short val : buffer())
            if (val > 0)
                ++i;
        return i;
    }

    /**
     * build an array of elements this formula contains. Each call of this method builds a new array.
     */
    public Element[] elementArray() {
        final TableSelection selection = getTableSelection();
        final ArrayList<Element> elements = new ArrayList<Element>(selection.size());
        final short[] buffer = buffer();
        for (int i = 0; i < buffer.length; ++i) {
            if (buffer[i] > 0) elements.add(selection.get(i));
        }
        return elements.toArray(new Element[elements.size()]);
    }

    /**
     * returns the number of atoms the formula contains for a given element
     */
    public int numberOf(Element element) {
        final int index = getTableSelection().getIndexIfExist(element);
        return (index < 0 || index >= buffer().length) ? 0 : buffer()[index];
    }

    /**
     * returns the number of atoms the formula contains for a given element
     * This method is slower than #numberOf(Element)!
     */
    public int numberOf(String element) {
        final int index = getTableSelection().getIndexIfExist(PeriodicTable.getInstance().getByName(element));
        return (index < 0 || index >= buffer().length) ? 0 : buffer()[index];
    }

    /**
     * The ring-double-bond-equation is the maximal number of free electron/valence-pairs in any molecular
     * graph of this formula. It is the halve of the {{@link #doubledRDBE()}.
     *
     * @return
     */
    public float rdbe() {
        return doubledRDBE() / 2f;
    }

    /**
     * the doubled ring-double-bond-equation is the maximal number of
     * not-satisfied valences in the molecular graph.
     * <p>
     * <p>
     * For example: In C2H6 each C atom has 4 valences, each H atom has 1 valence. We do not know
     * the molecular structure, but we know that the sum of all valences is 14, while each but one
     * atom consumes at least two valences (for a bond to another atom) such that the graph is fully connected. Therefore
     * the number of not satisfied valence-pairs is 14-(7*2) = 0.
     * <p>
     * If the number is odd, then the molecule is charged because there is one free electron. If the number
     * is negative, then there are free atoms which cannot be connected to the graph. Usually, we forbid
     * such molecules in our application.
     */
    public int doubledRDBE() {
        int rdbe = 2;
        final short[] amounts = buffer();
        final TableSelection selection = getTableSelection();
        for (int i = 0; i < amounts.length; ++i) {
            rdbe += amounts[i] * (selection.valenceOf(i) - 2);
        }
        return rdbe;
    }

    /**
     * number of atoms in this formula
     */
    public int atomCount() {
        int sum = 0;
        final short[] amounts = buffer();
        for (int i = 0; i < amounts.length; ++i) {
            sum += amounts[i];
        }
        return sum;
    }

    /**
     * absolute number of atoms in this formula. e.g. C5H-2 counts 7, not 3
     */
    public int absAtomCount() {
        int sum = 0;
        final short[] amounts = buffer();
        for (int i = 0; i < amounts.length; ++i) {
            sum += Math.abs(amounts[i]);
        }
        return sum;
    }


    public boolean isEmpty() {
        final short[] amounts = buffer();
        for (int i=0; i < amounts.length; ++i) {
            if (amounts[i]!=0) return false;
        }
        return true;
    }

    public boolean isCHNO() {
        return atomCount() <= numberOfCarbons() + numberOfHydrogens() + numberOfNitrogens() + numberOfOxygens();
    }
    public boolean isCHO() {
        return atomCount() <= numberOfCarbons() + numberOfHydrogens() + numberOfOxygens();
    }

    public boolean isCHNOPS() {
        return atomCount() <= numberOfCarbons() + numberOfHydrogens() + numberOfNitrogens() + numberOfOxygens() + numberOf(getTableSelection().getPeriodicTable().getByName("S")) + numberOf(getTableSelection().getPeriodicTable().getByName("P"));
    }

    private static HashSet<String> ALLOWED_ELEMS_CHNOPSBBRClIF = new HashSet<>(Arrays.asList("C", "H", "N", "O", "P", "S", "B", "Br", "Cl", "I", "F"));

    public boolean isCHNOPSBBrClFI() {
        final short[] buf = buffer();
        final TableSelection sel = getTableSelection();
        for (int k = 0; k < buf.length; ++k) {
            if (buf[k] > 0 && !ALLOWED_ELEMS_CHNOPSBBRClIF.contains(sel.get(k).getSymbol())) {
                return false;
            }
        }
        return true;
    }

    /**
     * A formula is charged if its rdbe value is not a whole-number (for example if it is 0.5)
     * or if its doubled-rdbe is an odd number.
     *
     * @return true, if formula is charged, false if formula is neutral
     */
    public boolean maybeCharged() {
        return doubledRDBE() % 2 == 1;
    }

    /**
     * @return the ratio of the hydrogen amount to the carbon amount
     */
    public float hydrogen2CarbonRatio() {
        final int carbon = numberOfCarbons();
        final int hydrogen = numberOfHydrogens();
        return (float) hydrogen / (carbon == 0 ? 0.8f : (float) carbon);
    }

    /**
     * @return the ratio of the non-hydrogen and non-carbon atoms to the number carbon atoms
     */
    public float hetero2CarbonRatio() {
        final int carbon = numberOfCarbons();
        final int hetero = atomCount() - carbon - numberOfHydrogens();
        return (float) hetero / (carbon == 0 ? 0.8f : (float) carbon);
    }

    public float heteroWithoutOxygenToCarbonRatio() {
        final int carbon = numberOfCarbons();
        final int hetero = atomCount() - (carbon + numberOfHydrogens() + numberOfOxygens());
        return (float) hetero / (carbon == 0 ? 0.8f : (float) carbon);
    }

    /**
     * @return the ratio of the non-hydrogen and non-oxygen to the number of oxygens
     */
    public float hetero2OxygenRatio() {
        final int oxygen = numberOfOxygens();
        final int hetero = atomCount() - oxygen - numberOfHydrogens();
        return (float) hetero / (oxygen == 0 ? 0.8f : (float) oxygen);
    }

    /**
     * @return number of hydrogen atoms
     */
    public int numberOfHydrogens() {
        final int hi = getTableSelection().hydrogenIndex();
        return buffer().length > hi ? buffer()[hi] : 0;
    }

    /**
     * @return number of oxygen atoms
     */
    public int numberOfOxygens() {
        final int hi = getTableSelection().oxygenIndex();
        return buffer().length > hi ? buffer()[hi] : 0;
    }

    public int numberOfNitrogens() {
        final int ni = getTableSelection().nitrogenIndex();
        return buffer().length > ni ? buffer()[ni] : 0;
    }

    /**
     * @return number of carbon atoms
     */
    public int numberOfCarbons() {
        final int ci = getTableSelection().carbonIndex();
        return buffer().length > ci ? buffer()[ci] : 0;
    }

    /**
     * copies the content of the formula into the given buffer to the given offset. The mapping between
     * index and element is done by the {{@link TableSelection}} class.
     */
    public void copyToBuffer(short[] buffer, int offset) {
        final short[] amounts = buffer();
        if (buffer.length - offset < amounts.length) throw new IndexOutOfBoundsException("buffer is to small");
        System.arraycopy(amounts, 0, buffer, offset, amounts.length);
    }

    public short[] copyToBuffer() {
        return buffer().clone();
    }

    public void copyToBuffer(ShortBuffer buffer) {
        buffer.put(buffer());
    }


    /**
     * a formula is subtractable from another formula, if for each element in the
     * periodic table the amount of atoms of this element is greater or equal to the other formula.
     * for all elements ei is (this(ei) - other(ei)) {@literal >=} 0
     */
    public boolean isSubtractable(MolecularFormula other) {
        if (getMass() < other.getMass()) return false;
        final short[] amounts = buffer();
        final TableSelection selection = getTableSelection();
        if (selection != other.getTableSelection()) return isSubtractableInc(other);
        final short[] otheram = other.buffer();
        if (otheram.length > amounts.length) {
            int i = otheram.length - 1;
            while (otheram[i] == 0) --i;
            if (i >= amounts.length) return false;
        }
        final int n = Math.min(otheram.length, amounts.length);
        for (int i = 0; i < n; ++i) {
            if (amounts[i] < otheram[i]) return false;
        }
        return true;
    }

    /**
     * checks if the molecular formula consists of the given subset and an arbitrary number
     * of additional carbon and/or hydrogen atoms
     */
    public boolean isCarbonHydrogenPlusSubset(MolecularFormula other) {
        final short[] amounts = buffer();
        final short[] amounts2 = other.buffer();
        final TableSelection sel = getTableSelection();
        final TableSelection sel2 = other.getTableSelection();
        final int carbon = sel2.carbonIndex();
        final int hydrogen = sel2.hydrogenIndex();
        int atoms = 0;
        for (short val : amounts) atoms += val;
        atoms -= numberOfCarbons();
        atoms -= numberOfHydrogens();
        for (int i = 0; i < amounts2.length; ++i) {
            if (amounts2[i] > 0) {
                final Element elem = sel2.get(i);
                final int index = sel.getIndexIfExist(elem);
                if (index < 0 || amounts.length <= index || amounts[index] < amounts2[i]) return false;
                else if (i != carbon && i != hydrogen) {
                    atoms -= amounts2[i];
                }
            }
        }
        return atoms == 0;
    }

    private boolean isSubtractableInc(MolecularFormula other) {
        final short[] amounts = buffer();
        final short[] amounts2 = other.buffer();
        final TableSelection sel = getTableSelection();
        final TableSelection sel2 = other.getTableSelection();
        for (int i = 0; i < amounts2.length; ++i) {
            if (amounts2[i] > 0) {
                final Element elem = sel2.get(i);
                final int index = sel.getIndexIfExist(elem);
                if (index < 0 || amounts.length <= index || amounts[index] < amounts2[i]) return false;
            }
        }
        return true;
    }

    public boolean isAllNonPositive() {
        final short[] amounts = buffer();
        for (int i = 0; i < amounts.length; ++i) {
            if (amounts[i] > 0) return false;
        }
        return true;
    }

    public boolean isAllPositiveOrZero() {
        final short[] amounts = buffer();
        for (int i = 0; i < amounts.length; ++i) {
            if (amounts[i] < 0) return false;
        }
        return true;
    }

    /*
            checks if the given formula is a subset of this formula
         */
    public boolean contains(final MolecularFormula other) {
        if (other == null || other.equals(MolecularFormula.emptyFormula())) return true;
        final short[] amounts = buffer();
        final TableSelection selection = getTableSelection();
        final short[] otherAmounts = other.buffer();
        final TableSelection otherSelection = other.getTableSelection();

        for (int i = 0; i < otherAmounts.length; i++) {
            int j = selection.indexOf(otherSelection.get(i));
            if (j < 0 || j >= amounts.length || amounts[j] < otherAmounts[i])
                return false;
        }
        return true;
    }

    /*
        Returns a new formula consisting of the maximum of each element of the single formulas
     */
    public MolecularFormula union(MolecularFormula other) {
        if (other.isEmpty()) return this;
        final short[] amounts = buffer();
        final TableSelection selection = getTableSelection();
        final short[] otherAmounts = other.buffer();
        final TableSelection otherSelection = other.getTableSelection();
        final TableSelection newSelection;
        final short[] nrs;
        if (selection != otherSelection) {
            // TODO: Handle special case where selection is subset of otherSelection
            final BitSet sel = selection.getBitMask();
            sel.or(otherSelection.getBitMask());
            newSelection = selection.getPeriodicTable().getSelectionFor(sel);
            nrs = new short[newSelection.numberOfElements()];
            for (int i = 0; i < amounts.length; ++i) {
                nrs[newSelection.indexOf(selection.get(i))] = amounts[i];
            }
            for (int i = 0; i < otherAmounts.length; ++i) {
                final int k = newSelection.indexOf(otherSelection.get(i));
                nrs[k] = (short) Math.max(nrs[k], otherAmounts[i]);
            }
        } else {
            newSelection = selection;
            if (amounts.length < otherAmounts.length) return other.union(this);
            nrs = Arrays.copyOf(amounts, amounts.length);
            for (int i = 0; i < otherAmounts.length; ++i) {
                nrs[i] = (short) Math.max(nrs[i], otherAmounts[i]);
            }
        }
        return new ImmutableMolecularFormula(newSelection, nrs);
    }

    /*
        Returns the number of difference non-hydrogen atoms in both molecules
     */
    public int numberOfDifferenceHeteroAtoms(MolecularFormula other) {
        final short[] amounts = buffer();
        final TableSelection selection = getTableSelection();
        final short[] otherAmounts = other.buffer();
        final TableSelection otherSelection = other.getTableSelection();
        int count = 0;
        if (selection != otherSelection) {
            final Counter counter = new Counter(other);
            visit(counter);
            final int hydrogenDifference = Math.abs(other.numberOfHydrogens() - numberOfHydrogens());
            return counter.count - hydrogenDifference;
        } else {
            final int hydrogenIndex = selection.hydrogenIndex();
            for (int k = 0; k < Math.min(amounts.length, otherAmounts.length); ++k) {
                if (k != hydrogenIndex) count += Math.abs(amounts[k] - otherAmounts[k]);
            }
            final short[] bigger = amounts.length > otherAmounts.length ? amounts : otherAmounts;
            for (int k = Math.min(amounts.length, otherAmounts.length); k < bigger.length; ++k) {
                if (k != hydrogenIndex) count += bigger[k];
            }
            return count;
        }
    }

    /*
        Returns the number of difference non-hydrogen atoms in both molecules
     */
    public int numberOfDifferentNonCarbonHydrogenAtoms(MolecularFormula other) {
        final short[] amounts = buffer();
        final TableSelection selection = getTableSelection();
        final short[] otherAmounts = other.buffer();
        final TableSelection otherSelection = other.getTableSelection();
        int count = 0;
        if (selection != otherSelection) {
            final Counter counter = new Counter(other);
            visit(counter);
            final int hydrogenDifference = Math.abs(other.numberOfHydrogens() - numberOfHydrogens());
            final int carbonDifference = Math.abs(other.numberOfCarbons() - numberOfCarbons());
            return counter.count - (hydrogenDifference + carbonDifference);
        } else {
            final int hydrogenIndex = selection.hydrogenIndex();
            final int carbonIndex = selection.carbonIndex();
            for (int k = 0; k < Math.min(amounts.length, otherAmounts.length); ++k) {
                if (k != hydrogenIndex && k != carbonIndex) count += Math.abs(amounts[k] - otherAmounts[k]);
            }
            final short[] bigger = amounts.length > otherAmounts.length ? amounts : otherAmounts;
            for (int k = Math.min(amounts.length, otherAmounts.length); k < bigger.length; ++k) {
                if (k != hydrogenIndex && k != carbonIndex) count += bigger[k];
            }
            return count;
        }
    }

    /**
     * returns a new formula containing the atoms of both formulas
     */
    public MolecularFormula add(MolecularFormula other) {
        if (other.isEmpty()) return this;
        if (isEmpty()) return other;
        final short[] amounts = buffer();
        final TableSelection selection = getTableSelection();
        final short[] otherAmounts = other.buffer();
        final TableSelection otherSelection = other.getTableSelection();
        final TableSelection newSelection;
        final short[] nrs;
        if (selection != otherSelection) {
            // TODO: Handle special case where selection is subset of otherSelection
            final BitSet sel = selection.getBitMask();
            sel.or(otherSelection.getBitMask());
            newSelection = selection.getPeriodicTable().getSelectionFor(sel);
            nrs = new short[newSelection.numberOfElements()];
            for (int i = 0; i < amounts.length; ++i) {
                nrs[newSelection.indexOf(selection.get(i))] = amounts[i];
            }
            for (int i = 0; i < otherAmounts.length; ++i) {
                nrs[newSelection.indexOf(otherSelection.get(i))] += otherAmounts[i];
            }
        } else {
            newSelection = selection;
            if (amounts.length < otherAmounts.length) return other.add(this);
            nrs = Arrays.copyOf(amounts, amounts.length);
            for (int i = 0; i < otherAmounts.length; ++i) {
                nrs[i] += otherAmounts[i];
            }
        }
        return new ImmutableMolecularFormula(newSelection, nrs);
    }

    /**
     * negates the amounts of the formula. Although a formula with negative atom count values has no
     * chemical meaning, it may be useful in some applications.
     */
    public MolecularFormula negate() {
        return multiply(-1);
    }

    /**
     * returns a new molecular formula by subtracting for each element the number in self with
     * the number in other.
     * If both formulas are {{@link #isSubtractable(MolecularFormula)}},
     * the result is a subformula which appears after other is cut of from self.
     */
    public MolecularFormula subtract(MolecularFormula other) {
        if (other.isEmpty()) return this;
        final short[] amounts = buffer();
        final TableSelection selection = getTableSelection();
        final short[] otherAmounts = other.buffer();
        final TableSelection otherSelection = other.getTableSelection();
        if (selection != otherSelection) return add(other.negate()); // TODO: improve performance
        final short[] nrs = Arrays.copyOf(amounts, Math.max(amounts.length, otherAmounts.length));
        for (int i = 0; i < otherAmounts.length; ++i) {
            nrs[i] -= otherAmounts[i];
        }
        return new ImmutableMolecularFormula(selection, nrs);
    }

    /**
     * returns a new molecular formula in which the amount of each element
     * is multiplied with the given scalar.
     */
    public MolecularFormula multiply(int scalar) {
        if (scalar == 1) return this;
        if (scalar == 0) return emptyFormula();
        final short[] nrs = Arrays.copyOf(buffer(), buffer().length);
        for (int i = 0; i < nrs.length; ++i) nrs[i] *= scalar;
        return new ImmutableMolecularFormula(getTableSelection(), nrs);
    }

    /**
     * returns a new molecular formula in which the amount of each element
     * is divided with the given scalar. Throws an exception if
     * division is not possible without remainer.
     */
    public MolecularFormula divide(int scalar) {
        if (scalar == 1) return this;
        if (scalar == 0)
            throw new IllegalArgumentException("Division by zero not possible");
        final short[] nrs = Arrays.copyOf(buffer(), buffer().length);
        for (int i = 0; i < nrs.length; ++i) {
            if (nrs[i] % scalar != 0)
                throw new IllegalArgumentException(toString() + " cannot be divided by " + scalar);
            nrs[i] /= scalar;
        }
        return new ImmutableMolecularFormula(getTableSelection(), nrs);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (o instanceof MolecularFormula) {
            boolean aus = equals((MolecularFormula) o);
            return aus;

        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = (int) getMass() * 1500450271;
        final short[] amounts = buffer();
        final TableSelection selection = getTableSelection();
        for (int i = 0; i < amounts.length; ++i) {
            if (amounts[i] != 0)
                hash ^= (amounts[i] << (selection.get(i).getId() % 16)); // Bits nicht gleichmäßig verteilt. Überarbeiten!
        }
        return hash;
    }

    public boolean equals(MolecularFormula formula) {
        if (formula == this) return true;
        if (formula == null) return false;
        final short[] amounts = buffer();
        final short[] otherAmounts = formula.buffer();
        if (amounts.length == 0 && otherAmounts.length == 0) return true;
        final TableSelection selection = getTableSelection();
        final TableSelection otherSelection = formula.getTableSelection();
        if ((long) getMass() != (long) formula.getMass()) return false;
        if (selection == otherSelection) {
            return Arrays.equals(amounts, otherAmounts);
        } else {
            for (int i = 0; i < amounts.length; ++i) {
                if (amounts[i] != 0 && amounts[i] != formula.numberOf(selection.get(i))) return false;
            }
            for (int i = 0; i < otherAmounts.length; ++i) {
                if (otherAmounts[i] != 0 && otherAmounts[i] != numberOf(otherSelection.get(i))) return false;
            }
            return true;
        }
    }

    /**
     * Standard output format for formulas: In Hill the formula is formated as sequence of elements with
     * their amount. The first element is C, the second is H, all further elements are sorted alphabetically.
     * For single-amount elements the number is skipped. For zero-amount elements both number and element
     * symbol is skipped. Example: CH4, H2, NOH, Fe
     * <p>
     * TODO: Handle special cases of negative amounts!
     */
    public String formatByHill() {
        final short[] amounts = buffer();
        final TableSelection selection = getTableSelection();
        final StringBuilder buffer = new StringBuilder(3 * amounts.length);
        final int c = numberOfCarbons();
        final boolean hasCarbon = c != 0;
        final Element[] elements = new Element[Math.max(0, hasCarbon ? amounts.length - 2 : amounts.length)];
        int k = 0;
        for (int i = 0; i < amounts.length; ++i) {
            if ((i == selection.hydrogenIndex() && hasCarbon) || i == selection.carbonIndex()) continue;
            if (amounts[i] != 0) {
                elements[k++] = selection.get(i);
            }
        }
        Arrays.sort(elements, 0, k, new Comparator<Element>() {
            @Override
            public int compare(Element o1, Element o2) {
                return o1.getSymbol().compareTo(o2.getSymbol());
            }
        });
        final int h = numberOfHydrogens();
        if (hasCarbon) {
            if (c < 0) buffer.append("-");
            buffer.append(selection.get(selection.carbonIndex()).getSymbol());
            if (Math.abs(c) > 1) {
                buffer.append(c);
            }
            if (h != 0) {
                if (h < 0) buffer.append("-");
                buffer.append(selection.get(selection.hydrogenIndex()).getSymbol());
            }
            if (Math.abs(h) > 1) {
                buffer.append(Math.abs(h));
            }
        }
        for (int i = 0; i < k; ++i) {
            final int n = numberOf(elements[i]);
            if (n < 0) buffer.append("-");
            buffer.append(elements[i]);
            if (Math.abs(n) > 1) buffer.append(n);
        }
        return buffer.toString();
    }

    @Override
    public String toString() {
        return formatByHill();
    }

    public MolecularFormula clone() {
        return new ImmutableMolecularFormula(this);
    }

    /**
     * Calls {{@link FormulaVisitor#visit(Element, int)}} for each (element, amount) pair in the
     * sum formula.
     */
    public void visit(FormulaVisitor<?> visitor) {
        final short[] buffer = buffer();
        final TableSelection sel = getTableSelection();
        for (int i = 0; i < buffer.length; ++i) {
            if (buffer[i] != 0) visitor.visit(sel.get(i), buffer[i]);
        }
    }

    @Override
    public Iterator<Element> iterator() {
        return new Iterator<Element>() {

            private int index = 0;
            private final TableSelection selection = getTableSelection();
            private final short[] buffer = buffer();

            @Override
            public boolean hasNext() {
                for (; index < buffer.length; index++) {
                    if (buffer[index] != 0)
                        return true;
                }
                return false;
            }

            @Override
            public Element next() {
                for (; index < buffer.length; index++) {
                    if (buffer[index] != 0)
                        return selection.get(index++);
                }
                throw new NoSuchElementException();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }


    /**
     * Two molecular formulas are only equal iff they are the same
     * formula. This is even the case if their masses are "equal" (which
     * should never happen for different formulas and infinite precision).
     */
    @Override
    public int compareTo(MolecularFormula o) {
        if (equals(o)) return 0;
        if (getMass() < o.getMass()) return -1;
        if (getMass() > o.getMass()) return 1;
        return Integer.compare(hashCode(),o.hashCode());
    }

    /**
     * @return a copy of this molecular formula with number of hydrogen is set to zero
     */
    public MolecularFormula withoutHydrogen() {
        final TableSelection sel = getTableSelection();
        final short[] copy = buffer().clone();
        final int hi = sel.hydrogenIndex();
        if (hi < copy.length) copy[sel.hydrogenIndex()] = 0;
        return new ImmutableMolecularFormula(sel, copy);
    }

    /**
     * @return a copy of this molecular formula with the given elements set to zero
     */
    public MolecularFormula without(Element... elems) {
        final TableSelection sel = getTableSelection();
        final short[] copy = buffer().clone();
        for (Element e : elems) {
            final int j = sel.getIndexIfExist(e);
            if (j >= 0 && j < copy.length) copy[j] = 0;
        }
        return new ImmutableMolecularFormula(sel, copy);
    }

    private final static class Counter implements FormulaVisitor<Object> {
        private int count = 0;
        private MolecularFormula other;

        private Counter(MolecularFormula other) {
            this.other = other;
        }

        @Override
        public Object visit(Element element, int amount) {
            count += Math.abs(other.numberOf(element) - amount);
            return null;
        }
    }

    private static class Pair {
        private final Element element;
        private final int amount;

        private Pair(Element element, int amount) {
            this.element = element;
            this.amount = amount;
        }
    }


}
