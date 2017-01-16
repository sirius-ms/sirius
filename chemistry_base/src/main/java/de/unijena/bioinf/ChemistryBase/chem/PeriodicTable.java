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

import com.google.common.collect.Range;
import de.unijena.bioinf.ChemistryBase.chem.utils.*;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Give access to all chemical elements and ions. This class should be seen as singleton, although it's
 * possible to create multiple PeriodicTables
 * All this information are parsed from a json file in the ChemistryBase library.
 * <p>
 * The PeriodicTable is not thread-safe, because in practice there should be only read-accesses. For write access,
 * you have to do the synchronisation yourself.
 * <p>
 * <pre>
 * PeriodicTable.getInstance().getByName("C").getMass();
 * PeriodicTable.getInstance().parse("C6H12O6", myAtomVisitor)
 * </pre>
 */
public class PeriodicTable implements Iterable<Element>, Cloneable {

    /*
                STATIC
     */
    private static PeriodicTable instance;
    private static ThreadLocal<PeriodicTable> localInstance = new ThreadLocal<PeriodicTable>();
    private static final ArrayList<PeriodicTable> instanceStack = new ArrayList<PeriodicTable>();
    private static final ThreadLocal<ArrayList<PeriodicTable>> localInstanceStack = new ThreadLocal<ArrayList<PeriodicTable>>();
    private static int threadLocal;
    private final static Pattern IONTYPE_PATTERN = Pattern.compile("[\\[\\]()+-]");
    private final static Pattern IONTYPE_NUM_PATTERN = Pattern.compile("^\\d+$");
    private final static Pattern IONTYPE_NUM_PATTERN_LEFT = Pattern.compile("^\\d+");

    /**
     * @return current enabled periodic table instance
     */
    public static PeriodicTable getInstance() {
        if (!isThreadLocal()) return instance;
        return getLocalInstance();
    }

    private static PeriodicTable getLocalInstance() {
        final PeriodicTable pt = localInstance.get();
        if (pt == null) return instance;
        return pt;
    }

    private static PeriodicTable push(PeriodicTable pt) {
        final ArrayList<PeriodicTable> stack;
        final PeriodicTable before;
        final boolean threadLocal = isThreadLocal();
        if (threadLocal) {
            final ArrayList<PeriodicTable> st = localInstanceStack.get();
            if (st == null) {
                stack = new ArrayList<PeriodicTable>();
                localInstanceStack.set(stack);
            } else stack = st;
            final PeriodicTable t = localInstance.get();
            if (t == null) before = instance;
            else before = t;
        } else {
            stack = instanceStack;
            before = instance;
        }
        stack.add(before);
        if (threadLocal) localInstance.set(pt);
        else instance = pt;
        return pt;
    }

    /**
     * Add a new empty periodic table to the stack and enable it. Can be used to change temporarily a periodic table
     *
     * @return the added periodic table
     */
    public static PeriodicTable push() {
        return push(new PeriodicTable());
    }

    /**
     * add a copy of the current periodic table to the stack. Can be used to change temporarily a periodic table
     *
     * @return the added periodic table
     */
    public static PeriodicTable pushCopy() {
        return push(instance.clone());
    }

    /**
     * removes the last periodic table which was added to the stack and enable the previous table. Use together with
     * push or pushCopy to change temporarily a periodic table.
     *
     * @return
     */
    public static PeriodicTable pop() {
        final boolean threadLocal = isThreadLocal();
        final PeriodicTable ret;
        if (threadLocal) {
            ret = localInstance.get();
            final ArrayList<PeriodicTable> stack = localInstanceStack.get();
            if (stack.size() == 0) localInstance.set(null);
            else {
                localInstance.set(stack.get(stack.size() - 1));
                stack.remove(stack.size() - 1);
            }
        } else {
            ret = instance;
            if (instanceStack.size() == 0) throw new RuntimeException("No further periodic table in stack");
            instance = instanceStack.get(instanceStack.size() - 1);
            instanceStack.remove(instanceStack.size() - 1);
        }
        return ret;
    }

    /**
     * returns true if the periodic table may be different in different threads.
     *
     * @return
     */
    public static boolean isThreadLocal() {
        return threadLocal > 0;
    }

