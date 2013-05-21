package de.unijena.bioinf.ChemistryBase.chem;

import org.apache.commons.collections.primitives.ArrayIntList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * FormulaConstraints contains all constraints which reduce the size of all possible decompositions of a mass.
 * It contains of:
 * - allowed elements
 * - boundaries for elements
 * - constraints for formulas
 */
public class FormulaConstraints {

    private final ChemicalAlphabet chemicalAlphabet;
    private final int[] upperbounds;
    private final List<FormulaFilter> filters;

    /**
     * A factory method which provides a nice way to instantiate formula constraints, but which is not type-safe. So
     * use it carefully and only if your alphabet is a "literal" or constant.
     *
     * <pre>ChemicalAlphabet.create(myTableSelection, filter1, filter2, "C", "H", "N", "O", "P", 5, "S", 3, "F", 6);</pre>
     * <pre>ChemicalAlphabet.create("C",20, "H", "Fe", 3);</pre>
     *
     * This will compile but throw a runtime exception
     * <pre>ChemicalAlphabet.create(321.02, new int[0], "Bla");</pre>
     *
     * @param varargs parameters in the order: [selection], {filter}, {elementName, [elementUpperbound]}
     * @return chemical alphabet
     */
    public static FormulaConstraints create(Object... varargs) {
        final ArrayList<FormulaFilter> filters = new ArrayList<FormulaFilter>();
        if (varargs.length == 0) return new FormulaConstraints(new ChemicalAlphabet());
        final PeriodicTable t = PeriodicTable.getInstance();
        int i=0;
        TableSelection sel = null;
        final ArrayList<Element> elements = new ArrayList<Element>();
        final ArrayIntList bounds = new ArrayIntList();
        for (; i < varargs.length; ++i) {
            final Object o = varargs[i];
            if (o instanceof Element) {
                elements.add((Element)varargs[i]);
                bounds.add(Integer.MIN_VALUE);
            } else if (o instanceof String) {
                elements.add(t.getByName((String)varargs[i]));
                bounds.add(Integer.MIN_VALUE);
            } else if (o instanceof Integer) {
                final int k=bounds.size()-1;
                if (k < 0 || bounds.get(k) != Integer.MIN_VALUE)
                    throw new IllegalArgumentException("Illegal format of parameters. Allowed is: [tableselection], {element, [number]}");
                bounds.set(k, (Integer)varargs[i]);
            } else if (o instanceof TableSelection) {
                if (sel != null) throw new IllegalArgumentException("Multiple table selections given for one formula constraints");
                sel = (TableSelection)o;
            } else if (o instanceof FormulaFilter) {
                filters.add((FormulaFilter)o);
            } else {
                throw new IllegalArgumentException("Expect String,Element,Integer,FormulaFilter or TableSelection, but " + i + "th parameter is of type " + o.getClass());
            }
        }
        final Element[] elems = elements.toArray(new Element[elements.size()]);
        final int[] upperbounds = bounds.toArray();
        final TableSelection s = (sel == null) ? t.getSelectionFor(elems) : sel;
        final ChemicalAlphabet alphabet = new ChemicalAlphabet(s, elems);
        return new FormulaConstraints(alphabet, filters);
    }

    public FormulaConstraints(FormulaConstraints c) {
        this(c.getChemicalAlphabet());
        System.arraycopy(c.upperbounds, 0, upperbounds, 0, c.upperbounds.length);
        filters.addAll(c.getFilters());
    }

    public FormulaConstraints(ChemicalAlphabet alphabet) {
        this(alphabet, null);
    }

    public FormulaConstraints(ChemicalAlphabet alphabet, List<FormulaFilter> filters) {
        this.chemicalAlphabet = alphabet;
        this.upperbounds = new int[alphabet.size()];
        Arrays.fill(upperbounds, Integer.MAX_VALUE);
        this.filters = filters == null ? new ArrayList<FormulaFilter>() : new ArrayList<FormulaFilter>(filters);
    }

    public ChemicalAlphabet getChemicalAlphabet() {
        return chemicalAlphabet;
    }

    public int[] getUpperbounds() {
        return upperbounds;
    }

    public void addFilter(FormulaFilter filter) {
        filters.add(filter);
    }

    public List<FormulaFilter> getFilters() {
        return filters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FormulaConstraints that = (FormulaConstraints) o;

        if (chemicalAlphabet != null ? !chemicalAlphabet.equals(that.chemicalAlphabet) : that.chemicalAlphabet != null)
            return false;
        if (filters != null ? !filters.equals(that.filters) : that.filters != null) return false;
        if (!Arrays.equals(upperbounds, that.upperbounds)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = chemicalAlphabet != null ? chemicalAlphabet.hashCode() : 0;
        result = 31 * result + (upperbounds != null ? Arrays.hashCode(upperbounds) : 0);
        result = 31 * result + (filters != null ? filters.hashCode() : 0);
        return result;
    }

    @Override
    public FormulaConstraints clone() {
        return new FormulaConstraints(this);
    }
}
