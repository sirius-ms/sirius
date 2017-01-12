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
package de.unijena.bioinf.ChemistryBase.chem;

import de.unijena.bioinf.ChemistryBase.algorithm.ImmutableParameterized;
import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.utils.FormulaVisitor;
import de.unijena.bioinf.ChemistryBase.chem.utils.ValenceFilter;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import gnu.trove.list.array.TIntArrayList;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * FormulaConstraints contains all constraints which reduce the size of all possible decompositions of a mass.
 * It contains of:
 * - allowed elements
 * - boundaries for elements
 * - constraints for formulas
 *
 * Important: The RDBE Filter is always added to the FormulaConstraints! If you don't want so: either exlude it
 * explicitly by calling constraints.getFilters().clear(); or create the constraints using the constructor
 * new FormulaConstraints(alphabet, new ArrayList());
 *
 * But in application, you probably want the RDBE filter always active. If you just want to change its limit, set
 * it explicitly by calling new FormulaConstraints(alphabet, Arrays.asList(new ValenceFilter(-4)));
 */
public class FormulaConstraints implements ImmutableParameterized<FormulaConstraints> {

    private final ChemicalAlphabet chemicalAlphabet;
    private final int[] upperbounds;
    private final List<FormulaFilter> filters;

    private final static Pattern INTERVAL = Pattern.compile("\\[(\\d+)\\]");