    /**
     * Enable thread local periodic tables. If set to true, each change of a periodic table (either by push or by
     * direct modification) affect only the current thread. Be careful with this option, because it may reduce
     * the performance. Especially, it may be dangerous to set this option back to false, after enable it, because
     * the standard access to the periodic table is not synchronized
     *
     * @param value
     */
    public static synchronized void setThreadLocal(boolean value) {
        synchronized (IsotopicDistribution.class) {
            threadLocal += (value ? 1 : -1);
        }
        if (!value) localInstance.set(null);
    }

    static {
        instance = new PeriodicTable();
        try {
            new PeriodicTableBlueObeliskReader().readFromClasspath(instance);
            new PeriodicTableJSONReader().readFromClasspath(instance, "/additional_elements.json");
            //new PeriodicTableJSONReader().readFromClasspath(instance);
            instance.cache.addDefaultAlphabet();
            instance.setDistribution(new IsotopicDistributionBlueObeliskReader().getFromClasspath());
            instance.addDefaultIons();
        } catch (IOException e) {
            LoggerFactory.getLogger(PeriodicTable.class).error(e.getMessage(),e);
        }

    }

    public PrecursorIonType getPrecursorIonTypeForEI() {
        return EI_TYPE;
    }

    private IonMode[] POSITIVE_ION_MODES, NEGATIVE_ION_MODES;
    private Charge POSITIVE_IONIZATION, NEGATIVE_IONIZATION;
    private IonMode PROTONATION, DEPROTONATION, INTRINSICALLY_CHARGED_POSITIVE, INTRINSICALLY_CHARGED_NEGATIVE;
    private Ionization ELECTRON_IONIZATION;
    private PrecursorIonType EI_TYPE, UNKNOWN_POSITIVE_IONTYPE, UNKNOWN_NEGATIVE_IONTYPE;

    public Iterable<Ionization> getKnownIonModes(int charge) {
        if (Math.abs(charge)!=1) throw new IllegalArgumentException("Do not support multiple charges yet");
        if (charge>0) {
            return Arrays.asList((Ionization[])POSITIVE_ION_MODES);
        } else {
            return Arrays.asList((Ionization[])NEGATIVE_ION_MODES);
        }
    }

    /**
     * returns a list of PrecursorIonType instances with the given charge
     * ion types at the beginning of the list are more common/likely than ion types
     * at the end of the list
     * @param charge
     * @return
     */
    public Iterable<PrecursorIonType> getKnownLikelyPrecursorIonizations(int charge) {
        if (Math.abs(charge)!=1) throw new IllegalArgumentException("Do not support multiple charges yet");
        final HashSet<PrecursorIonType> ions = new HashSet<PrecursorIonType>(knownIonTypes.values());
        final ArrayList<PrecursorIonType> likely = new ArrayList<PrecursorIonType>();

        if (charge > 0) {
            likely.add(ionByName("[M+H]+"));
            likely.add(ionByName("[M]+"));
            likely.add(ionByName("[M+H-H2O]+"));
            likely.add(ionByName("[M+Na]+"));
            for (PrecursorIonType i : likely) ions.remove(i);
            likely.addAll(ions);
        } else {
            likely.add(ionByName("[M-H]-"));
            likely.add(ionByName("[M]-"));
            for (PrecursorIonType i : likely) ions.remove(i);
            likely.addAll(ions);
        }
        return likely;
    }

    private static String canonicalizeIonName(String ionName) {
        return ionName.replaceAll("\\s+", "");
    }

