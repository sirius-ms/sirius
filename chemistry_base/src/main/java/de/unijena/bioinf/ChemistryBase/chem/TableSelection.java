
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

import de.unijena.bioinf.ChemistryBase.chem.utils.ElementMap;

import java.util.*;
import java.util.regex.Matcher;

/**
 * A table selection maps a subset of the periodic table to array indizes. It allows
 * memory-efficient and fast molecular formulas. While a molecular formula is only an array of
 * amounts, (ideally) all molecular formula are sharing the same table selection which maps an array index
 * to a concrete element in the periodic table. Two molecular formulas with completly different set
 * of elements may use different table selections. The {{@link TableSelectionCache}} maps a formula
 * to the best table selection and creates new table selections on demand.
 * 
 */
public class TableSelection implements Cloneable {

    private Element[] entries;
    BitSet bitmask;
    private short[] element2Index;
    private PeriodicTable pt;
    private int carbonIndex=-1, oxygenIndex=-1, hydrogenIndex=-1, nitrogenIndex; // fast access to most important elements

    public static TableSelection fromString(PeriodicTable pt, String s) {
        final Matcher matcher = pt.getPattern().matcher(s);
        final Set<Element> elements = new HashSet<Element>();
        while (matcher.find()) {
            elements.add(pt.getByName(matcher.group(1)));
        }
        return new TableSelection(pt, elements);
    }
    
    int numberOfElements() {
    	return entries.length;
    }
    
    public PeriodicTable getPeriodicTable() {
    	return pt;
    }
    
    public BitSet getBitMask() {
    	return (BitSet)bitmask.clone();
    }

    public TableSelection(PeriodicTable pt, Element... elements) {
        this(pt, Arrays.asList(elements));
    }
    
    private TableSelection(TableSelection other) {
    	this.entries = other.entries.clone();
    	this.bitmask = (BitSet) other.bitmask.clone();
    	this.element2Index = other.element2Index.clone();
    	this.pt = other.pt;
        this.hydrogenIndex = other.hydrogenIndex;
        this.carbonIndex = other.carbonIndex;
        this.oxygenIndex = other.oxygenIndex;
        this.nitrogenIndex = other.nitrogenIndex;
    }

    public TableSelection(PeriodicTable pt, Collection<Element> elements) {
        this(pt,elements,false);
    }

    public TableSelection(PeriodicTable pt, Collection<Element> elements, boolean enforceOrder) {
        this.pt = pt;
        entries = elements.toArray(new Element[elements.size()]);
        if (!enforceOrder) Arrays.sort(entries);
        element2Index = new short[pt.numberOfElements()];
        Arrays.fill(element2Index, (short)-1);
        for (int i=0; i < entries.length; ++i) {
            element2Index[entries[i].getId()] = (short)i;
        }
        this.bitmask = new BitSet(pt.numberOfElements()+1);
        for (Element e : elements) {
        	bitmask.set(e.getId());
        }
        final Element carbon = pt.getByName("C");
        final Element hydrogen = pt.getByName("H");
        final Element oxygen = pt.getByName("O");
        final Element nitrogen = pt.getByName("N");
        if (carbon != null && elements.contains(carbon)) carbonIndex = element2Index[carbon.getId()];
        if (hydrogen != null && elements.contains(hydrogen)) hydrogenIndex = element2Index[hydrogen.getId()];
        if (oxygen != null && elements.contains(oxygen)) oxygenIndex = element2Index[oxygen.getId()];
        if (nitrogen != null && elements.contains(nitrogen)) nitrogenIndex = element2Index[nitrogen.getId()];

    }
    
    @Override
    public TableSelection clone() {
    	return new TableSelection(this);
    }

    public boolean isSubsetOf(TableSelection superset) {
        if (superset.size()<entries.length) return false;
        for (int k=0; k < entries.length; ++k) {
            if (!superset.get(k).equals(entries[k])) return false;
        }
        return true;
    }

    public void replace(TableSelection superset) {
        if (!isSubsetOf(superset)) throw new IllegalArgumentException("Cannot replace table selection " + this + " with " + superset);
        this.entries = superset.entries;
        this.element2Index = superset.element2Index;
        this.bitmask = superset.bitmask;
        this.carbonIndex = superset.carbonIndex;
        this.hydrogenIndex = superset.hydrogenIndex;
        this.oxygenIndex = superset.oxygenIndex;
        this.nitrogenIndex = superset.nitrogenIndex;
    }