    public FormulaConstraints(String string) {
        final PeriodicTable PT = PeriodicTable.getInstance();
        final Pattern pattern = PT.getPattern();
        final Matcher matcher = pattern.matcher(string);
        if (!matcher.find()) throw new IllegalArgumentException("Invalid alphabet: " + string);
        HashMap<Element, Integer> elements = new HashMap<Element, Integer>();
        while(true) {
            final String m = matcher.group(0);
            if (m.charAt(0)=='(' || m.charAt(0) == ')') throw new IllegalArgumentException("Invalid alphabet: " + string);
            final Element element = PT.getByName(m);
            if (element == null) throw new IllegalArgumentException("Unknown character: " + m);
            final int start = matcher.end();
            final boolean next = matcher.find();
            final int end = next ? matcher.start() : string.length();
            elements.put(element, Integer.MAX_VALUE);
            if (end-start > 0) {
                final Matcher n = INTERVAL.matcher(string.substring(start, end));
                if (n.find()) {
                    final int a = Integer.parseInt(n.group(1));
                    elements.put(element, a);
                }
            }
            if (!next) break;
        }
        this.chemicalAlphabet = new ChemicalAlphabet(elements.keySet().toArray(new Element[0]));
        this.upperbounds = new int[chemicalAlphabet.size()];
        int k=0;
        for (Element e : chemicalAlphabet.getElements()) {
            upperbounds[k++] = elements.get(e);
        }
        this.filters = new ArrayList<FormulaFilter>();
        addFilter(new ValenceFilter());
    }

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
        final TIntArrayList bounds = new TIntArrayList();
        for (; i < varargs.length; ++i) {
            final Object o = varargs[i];
            if (o instanceof Element) {
                elements.add((Element)varargs[i]);
                bounds.add(Integer.MAX_VALUE);
            } else if (o instanceof String) {
                elements.add(t.getByName((String)varargs[i]));
                bounds.add(Integer.MAX_VALUE);
            } else if (o instanceof Integer) {
                final int k=bounds.size()-1;
                if (k < 0 || bounds.get(k) != Integer.MAX_VALUE)
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
        final ChemicalAlphabet alphabet = new ChemicalAlphabet(elements.toArray(new Element[elements.size()]));
        final FormulaConstraints c= new FormulaConstraints(alphabet, filters);
        for (int j=0; i < bounds.size(); ++i) {
            c.upperbounds[alphabet.getElements().indexOf(elements.get(j))] = bounds.get(j);
        }
        return c;
    }

    public FormulaConstraints(FormulaConstraints c) {
        this(c.getChemicalAlphabet());
        System.arraycopy(c.upperbounds, 0, upperbounds, 0, c.upperbounds.length);
        filters.addAll(c.getFilters());
    }

    public static FormulaConstraints allSubsetsOf(MolecularFormula f) {
        final FormulaConstraints c = new FormulaConstraints(new ChemicalAlphabet(f.elementArray()));
        f.visit(new FormulaVisitor<Object>() {
            @Override
            public Object visit(Element element, int amount) {
                c.setUpperbound(element, amount); return null;
            }
        });
        return c;
    }

    public static FormulaConstraints allSubsetsOf(Iterable<MolecularFormula> formulas) {
        final HashSet<Element> elements = new HashSet<Element>();
        for (MolecularFormula f : formulas) {
            elements.addAll(f.elements());
        }
        final FormulaConstraints c = new FormulaConstraints(new ChemicalAlphabet(elements.toArray(new Element[elements.size()])));
        for (Element e : elements) {
            c.setUpperbound(e, 0);
        }
        for (MolecularFormula f : formulas) {
            f.visit(new FormulaVisitor<Object>() {
                @Override
                public Object visit(Element element, int amount) {
                    c.setUpperbound(element, Math.max(c.getUpperbound(element), amount)); return null;
                }
            });
        }
        return c;
    }

    public FormulaConstraints() {
        this(new ChemicalAlphabet());
    }

    public FormulaConstraints(ChemicalAlphabet alphabet) {
        this(alphabet, null);
    }

    public FormulaConstraints(ChemicalAlphabet alphabet, List<FormulaFilter> filters) {
        this.chemicalAlphabet = alphabet;
        this.upperbounds = new int[alphabet.size()];
        Arrays.fill(upperbounds, Integer.MAX_VALUE);
        this.filters = filters == null ? new ArrayList<FormulaFilter>(Arrays.asList(new ValenceFilter())) : new ArrayList<FormulaFilter>(filters);
    }

    public ChemicalAlphabet getChemicalAlphabet() {
        return chemicalAlphabet;
    }

    public int[] getUpperbounds() {
        return upperbounds;
    }

    public int getUpperbound(Element e) {
        final int i = chemicalAlphabet.indexOf(e);
        if (i < 0) return 0;
        return upperbounds[i];
    }

    public void setUpperbound(Element e, int upperbound) {
        final int i = chemicalAlphabet.indexOf(e);
        if (i < 0 && upperbound > 0) {
            throw new NoSuchElementException(e + " is not contained in the chemical alphabet " + chemicalAlphabet);
        }
        upperbounds[i] = upperbound;
    }

    public void addFilter(FormulaFilter filter) {
        filters.add(filter);
    }

    public List<FormulaFilter> getFilters() {
        return filters;
    }

    /**
     * Create a new FormulaConstraints object which alphabet includes the given elements and which
     * upperbounds are the same as for this
     * @param elements
     * @return
     */
    public FormulaConstraints getExtendedConstraints(Element... elements) {
        final Set<Element> elems = new HashSet<Element>(getChemicalAlphabet().getElements());
        elems.addAll(Arrays.asList(elements));
        final FormulaConstraints c = new FormulaConstraints(new ChemicalAlphabet(elems.toArray(new Element[elems.size()])));
        for (Element e :getChemicalAlphabet()) {
            c.setUpperbound(e, getUpperbound(e));
        }
        final ArrayList<FormulaFilter> filters = new ArrayList<FormulaFilter>(getFilters());
        filters.removeAll(c.getFilters());
        for (FormulaFilter f : filters) {
            c.addFilter(f);
        }
        return c;
    }

    /**
     * Create a new FormulaConstraints which allows all molecular formulas which are either satisfy this constraints or
     * otherConstraints
     * @param otherConstraints
     * @return
     */
    public FormulaConstraints getExtendedConstraints(FormulaConstraints otherConstraints) {
        final Set<Element> elems = new HashSet<Element>(otherConstraints.chemicalAlphabet.getElements());
        elems.addAll(chemicalAlphabet.getElements());
        final FormulaConstraints c = new FormulaConstraints(new ChemicalAlphabet(elems.toArray(new Element[elems.size()])));
        for (Element e : elems) {
            c.setUpperbound(e, Math.max(otherConstraints.getUpperbound(e), getUpperbound(e)));
        }
        final ArrayList<FormulaFilter> filters = new ArrayList<FormulaFilter>(getFilters());
        filters.removeAll(otherConstraints.getFilters());
        for (FormulaFilter f : filters) {
            c.addFilter(f);
        }
        return c;
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

    public boolean isViolated(MolecularFormula formula) {
        for (FormulaFilter f : filters)
            if (!f.isValid(formula)) return true;
        final boolean[] violation = new boolean[]{false};
        formula.visit(new FormulaVisitor<Object>() {
            @Override
            public Object visit(Element element, int amount) {
                if (violation[0]) return null;
                if (getUpperbound(element) < amount) {
                    violation[0] = true;
                }
                return null;
            }
        });
        return violation[0];
    }
    public boolean isViolated(ChemicalAlphabet formula) {
        for (Element e : formula.getElements()) {
            if (getUpperbound(e) <= 0) return false;
        }
        return true;
    }

    public boolean isSatisfied(MolecularFormula formula) {
        return !isViolated(formula);
    }
    public boolean isSatisfied(ChemicalAlphabet formula) {
        return !isViolated(formula);
    }

    @Override
    public FormulaConstraints clone() {
        return new FormulaConstraints(this);
    }

    @Override
    public <G, D, L> FormulaConstraints readFromParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        final ChemicalAlphabet alphabet = new ChemicalAlphabet(MolecularFormula.parse(document.getStringFromDictionary(dictionary, "alphabet")).elementArray());
        final FormulaConstraints constraints = new FormulaConstraints(alphabet);
        final Iterator<Map.Entry<String, G>> upperbounds = document.iteratorOfDictionary(document.getDictionaryFromDictionary(dictionary, "upperbounds"));
        final PeriodicTable PT = PeriodicTable.getInstance();
        final int[] ub = constraints.getUpperbounds();
        while (upperbounds.hasNext()) {
            final Map.Entry<String, G> entry = upperbounds.next();
            final Element e = PT.getByName(entry.getKey());
            ub[alphabet.getElements().indexOf(e)] = (int)document.getInt(entry.getValue());
        }
        final Iterator<G> filters = document.iteratorOfList(document.getListFromDictionary(dictionary, "filters"));
        while (filters.hasNext()) {
            addFilter((FormulaFilter)helper.unwrap(document, filters.next()));
        }
        return constraints;
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "alphabet", chemicalAlphabet.toString());
        final D upper = document.newDictionary();
        for (int i=0; i < upperbounds.length; ++i) {
            if (upperbounds[i] < Integer.MAX_VALUE) {
                document.addToDictionary(upper, chemicalAlphabet.getElements().get(i).getSymbol(), document.wrap(upperbounds[i]));
            }
        }
        document.addToDictionary(dictionary, "upperbounds", document.wrapDictionary(upper));
        final L filters = document.newList();
        for (FormulaFilter filter : getFilters()) {
            document.addToList(filters, helper.wrap(document, filter));
        }
        document.addToDictionary(dictionary, "filters", document.wrapList(filters));
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        for (Element e : getChemicalAlphabet()) {
            final int up = getUpperbound(e);
            if (up>0) {
                buf.append(e.getSymbol());
                if (up < Short.MAX_VALUE) {
                    buf.append('[');
                    buf.append(String.valueOf(up));
                    buf.append(']');
                }
            }
        }
        return buf.toString();
    }
}