    private void addDefaultIons() {
        // ION MODES
        PROTONATION = new IonMode(1, "[M+H]+", MolecularFormula.parse("H"));
        DEPROTONATION = new IonMode(-1, "[M-H]-", MolecularFormula.parse("H").negate());
        this.UNKNOWN_NEGATIVE_IONTYPE = new PrecursorIonType(new Charge(-1), MolecularFormula.emptyFormula(), MolecularFormula.emptyFormula(), true);
        this.UNKNOWN_POSITIVE_IONTYPE = new PrecursorIonType(new Charge(1), MolecularFormula.emptyFormula(), MolecularFormula.emptyFormula(), true);
        this.POSITIVE_ION_MODES = new IonMode[]{
                new IonMode(1, "[M+K]+", MolecularFormula.parse("K")),
                new IonMode(1, "[M+Na]+", MolecularFormula.parse("Na")),
                PROTONATION
        };
        this.NEGATIVE_ION_MODES = new IonMode[]{
                new IonMode(-1, "[M+Cl]-", MolecularFormula.parse("Cl")),
                DEPROTONATION
        };
        this.POSITIVE_IONIZATION = new Charge(1);
        this.NEGATIVE_IONIZATION = new Charge(-1);
        this.INTRINSICALLY_CHARGED_NEGATIVE = new IonMode(-1, "[M]-", MolecularFormula.emptyFormula());
        this.INTRINSICALLY_CHARGED_POSITIVE = new IonMode(1, "[M]+", MolecularFormula.emptyFormula());
        this.ELECTRON_IONIZATION = new ElectronIonization();
        this.EI_TYPE = new PrecursorIonType(ELECTRON_IONIZATION, MolecularFormula.emptyFormula(), MolecularFormula.emptyFormula(), false);
        // ADDUCTS
        final String[] adductsPositive = new String[]{
                "[M+H]+", "[M]+", "[M+K]+", "[M+Na]+",
                "[M+H-H2O]+", "[M+Na2-H]+", "[M+2K-H]+", "[M+NH4]+", "[M + H3O]+",
                "[M + MeOH + H]+", // methanol
                "[M + ACN + H]+", // Acetonitrile CH3CN
                "[M + 2ACN + H]+",
                "[M+IPA+H]+",
                "[M + ACN + Na]+",
                "[M + DMSO + H]+"
        };
        final String[] adductsNegative = new String[]{
                "[M-H]-",
                "[M]-",
                "[M+K-2H]-",
                "[M+Cl]-",
                "[M - H2O - H]-",
                "[M+Na-2H]-",
                "[M+FA-H]-",
                "[M+Br]-",
                "[M+HAc-H]-",
                "[M+TFA-H]-",
                "[M+ACN-H]-"
        };
        final HashMap<String, PrecursorIonType> positiveIonTypes = new HashMap<String, PrecursorIonType>();
        for (String pos : adductsPositive) {
            String posName = canonicalizeIonName(pos);
            positiveIonTypes.put(posName, parseIonType(pos));


            //System.out.println(pos + "\t=>\t" + positiveIonTypes.get(pos)+ "\t=\t" + positiveIonTypes.get(pos).getIonization().toString() + " ionization with " + positiveIonTypes.get(pos).getAdduct().toString() + " adduct");

            assert positiveIonTypes.get(posName).getIonization().getCharge() > 0;
        }
        final HashMap<String, PrecursorIonType> negativeIonTypes = new HashMap<String, PrecursorIonType>();
        for (String neg : adductsNegative) {
            final String negName = canonicalizeIonName(neg);
            negativeIonTypes.put(negName, parseIonType(neg));
            assert negativeIonTypes.get(negName).getIonization().getCharge() < 0;
        }
        knownIonTypes.putAll(positiveIonTypes);
        knownIonTypes.putAll(negativeIonTypes);

        // add common misspelled aliases...
        final PrecursorIonType hplus = knownIonTypes.get("[M+H]+");
        final PrecursorIonType hminus = knownIonTypes.get("[M-H]-");
        knownIonTypes.put("M+H", hplus);
        knownIonTypes.put("M+H+", hplus);
        knownIonTypes.put("[M+H]", hplus);
        knownIonTypes.put("M-H", hminus);
        knownIonTypes.put("M-H-", hminus);
        knownIonTypes.put("[M-H]", hminus);
        knownIonTypes.put("M+", knownIonTypes.get("[M]+"));
        knownIonTypes.put("M-", knownIonTypes.get("[M]-"));
    }

    protected Pattern MULTIMERE_PATTERN = Pattern.compile("\\d+M([+-]|\\])");