    public boolean extendElements(Element... elems) {
        final ArrayList<Element> toExtend = new ArrayList<Element>(elems.length);
        int maxIndex = 0;
        for (Element e : elems) {
            if (e.getId() >= element2Index.length) {
                toExtend.add(e);
                maxIndex = Math.max(maxIndex, e.getId());
            } else if (element2Index[e.getId()] < 0) toExtend.add(e);
        }
        if (toExtend.isEmpty()) return false;
        if (maxIndex >= element2Index.length) {
            element2Index = Arrays.copyOf(element2Index, maxIndex+1);
            Arrays.fill(element2Index, maxIndex, element2Index.length, (short)-1);
        }
        int offset = entries.length;
        this.entries = Arrays.copyOf(entries, offset + toExtend.size());
        for (Element e : toExtend) {
            entries[offset] = e;
            element2Index[e.getId()] = (short)offset;
            ++offset;
            bitmask.set(e.getId());
        }
        if (carbonIndex < 0) {
            final Element carbon = pt.getByName("C");
            if (carbon != null && Arrays.asList(elems).contains(carbon)) carbonIndex = element2Index[carbon.getId()];
        }
        if (hydrogenIndex < 0) {
            final Element hydrogen = pt.getByName("H");
            if (hydrogen != null && Arrays.asList(elems).contains(hydrogen)) hydrogenIndex = element2Index[hydrogen.getId()];
        }
        if (oxygenIndex < 0) {
            final Element oxygen = pt.getByName("O");
            if (oxygen != null && Arrays.asList(elems).contains(oxygen)) oxygenIndex = element2Index[oxygen.getId()];
        }
        if (nitrogenIndex < 0) {
            final Element nitrogen = pt.getByName("N");
            if (nitrogen != null && Arrays.asList(elems).contains(nitrogen)) nitrogenIndex = element2Index[nitrogen.getId()];
        }
        return true;
    }

    public MolecularFormula parse(String s) {
        return parse(s, true);
    }

    public MolecularFormula parse(String s, boolean extend) {
        final Matcher matcher = pt.getPattern().matcher(s);
        short[] amounts = new short[entries.length];
        while (matcher.find()) {
            final Element elem = PeriodicTable.getInstance().getByName(matcher.group(1));
            final int amount = matcher.group(2).isEmpty() ? 1 : Integer.parseInt(matcher.group(2));
            final int index = element2Index[elem.getId()];
            if (index < 0) {
            	if (extend) {
            		extendElements(elem);
            	} else {
            		throw new RuntimeException("Element '" + elem.getSymbol() + "' is not contained in selection");
            	}
            }
            amounts = Arrays.copyOf(amounts, entries.length);
            amounts[element2Index[elem.getId()]] = (short)amount;
        }
        return new ImmutableMolecularFormula(this, amounts);
    }

    public MolecularFormula toFormula(int[] amounts) {
        int length = Math.min(amounts.length, size());
        while (length > 1 &&  amounts[length-1] == 0) --length;
        final short[] compomere = new short[Math.min(amounts.length, size())];
        for (int i=0; i < compomere.length; ++i) {
            if (amounts[i] > Short.MAX_VALUE || amounts[i] < Short.MIN_VALUE)
                throw new ClassCastException("Can't cast int " + amounts[i] + " to short");
            compomere[i] = (short)amounts[i];
        }
        return new ImmutableMolecularFormula(this, compomere);
    }

    public MolecularFormula toFormula(short[] amounts) {
        int length = Math.min(amounts.length, size());
        while (length > 1 &&  amounts[length-1] == 0) --length;
        return new ImmutableMolecularFormula(this, Arrays.copyOf(amounts, length));
    }

    public int hydrogenIndex() {
        return hydrogenIndex;
    }

    public int carbonIndex() {
        return carbonIndex;
    }

    public int oxygenIndex() {
        return oxygenIndex;
    }

    public int nitrogenIndex() {
        return nitrogenIndex;
    }

    public int valenceOf(int i) {
        return entries[i].getValence();
    }
    
    public int size() {
        return entries.length;
    }
    
    public double weightOf(int i) {
        return entries[i].getMass();
    }

    public Element get(int i) {
        return entries[i];
    }

    /**
     * Returns the index of the given element, if it is contained in the selection. Otherwise, return -1.
     * Does not throw an exception.
     * @see #indexOf(Element)
     * @param character
     * @return index of character or -1, if the character is not contained in the selection
     */
    public int getIndexIfExist(Element character) {
        return element2Index[character.getId()];
    }

    public int indexOf(Element character) {
        final int id = element2Index[character.getId()];
        if (id < 0) throw new NoSuchElementException("Selection " + this + " does not contain element '" + character + "'" );
        return id;
    }

    public static int formulaBufferSize(MolecularFormula formula) {
        return formula.buffer().length;
    }

    public short[] makeCompomer() {
        return new short[entries.length];
    }

    public <S> Map<Element, S> toMap() {
        return new ElementMap<S>(this);
    }

    public Iterator<Element> iterator() {
        return Arrays.asList(entries).iterator();
    }

    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder(size()*2 + 20);
        buffer.append("<Selection: ");
        for (Element e : entries) {
            buffer.append(e);
        }
        buffer.append(" >");
        return buffer.toString();
    }

}
