
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

import de.unijena.bioinf.ChemistryBase.chem.utils.FormulaVisitor;
import de.unijena.bioinf.ChemistryBase.chem.utils.ValenceFilter;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import gnu.trove.list.array.TIntArrayList;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * FormulaConstraints contain all constraints which reduce the size of all possible decompositions of a mass.
 * It consists of:
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
 *
 * @author kaidu
 */
public class FormulaConstraints implements Ms2ExperimentAnnotation {

    private final ChemicalAlphabet chemicalAlphabet;
    private final int[] upperbounds, lowerbounds;
    private final List<FormulaFilter> filters;

    private final static Pattern INTERVAL = Pattern.compile("\\[(?:(\\d*)\\s*-\\s*)?(\\d*)?\\]");

    public static FormulaConstraints fromString(String alphabet) {
        return new FormulaConstraints(alphabet);
    }

    public FormulaConstraints(String string) {
        string = string.replace(",","");
        final PeriodicTable PT = PeriodicTable.getInstance();
        final Pattern pattern = PT.getPattern();
        final Matcher matcher = pattern.matcher(string);
        if (!matcher.find()) throw new IllegalArgumentException("Invalid alphabet: " + string);
        final List<Object> bounds = new ArrayList<>();
        final Set<Element> elements = new HashSet<>(10);
        final Integer MAX_VALUE = Integer.MAX_VALUE;
        while(true) {
            final String m = matcher.group(0);
            if (m.charAt(0)=='(' || m.charAt(0) == ')') throw new IllegalArgumentException("Invalid alphabet: " + string);
            final Element element = PT.getByName(m);
            if (element == null) throw new IllegalArgumentException("Unknown character: " + m);
            final int start = matcher.end();
            final boolean next = matcher.find();
            final int end = next ? matcher.start() : string.length();
            if (end-start > 0) {
                final Matcher n = INTERVAL.matcher(string.substring(start, end));
                if (n.find()) {
                    final String sa = n.group(1);
                    final String sb = n.group(2);
                    final int a, b;
                    if (sa != null && sa.length()>0) {
                        a = Integer.parseInt(sa);
                    } else {
                        a = 0;
                    }
                    if (sb != null && sb.length()>0) {
                        b = Integer.parseInt(sb);
                    } else {
                        b = Short.MAX_VALUE;
                    }
                    if (b < a) {
                        throw new IllegalArgumentException("Maximum number of allowed element is smaller than minimum number: '" + m + "'");
                    }
                    if (b > 0) {
                        elements.add(element);
                        bounds.add(element);
                        bounds.add(a);
                        bounds.add(b);
                    }
                }
            } else {
                bounds.add(element);
                elements.add(element);
                bounds.add(0);
                bounds.add(MAX_VALUE);
            }
            if (!next) break;
        }
        this.chemicalAlphabet = new ChemicalAlphabet(elements.toArray(new Element[elements.size()]));
        this.upperbounds = new int[chemicalAlphabet.size()];
        this.lowerbounds = upperbounds.clone();
        for (int k=0; k < bounds.size(); k += 3) {
            final Element e = (Element)bounds.get(k);
            final Integer min = (Integer)bounds.get(k+1);
            final Integer max = (Integer)bounds.get(k+2);
            final int i = chemicalAlphabet.indexOf(e);
            lowerbounds[i] = min;
            upperbounds[i] = max;
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
        int stringsOrNumbers = 0;
        int indexOfOnlyString = -1;
        for (int i=0; i < varargs.length; ++i) {
            final Object v = varargs[i];
            if (v instanceof String) {
                ++stringsOrNumbers;
                indexOfOnlyString = i;
            } else if (v instanceof FormulaFilter) continue;
            else stringsOrNumbers = Short.MAX_VALUE;
        }
        if (stringsOrNumbers==1) {
            final FormulaConstraints constraints = new FormulaConstraints((String)varargs[indexOfOnlyString]);
            for (Object vararg : varargs) {
                if (vararg instanceof FormulaFilter) constraints.addFilter((FormulaFilter)vararg);
            }
            return constraints;
        }
        final PeriodicTable t = PeriodicTable.getInstance();
        int i=0;
        TableSelection sel = null;
        final ArrayList<Element> elements = new ArrayList<Element>();
        final TIntArrayList bounds = new TIntArrayList();
        int bound = 2;
        for (; i < varargs.length; ++i) {
            final Object o = varargs[i];

            if (o instanceof Element || o instanceof String) {
                if (bound==0) {
                    bounds.add(0);
                    bounds.add(Integer.MAX_VALUE);
                    bound = 0;
                } else if (bound==1) {
                    bounds.add(bounds.get(bounds.size()-1));
                    bounds.set(bounds.size()-2, 0);
                    bound = 0;
                } else bound = 0;
            }

            if (o instanceof Element) {
                elements.add((Element)varargs[i]);
            } else if (o instanceof String) {
                elements.add(t.getByName((String)varargs[i]));
            } else if (o instanceof Integer) {
                if (bound >= 2)
                    throw new IllegalArgumentException("Only an interval of two numbers is allowed");
                bounds.add((Integer)varargs[i]);
                ++bound;
            } else if (o instanceof TableSelection) {
                if (sel != null) throw new IllegalArgumentException("Multiple table selections given for one formula constraints");
                sel = (TableSelection)o;
            } else if (o instanceof FormulaFilter) {
                filters.add((FormulaFilter)o);
            } else {
                throw new IllegalArgumentException("Expect String,Element,Integer,FormulaFilter or TableSelection, but " + i + "th parameter is of type " + o.getClass());
            }
        }
        if (bound==0) {
            bounds.add(0);
            bounds.add(Integer.MAX_VALUE);
        } else if (bound==1) {
            bounds.add(bounds.get(bounds.size()-1));
            bounds.set(bounds.size()-2, 0);
        }
        final ChemicalAlphabet alphabet = new ChemicalAlphabet(elements.toArray(new Element[elements.size()]));
        final FormulaConstraints c= new FormulaConstraints(alphabet, filters);
        for (int j=0; j < bounds.size(); j += 2) {
            final int elem = alphabet.getElements().indexOf(elements.get(j/2));
            c.lowerbounds[elem] = bounds.get(j);
            c.upperbounds[elem] = bounds.get(j+1);
        }
        return c;
    }

    public FormulaConstraints(FormulaConstraints c) {
        this(c.getChemicalAlphabet());
        System.arraycopy(c.upperbounds, 0, upperbounds, 0, c.upperbounds.length);
        System.arraycopy(c.lowerbounds, 0, lowerbounds, 0, c.lowerbounds.length);
        filters.addAll(c.getFilters());
    }

    public static FormulaConstraints allSubsetsOf(MolecularFormula f) {
        final FormulaConstraints c = new FormulaConstraints(new ChemicalAlphabet(f.elementArray()));
        f.visit((element, amount) -> {
            c.setUpperbound(element, amount); return null;
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

    private static final FormulaConstraints EMPTY = new FormulaConstraints(ChemicalAlphabet.empty());
    public static FormulaConstraints empty() {
        return EMPTY;
    }

    public FormulaConstraints(ChemicalAlphabet alphabet, List<FormulaFilter> filters) {
        this.chemicalAlphabet = alphabet;
        this.upperbounds = new int[alphabet.size()];
        this.lowerbounds = upperbounds.clone();
        Arrays.fill(upperbounds, Integer.MAX_VALUE);
        this.filters = filters == null ? new ArrayList<FormulaFilter>(Arrays.asList(new ValenceFilter())) : new ArrayList<FormulaFilter>(filters);
    }

    public ChemicalAlphabet getChemicalAlphabet() {
        return chemicalAlphabet;
    }

    public int[] getLowerbounds() {
        return lowerbounds;
    }

    public int getLowerbound(Element e) {
        final int i = chemicalAlphabet.indexOf(e);
        if (i < 0) return 0;
        return lowerbounds[i];
    }

    public void setBound(Element e, int lowerbound, int upperbound) {
        final int i = chemicalAlphabet.indexOf(e);
        if ((i < 0) && (upperbound > 0)) {
            throw new NoSuchElementException(e + " is not contained in the chemical alphabet " + chemicalAlphabet);
        }
        if (lowerbound > upperbound)
            throw new IllegalArgumentException("Lowerbound is larger than upperbound: " + e.getSymbol() + "[" + lowerbound + " - " + upperbound);
        lowerbounds[i] = lowerbound;
        upperbounds[i] = upperbound;
    }

    public void setLowerbound(Element e, int lowerbound) {
        final int i = chemicalAlphabet.indexOf(e);
        if (i < 0 && lowerbound > 0) {
            throw new NoSuchElementException(e + " is not contained in the chemical alphabet " + chemicalAlphabet);
        }
        if (lowerbound > upperbounds[i])
            throw new IllegalArgumentException("Lowerbound is larger than upperbound: " + e.getSymbol() + "[" + lowerbound + " - " + upperbounds[i]);
        lowerbounds[i] = lowerbound;
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
        if (i < 0) {
            if (upperbound > 0)
                throw new NoSuchElementException(e + " is not contained in the chemical alphabet " + chemicalAlphabet);
            else
                return;
        }
        if (lowerbounds[i] > upperbound)
            throw new IllegalArgumentException("Upperbound is larger than lowerbound: " + e.getSymbol() + "[" + lowerbounds[i] + " - " + upperbound);
        upperbounds[i] = upperbound;
    }

    public boolean hasElement(Element e) {
        final int i = chemicalAlphabet.indexOf(e);
        return i>=0 && upperbounds[i]>0;
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
            c.setLowerbound(e, getLowerbound(e));
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
            if (hasElement(e)) {
                if (otherConstraints.hasElement(e)) {
                    c.setLowerbound(e, Math.min(otherConstraints.getLowerbound(e), getLowerbound(e)));
                } else c.setLowerbound(e, getLowerbound(e));
            } else {
                c.setLowerbound(e,otherConstraints.getLowerbound(e));
            }
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
        return Arrays.equals(upperbounds, that.upperbounds);
    }

    @Override
    public int hashCode() {
        int result = chemicalAlphabet != null ? chemicalAlphabet.hashCode() : 0;
        result = 31 * result + (upperbounds != null ? Arrays.hashCode(upperbounds) : 0);
        result = 31 * result + (filters != null ? filters.hashCode() : 0);
        return result;
    }

    public boolean isViolated(MolecularFormula formula, Ionization ionization) {
        for (FormulaFilter f : filters)
            if (!f.isValid(formula, ionization)) return true;
        /*
        formula.visit(new FormulaVisitor<Object>() {
            @Override
            public Object visit(Element element, int amount) {
                if (violation[0]) return null;
                final int i = chemicalAlphabet.indexOf(element);
                if (i < 0 || amount < lowerbounds[i] || amount > upperbounds[i]) {
                    violation[0] = true;
                }
                return null;
            }
        });
        */
        return isViolatesBounds(formula);
    }

    private boolean isViolatesBounds(MolecularFormula formula) {
        int atomNumber = 0;
        for (int i=0; i < lowerbounds.length; ++i) {
            final Element e = chemicalAlphabet.get(i);
            final int amount = formula.numberOf(e);
            if (amount < lowerbounds[i] || amount > upperbounds[i]) return true;
            atomNumber += amount;
        }
        return (atomNumber != formula.atomCount());
    }

    public boolean isViolated(ChemicalAlphabet formula) {
        for (Element e : formula.getElements()) {
            if (getUpperbound(e) <= 0) return true;
        }
        return false;
    }

    public boolean isViolated(MolecularFormula formula) {
        for (FormulaFilter f : filters)
            if (!f.isValid(formula)) return true;

        return isViolatesBounds(formula);
    }

    public boolean isSatisfied(MolecularFormula formula, Ionization ionization) {
        return !isViolated(formula, ionization);
    }

    public boolean isSatisfied(ChemicalAlphabet formula) {
        return !isViolated(formula);
    }

    public boolean isSatisfied(MolecularFormula formula) {
        return !isViolated(formula);
    }

    @Override
    public FormulaConstraints clone() {
        return new FormulaConstraints(this);
    }

//    @Override
//    public <G, D, L> FormulaConstraints readFromParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
//        final ChemicalAlphabet alphabet = new ChemicalAlphabet(MolecularFormula.parse(document.getStringFromDictionary(dictionary, "alphabet")).elementArray());
//        final FormulaConstraints constraints = new FormulaConstraints(alphabet);
//        final Iterator<Map.Entry<String, G>> upperbounds = document.iteratorOfDictionary(document.getDictionaryFromDictionary(dictionary, "upperbounds"));
//        final PeriodicTable PT = PeriodicTable.getInstance();
//        final int[] ub = constraints.getUpperbounds();
//        while (upperbounds.hasNext()) {
//            final Map.Entry<String, G> entry = upperbounds.next();
//            final Element e = PT.getByName(entry.getKey());
//            ub[alphabet.getElements().indexOf(e)] = (int)document.getInt(entry.getValue());
//        }
//        final Iterator<G> filters = document.iteratorOfList(document.getListFromDictionary(dictionary, "filters"));
//        while (filters.hasNext()) {
//            addFilter((FormulaFilter)helper.unwrap(document, filters.next()));
//        }
//        return constraints;
//    }
//
//    @Override
//    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
//        document.addToDictionary(dictionary, "alphabet", chemicalAlphabet.toString());
//        final D upper = document.newDictionary();
//        for (int i=0; i < upperbounds.length; ++i) {
//            if (upperbounds[i] < Integer.MAX_VALUE) {
//                document.addToDictionary(upper, chemicalAlphabet.getElements().get(i).getSymbol(), document.wrap(upperbounds[i]));
//            }
//        }
//        document.addToDictionary(dictionary, "upperbounds", document.wrapDictionary(upper));
//        final L filters = document.newList();
//        for (FormulaFilter filter : getFilters()) {
//            document.addToList(filters, helper.wrap(document, filter));
//        }
//        document.addToDictionary(dictionary, "filters", document.wrapList(filters));
//    }

    public String toString() {
        return toString("");
    }

    public String toString(String separator) {
        StringBuilder buf = new StringBuilder();
        for (Element e : getChemicalAlphabet()) {
            final int i = chemicalAlphabet.indexOf(e);
            final int up = upperbounds[i];
            final int low = lowerbounds[i];
            if (up>0) {
                if (buf.length()>0) buf.append(separator);
                buf.append(e.getSymbol());
                if (low > 0 || up < Short.MAX_VALUE) {
                    buf.append('[');
                    if (low > 0) {
                        buf.append(String.valueOf(low));
                        buf.append('-');
                    }
                    if (up < Short.MAX_VALUE) {
                        buf.append(String.valueOf(up));
                    }
                    buf.append(']');
                }
            }
        }
        final String s = buf.toString();
        return s.isEmpty() ? "," : s;
    }

    public FormulaConstraints intersection(FormulaConstraints formulaConstraints) {
        final List<Element> elements = new ArrayList<>(this.chemicalAlphabet.getElements());
        //remove elements if bounds do not intersect
        elements.removeIf(e -> !formulaConstraints.hasElement(e) || (Math.max(getLowerbound(e), formulaConstraints.getLowerbound(e)) > Math.min(getUpperbound(e), formulaConstraints.getUpperbound(e))));
        final ChemicalAlphabet alphabet = new ChemicalAlphabet(elements.toArray(Element[]::new));
        final FormulaConstraints intersection = new FormulaConstraints(alphabet);
        final HashMap<Class<? extends FormulaFilter>, FormulaFilter> ifils = new HashMap<>();
        for (FormulaFilter f : this.filters)
            ifils.put(f.getClass(), f);
        for (FormulaFilter f : formulaConstraints.filters)
            ifils.put(f.getClass(), f);
        intersection.filters.addAll(ifils.values());
        for (Element e : elements) {
            intersection.setBound(e, Math.max(getLowerbound(e), formulaConstraints.getLowerbound(e)), Math.min(getUpperbound(e), formulaConstraints.getUpperbound(e)));
        }
        return intersection;
    }

    /**
     * limits the {@link FormulaConstraints} to an array of elements.
     * @param elements
     * @return
     */
    public FormulaConstraints intersection(Element... elements) {
        final List<Element> newElements = Arrays.stream(elements).collect(Collectors.toList());
        newElements.removeIf(e -> !hasElement(e));
        final ChemicalAlphabet alphabet = new ChemicalAlphabet(newElements.toArray(Element[]::new));
        final FormulaConstraints intersection = new FormulaConstraints(alphabet, filters);
        for (Element e : newElements) {
            intersection.setBound(e, getLowerbound(e), getUpperbound(e));
        }
        return intersection;
    }

    public FormulaConstraints withNewFilters(List<FormulaFilter> formulaFilters){
        final FormulaConstraints fc = this.clone();
        fc.filters.clear();
        fc.filters.addAll(formulaFilters);
        return fc;
    }
}