    private PrecursorIonType parseIonType(String name) {
        if (MULTIMERE_PATTERN.matcher(name).find()) throw new IllegalArgumentException("Do not support multiplier before a molecular formula: '" + name + "'");
        // tokenize String
        final String ACN =  "CH3CN"; // ACN
        final String FA = "H2CO2"; // FA
        final String MeOH = "CH4O"; // MeOH, methanol
        final String IPA = "C3H8O"; // IPA, Isopropanol, IsoProp
        final String DMSO = "C2H6OS"; // Dimethylsulfoxid
        final String HAC = "C2H4O2"; // acetic acid
        final String TFA = "CF3CO2H"; // TFA, Trifluoroacetic acid
        final HashMap<String, String> replacement = new HashMap<>();
        replacement.put("ACN", ACN);
        replacement.put("FA", FA);
        replacement.put("MEOH", MeOH);
        replacement.put("IPA", IPA);
        replacement.put("DMSO", DMSO);
        replacement.put("HAC", HAC);
        replacement.put("TFA", TFA);
        final ArrayList<String> tokens = new ArrayList<String>();
        final Matcher m = IONTYPE_PATTERN.matcher(name);
        int lastPos = 0;
        while (m.find()) {
            if (m.start() > lastPos) tokens.add(name.substring(lastPos, m.start()));
            tokens.add(m.group());
            lastPos = m.end();
        }
        if (lastPos < name.length()) tokens.add(name.substring(lastPos, name.length()));

        int state = 0;
        final ArrayList<MolecularFormula> adducts = new ArrayList<MolecularFormula>();
        final ArrayList<MolecularFormula> insourceFrags = new ArrayList<MolecularFormula>();

        boolean isAdd = true;
        int number = 1;

        for (int k = 0; k < tokens.size(); ++k) {
            final String token = tokens.get(k).trim();
            final char c = token.charAt(0);
            switch (c) {
                case '(':
                    if (number != 1) {
                        throw new IllegalArgumentException("Do not support multiplier before a molecular formula: '" + name + "'");
                    } else break;
                case ')':
                    break;
                case '[':
                    break; // ignore
                case ']':
                    break; // ignore
                case '+':
                    if (number != 1) {
                        throw new IllegalArgumentException("Do not support multiple charges: '" + name + "'");
                    } else {
                        isAdd = true;
                        break;
                    }
                case '-':
                    if (number != 1) {
                        throw new IllegalArgumentException("Do not support multiple charges: '" + name + "'");
                    } else {
                        isAdd = false;
                        break;
                    }
                case 'M':
                    if (token.length() <= 1 || !(Character.isDigit(token.charAt(1)) || Character.isAlphabetic(token.charAt(1)))) {
                        if (number != 1) {
                            throw new IllegalArgumentException("Do not support multimeres: '" + name + "'");
                        } else if (!isAdd) {
                            throw new IllegalArgumentException("Invalid format of ion type: '" + name + "'");
                        } else break;
                    }
                default: {
                    if (IONTYPE_NUM_PATTERN.matcher(token).find()) {
                        // is a number
                        number = Integer.parseInt(token);
                    } else {
                        final String formulaString;
                        final Matcher numm = IONTYPE_NUM_PATTERN_LEFT.matcher(token);
                        if (numm.find()) {
                            if (number != 1) {
                                throw new IllegalArgumentException("Do not support nested groups in formula string: '" + name + "'");
                            }
                            number = Integer.parseInt(numm.group());
                            formulaString = token.substring(numm.group().length());
                        } else {
                            formulaString = token;
                        }
                        // should be a molecular formula
                        MolecularFormula f;
                        if (replacement.containsKey(formulaString.toUpperCase())) {
                            f = MolecularFormula.parse(replacement.get(formulaString.toUpperCase()));
                        } else {
                            f = MolecularFormula.parse(formulaString);
                        }
                        if (number != 1) {
                            f = f.multiply(number);
                        }
                        if (isAdd) {
                            adducts.add(f);
                        } else {
                            insourceFrags.add(f);
                        }
                        isAdd = true;
                        number = 1;
                    }
                }
            }
        }
        final int charge = (isAdd ? 1 : -1);

        // find ionization mode
        Ionization usedIonMode = null;
        final IonMode[] ionModes = (charge > 0) ? POSITIVE_ION_MODES : NEGATIVE_ION_MODES;
        for (IonMode ion : ionModes) {
            if (ion.getAtoms().atomCount() > 0) {
                // search for adduct containing this ion
                int found = -1;
                for (int i = 0; i < adducts.size(); ++i) {
                    if (ion.getAtoms().equals(adducts.get(i))) {
                        found = i;
                        break;
                    }
                }
                if (found >= 0) {
                    adducts.remove(found);
                    usedIonMode = ion;
                } else {
                    for (int i = 0; i < adducts.size(); ++i) {
                        if (adducts.get(i).isSubtractable(ion.getAtoms())) {
                            found = i;
                            break;
                        }
                    }
                    if (found >= 0) {
                        usedIonMode = ion;
                        adducts.set(found, adducts.get(found).subtract(ion.getAtoms()));
                    }
                }
            } else if (ion.getAtoms().atomCount() < 0) {
                // search for loss containing this ion
                int found = -1;
                MolecularFormula neg = ion.getAtoms().negate();
                for (int i = 0; i < insourceFrags.size(); ++i) {
                    if (neg.equals(insourceFrags.get(i))) {
                        found = i;
                        break;
                    }
                }
                if (found >= 0) {
                    insourceFrags.remove(found);
                    usedIonMode = ion;
                } else {
                    for (int i = 0; i < insourceFrags.size(); ++i) {
                        if (insourceFrags.get(i).isSubtractable(neg)) {
                            found = i;
                            break;
                        }
                    }
                    if (found >= 0) {
                        usedIonMode = ion;
                        insourceFrags.set(found, insourceFrags.get(found).subtract(neg));
                    }
                }
            }
            if (usedIonMode != null) break;
        }
        if (usedIonMode == null) {
            usedIonMode = charge > 0 ? INTRINSICALLY_CHARGED_POSITIVE : INTRINSICALLY_CHARGED_NEGATIVE;
        }
        MolecularFormula adduct = MolecularFormula.emptyFormula();
        for (MolecularFormula f : adducts) adduct = adduct.add(f);
        MolecularFormula insource = MolecularFormula.emptyFormula();
        for (MolecularFormula f : insourceFrags) insource = insource.add(f);
        return new PrecursorIonType(usedIonMode, insource, adduct, false);
    }


    public IonMode getProtonation() {
        return PROTONATION;
    }

    public IonMode getDeprotonation() {
        return DEPROTONATION;
    }

    public PrecursorIonType getPrecursorIonTypeFromIonization(Ionization ion) {
        if (ion instanceof Charge) {
            if (ion.getCharge() == 1) return UNKNOWN_POSITIVE_IONTYPE;
            else if (ion.getCharge() == -1) return UNKNOWN_NEGATIVE_IONTYPE;
            else throw new IllegalArgumentException("Multiple charges are not supported yet");
        }
        for (PrecursorIonType i : knownIonTypes.values()) {
            if (i.getIonization().equals(ion) && i.getAdduct().atomCount() == 0 && i.getInSourceFragmentation().atomCount() == 0)
                return i;
        }
        return new PrecursorIonType(ion, MolecularFormula.emptyFormula(), MolecularFormula.emptyFormula(), false);
    }

    public PrecursorIonType getUnknownPrecursorIonType(int charge) {
        if (charge != -1 && charge != 1) throw new IllegalArgumentException("Multiple charges are not allowed!");
        if (charge > 0) return UNKNOWN_POSITIVE_IONTYPE;
        else return UNKNOWN_NEGATIVE_IONTYPE;
    }

    private final static class ElementStack {
        private Element element;
        private short amount;
        private ElementStack neighbour;

        private void set(Element element, int amount) {
            this.element = element;
            if (amount > Short.MAX_VALUE || amount < Short.MIN_VALUE) {
                throw new RuntimeException("Element number exceeds formula space: " + amount);
            }
            this.amount = (short) amount;
        }
    }


    /**
     * Regular Expression for molecular formulas regarding all chemical elements in PeriodicTable
     */
    private Pattern pattern;
    private final HashMap<String, Element> nameMap;
    private final ArrayList<Element> elements;
    private IsotopicDistribution distribution;
    private MolecularFormula emptyFormula;
    private final HashMap<String, PrecursorIonType> knownIonTypes;
    /**
     * Cache which is used to build molecular formulas with shared structure
     */
    final TableSelectionCache cache;

    PeriodicTable() {
        this.elements = new ArrayList<Element>();
        this.nameMap = new HashMap<String, Element>();
        this.cache = new TableSelectionCache(this, TableSelectionCache.DEFAULT_MAX_COMPOMERE_SIZE);
        this.knownIonTypes = new HashMap<String, PrecursorIonType>();
        this.distribution = new IsotopicDistribution(this);
        this.emptyFormula = null;
    }

    PeriodicTable(PeriodicTable pt) {
        this.elements = new ArrayList<Element>(pt.elements);
        this.nameMap = new HashMap<String, Element>(pt.nameMap);
        this.pattern = pt.pattern;
        this.knownIonTypes = new HashMap<String, PrecursorIonType>();
        // new cache =(
        this.cache = new TableSelectionCache(this, TableSelectionCache.DEFAULT_MAX_COMPOMERE_SIZE);
        this.emptyFormula = null;
        this.distribution = new IsotopicDistribution(this);
        distribution.merge(pt.distribution);
    }

    /**
     * @return the empty formula. This object is cached as molecular formulas are immutable
     */
    public MolecularFormula emptyFormula() {
        if (emptyFormula == null)
            emptyFormula = MolecularFormula.fromCompomer(cache.getSelectionFor(new BitSet()), new short[0]);
        return emptyFormula;
    }

    public void addElement(String name, String symbol, double mass, int valence) {
        if (nameMap.containsKey(symbol))
            throw new IllegalArgumentException("There is already an element with name '" + symbol + "'");
        elements.add(new Element(elements.size(), name, symbol, mass, valence));
        nameMap.put(symbol, elements.get(elements.size() - 1));
        pattern = null;
    }

    /**
     * Adds a new ion type to the list of known/common ion types
     *
     * @param name    name of the ion type
     * @param ionType ion type that should be added
     * @return true if ion type is added, false if the ion type was already in the set
     * @throws IllegalArgumentException if the name is already used for a different ion type
     */
    public boolean addCommonIonType(String name, PrecursorIonType ionType) {
        if (knownIonTypes.containsKey(name)) {
            if (ionType.equals(knownIonTypes.get(ionType))) return false;
            else throw new IllegalArgumentException("There is already an ionization with name '" + name + "'");
        }
        knownIonTypes.put(name, ionType);
        return true;
    }

    /**
     * @see PeriodicTable#addCommonIonType(String, PrecursorIonType)
     * uses the normalized name of the ion type
     */
    public boolean addCommonIonType(PrecursorIonType ionType) {
        return addCommonIonType(ionType.toString(), ionType);
    }

    /**
     * @return the regular expression pattern that is used to parse molecular formulas
     */
    public Pattern getPattern() {
        if (pattern == null) refreshRegularExpression();
        return pattern;
    }

    @Override
    protected PeriodicTable clone() {
        return new PeriodicTable(this);
    }

    public IsotopicDistribution getDistribution() {
        return distribution;
    }

    public void setDistribution(IsotopicDistribution distribution) {
        this.distribution = distribution;
        for (Element e : elements) {
            final Isotopes d = distribution.getIsotopesFor(e);
            if (d == null) continue;
            double minMass = Double.MAX_VALUE;
            for (int i = 0; i < d.getNumberOfIsotopes(); ++i) {
                if (d.getAbundance(i) > 0 && d.getMass(i) < minMass) {
                    minMass = d.getMass(i);
                }
            }
            e.mass = minMass;
            e.nominalMass = (int) (Math.round(minMass));
        }
    }

    private void refreshRegularExpression() {
        if (elements.isEmpty()) {
            pattern = Pattern.compile("");
            return;
        }
        final StringBuilder buffer = new StringBuilder();
        final Element[] orderedElements = elements.toArray(new Element[elements.size()]);
        Arrays.sort(orderedElements, new Comparator<Element>() {
            @Override
            public int compare(Element o1, Element o2) {
                return o2.getSymbol().length() - o1.getSymbol().length();
            }
        });
        final Iterator<Element> elementIterator = Arrays.asList(orderedElements).iterator();
        buffer.append("(\\)|");
        buffer.append(elementIterator.next().getSymbol());
        while (elementIterator.hasNext()) {
            buffer.append("|").append(elementIterator.next());
        }
        buffer.append(")(\\d*)|\\(");
        this.pattern = Pattern.compile(buffer.toString());
    }


    /**
     * build a bitset of an array of elements. The i-th bit is set if the i-th element is contained
     * in the array
     */
    BitSet bitsetOfElements(Element... elements) {
        final int maxId = Collections.max(Arrays.asList(elements), new Comparator<Element>() {
            @Override
            public int compare(Element o1, Element o2) {
                return o1.getId() - o2.getId();
            }
        }).getId();
        final BitSet bitset = new BitSet(maxId + 1);
        for (Element e : elements) {
            bitset.set(e.getId());
        }
        return bitset;
    }

    /**
     * @return an immutable list of ions
     */
    public Collection<PrecursorIonType> getIons() {
        return knownIonTypes.values();
    }

    /**
     * return the element with the given Id
     *
     * @param id
     * @return an Element or null if there is no element with this id
     */
    public Element get(int id) {
        return elements.get(id);
    }

    /**
     * returns an array with elements corresponding to the given names. The i-th element in the
     * array has the i-th name of the given names-array. If a name appears multiple times in the
     * names array, so it does in the returned array. If a name is not found, the position in the
     * array is filled with null.
     *
     * @param names
     * @return array with Element instances or nulls.
     */
    public Element[] getAllByName(String... names) {
        final Element[] elements = new Element[names.length];
        for (int i = 0; i < names.length; ++i) {
            elements[i] = getByName(names[i]);
        }
        return elements;
    }

    /**
     * return the element with the given symbol.
     *
     * @param name
     * @return an Element or null if there is no element with this symbol
     */
    public Element getByName(String name) {
        return nameMap.get(name);
    }

    /**
     * Search for a TableSelection which contains at least all the given elements. Use this, if you
     * want to control onto which TableSelection your molecules are built.
     *
     * @param elements
     * @return
     */
    public TableSelection getSelectionFor(Element... elements) {
        return getSelectionFor(bitsetOfElements(elements));
    }

    /**
     * Search for a TableSelection which contains at least all the given elements. Use this, if you
     * want to control onto which TableSelection your molecules are built.
     *
     * @return
     */
    public TableSelection getSelectionFor(BitSet bitset) {
        return cache.getSelectionFor(bitset);
    }

    public PrecursorIonType ionByMass(double mass, double absError) {
        return ionByMass(mass, absError, 0);
    }

    /**
     * search for a known ion which mass is corresponding to the given mass while considering the
     * given mass error. If there multiple ions in the mass window, the method returns the ion with
     * the lowest mass error.
     *
     * @param mass
     * @param absError
     * @return an ion with the given mass or null if no ion is found
     */
    public PrecursorIonType ionByMass(double mass, double absError, int charge) {
        PrecursorIonType minIon = null;
        double minDistance = Double.MAX_VALUE;
        for (PrecursorIonType iontype : knownIonTypes.values()) {
            final Ionization ion = iontype.getIonization();
            if (charge != 0 && ion.getCharge() != charge) continue;
            final double abw = Math.abs(iontype.getModificationMass() - mass);
            if (abw < minDistance) {
                minDistance = abw;
                minIon = iontype;
            }
        }
        if (minDistance < absError) return minIon;
        else return null;
    }

    /**
     * search for an ion with the given name. Usually, the names are in the format '[M'[+-]X']'[+-] where
     * X is a molecular formula, for example [M+H2O]+.
     * <p>
     * [M+H]+
     */
    public PrecursorIonType ionByName(String name) {
        name = canonicalizeIonName(name);
        if (name.equals("[M+?]+") || name.equals("M+?+")) return PrecursorIonType.unknown(1);
        if (name.equals("[M+?]-") || name.equals("M+?-")) return PrecursorIonType.unknown(-1);
        if (knownIonTypes.containsKey(name)) return knownIonTypes.get(name);
        else return parseIonType(name);
    }

    /**
     * Calculate for a given alphabet the maximal and minimal mass defects of isotopes.
     *
     * @param alphabet   chemical alphabet
     * @param deviation  allowed mass deviation
     * @param monomz     m/z of monoisotopic peak
     * @param peakOffset integer distance between isotope peak and monoisotopic peak (minimum: 1)
     * @return an interval which should contain the isotopic peak
     */
    public Range<Double> getIsotopicMassWindow(ChemicalAlphabet alphabet, Deviation deviation, double monomz, int peakOffset) {
        if (peakOffset < 1) throw new IllegalArgumentException("Expect a peak offset of at least 1");
        final IsotopicDistribution dist = getDistribution();
        double minmz = Double.POSITIVE_INFINITY;
        double maxmz = Double.NEGATIVE_INFINITY;
        for (Element e : alphabet) {
            final Isotopes iso = dist.getIsotopesFor(e);
            for (int k = 1; k < iso.getNumberOfIsotopes(); ++k) {
                final int i = iso.getIntegerMass(k) - e.getIntegerMass();
                if (i > peakOffset) break;
                double diff = iso.getMassDifference(k) - i;
                diff *= (peakOffset / i);
                minmz = Math.min(minmz, diff);
                maxmz = Math.max(maxmz, diff);
            }
        }
        final double a = monomz + peakOffset + minmz;
        final double b = monomz + peakOffset + maxmz;
        return Range.closed(a - deviation.absoluteFor(a), b + deviation.absoluteFor(b));
    }

    /**
     * returns an iterator which yields each element in the table in order of their ids.
     */
    @Override
    public Iterator<Element> iterator() {
        return Collections.unmodifiableList(elements).iterator();
    }

    /**
     * @return number of elements in the table
     */
    public int numberOfElements() {
        return elements.size();
    }

    /**
     * parses a formula and invoke {@link FormulaVisitor#visit(Element, int)} for each atom.
     * Remark that there is no guarantee that for each atom type this method is called only one time.
     * Use this, if you want to build your own molecular formula type without using MolecularFormula.
     * Another advantage of this function is the independence from TableSelection.
     *
     * @param formula
     * @param visitor
     */
    public void parse(String formula, FormulaVisitor<?> visitor) {
        if (formula.indexOf('(') < 0) {
            parseUnstackedFormula(formula, visitor);
        } else {
            parseStackedFormula(formula, visitor);
        }
    }

    private void parseStackedFormula(String formula, FormulaVisitor<?> visitor) {
        final Matcher matcher = pattern.matcher(formula);
        final ArrayDeque<ElementStack> stack = new ArrayDeque<ElementStack>();
        while (matcher.find()) {
            switch (matcher.group().charAt(0)) {
                case '(':
                    stack.push(new ElementStack());
                    break;
                case ')':
                    final String multiplyStr = matcher.group(2);
                    final int multiply = (multiplyStr != null && multiplyStr.length() > 0) ?
                            Integer.parseInt(multiplyStr) : 1;
                    ElementStack stackItem = stack.pop();
                    ElementStack prev = stack.isEmpty() ? null : stack.pop();
                    while (stackItem.neighbour != null) {
                        stackItem = stackItem.neighbour;
                        if (prev == null) {
                            visitor.visit(stackItem.element, stackItem.amount * multiply);
                        } else {
                            prev.set(stackItem.element, stackItem.amount * multiply);
                            final ElementStack add = new ElementStack();
                            add.neighbour = prev;
                            prev = add;
                        }
                    }
                    if (prev != null) stack.push(prev);
                    break;
                default:
                    ElementStack last = stack.isEmpty() ? null : stack.pop();
                    final String elementName = matcher.group(1);
                    final String elementAmount = matcher.group(2);
                    final Element element = getByName(elementName);
                    final int amount = elementAmount != null && elementAmount.length() > 0 ?
                            Integer.parseInt(elementAmount) : 1;
                    if (last == null) {
                        visitor.visit(element, amount);
                    } else {
                        last.set(element, amount);
                        final ElementStack add = new ElementStack();
                        add.neighbour = last;
                        last = add;
                        stack.push(last);
                    }
            }
        }
    }

    private void parseUnstackedFormula(String formula, FormulaVisitor<?> visitor) {
        final int multiplier;
        if (formula.isEmpty()) return;
        if (Character.isDigit(formula.charAt(0))) {
            int lastnum = 0;
            while (lastnum < formula.length() && Character.isDigit(formula.charAt(lastnum))) ++lastnum;
            multiplier = Integer.parseInt(formula.substring(0, lastnum));
            formula = formula.substring(lastnum, formula.length());
        } else multiplier=1;
        final Matcher matcher = getPattern().matcher(formula);
        while (matcher.find()) {
            final String elementName = matcher.group(1);
            final String elementAmount = matcher.group(2);
            final Element element = getByName(elementName);
            final int amount = multiplier*(elementAmount != null && elementAmount.length() > 0 ?
                    Integer.parseInt(elementAmount) : 1);
            visitor.visit(element, amount);
        }
    }
}
