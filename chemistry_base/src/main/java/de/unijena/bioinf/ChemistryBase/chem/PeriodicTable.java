
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

import de.unijena.bioinf.ChemistryBase.chem.utils.*;
import de.unijena.bioinf.ChemistryBase.exceptions.MultimereException;
import de.unijena.bioinf.ChemistryBase.exceptions.MultipleChargeException;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.apache.commons.lang3.Range;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Give access to all chemical elements and iondetection. This class should be seen as singleton, although it's
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
public final class PeriodicTable implements Iterable<Element>, Cloneable {


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
            instance.cache.addDefaultAlphabet();
            instance.setDistribution(new IsotopicDistributionBlueObeliskReader().getFromClasspath());
            instance.addDefaultIons();
        } catch (IOException e) {
            LoggerFactory.getLogger(PeriodicTable.class).error(e.getMessage(), e);
        }
    }

    public PrecursorIonType getPrecursorIonTypeForEI() {
        throw new UnsupportedOperationException();
    }

    private IonMode[] POSITIVE_ION_MODES, NEGATIVE_ION_MODES;
    private Charge UNKNOWN_IONIZATION, POSITIVE_IONIZATION, NEGATIVE_IONIZATION;
    private IonMode PROTONATION, DEPROTONATION;
    private PrecursorIonType UNKNOWN_IONTYPE, UNKNOWN_POSITIVE_IONTYPE, UNKNOWN_NEGATIVE_IONTYPE, INTRINSICALLY_CHARGED_POSITIVE, INTRINSICALLY_CHARGED_NEGATIVE;
    private IonMode NEUTRAL_IONIZATION_DUMMY;

    public Iterable<Ionization> getKnownIonModes(int charge) {
        if (Math.abs(charge) != 1) throw new MultipleChargeException("Do not support multiple charges yet");
        if (charge > 0) {
            return Arrays.asList((Ionization[]) POSITIVE_ION_MODES);
        } else {
            return Arrays.asList((Ionization[]) NEGATIVE_ION_MODES);
        }
    }

    /**
     * returns a list of PrecursorIonType instances with the given charge
     * ion types at the beginning of the list are more common/likely than ion types
     * at the end of the list
     *
     * @param charge
     * @return
     */
    public Iterable<PrecursorIonType> getKnownLikelyPrecursorIonizations(int charge) {
        if (charge == 0) throw new MultipleChargeException("Do not support uncharged compounds");
        if (Math.abs(charge) != 1) throw new MultipleChargeException("Do not support multiple charges yet");
        final HashSet<PrecursorIonType> ions = new HashSet<PrecursorIonType>();
        for (PrecursorIonType ionType : knownIonTypes.values()) {
            if (ionType.getCharge() == charge) ions.add(ionType);
        }
        final ArrayList<PrecursorIonType> likely = new ArrayList<PrecursorIonType>();

        if (charge > 0) {
            likely.add(ionByNameOrThrow("[M+H]+"));
            likely.add(ionByNameOrThrow("[M]+"));
            likely.add(ionByNameOrThrow("[M+H-H2O]+"));
            likely.add(ionByNameOrThrow("[M+Na]+"));
            for (PrecursorIonType i : likely) ions.remove(i);
            likely.addAll(ions);
        } else if (charge < 0) {
            likely.add(ionByNameOrThrow("[M-H]-"));
            likely.add(ionByNameOrThrow("[M]-"));
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
        this.POSITIVE_IONIZATION = new Charge(1);
        this.NEGATIVE_IONIZATION = new Charge(-1);
        this.UNKNOWN_IONIZATION = new Charge(0); //lets use zero for unknown
        this.NEUTRAL_IONIZATION_DUMMY = new IonMode(0, 1, "NEUTRAL_IONIZATION", MolecularFormula.emptyFormula());
        PROTONATION = new IonMode(1, "[M + H]+", MolecularFormula.parseOrThrow("H"));
        DEPROTONATION = new IonMode(-1, "[M - H]-", MolecularFormula.parseOrThrow("H").negate());
        this.UNKNOWN_NEGATIVE_IONTYPE = new PrecursorIonType(NEGATIVE_IONIZATION, MolecularFormula.emptyFormula(), MolecularFormula.emptyFormula(),1, PrecursorIonType.SPECIAL_TYPES.UNKNOWN);
        this.UNKNOWN_POSITIVE_IONTYPE = new PrecursorIonType(POSITIVE_IONIZATION, MolecularFormula.emptyFormula(), MolecularFormula.emptyFormula(), 1, PrecursorIonType.SPECIAL_TYPES.UNKNOWN);
        this.UNKNOWN_IONTYPE = new PrecursorIonType(UNKNOWN_IONIZATION, MolecularFormula.emptyFormula(), MolecularFormula.emptyFormula(), 1, PrecursorIonType.SPECIAL_TYPES.UNKNOWN);

        this.POSITIVE_ION_MODES = new IonMode[]{
                new IonMode(1, "[M + K]+", MolecularFormula.parseOrThrow("K")),
                new IonMode(1, "[M + Na]+", MolecularFormula.parseOrThrow("Na")),
                // TODO: we have to add this!
                //new IonMode(1, "[M - H + Na2]+", MolecularFormula.parseOrThrow("Na2").subtract(MolecularFormula.parseOrThrow("H"))),
                PROTONATION
        };
        this.NEGATIVE_ION_MODES = new IonMode[]{
                new IonMode(-1, "[M + Cl]-", MolecularFormula.parseOrThrow("Cl")),
                new IonMode(-1, "[M + Br]-", MolecularFormula.parseOrThrow("Br")),
                DEPROTONATION
        };
        this.INTRINSICALLY_CHARGED_NEGATIVE = new PrecursorIonType(DEPROTONATION, MolecularFormula.emptyFormula(), MolecularFormula.emptyFormula(), 1, PrecursorIonType.SPECIAL_TYPES.INTRINSICAL_CHARGED);
        this.INTRINSICALLY_CHARGED_POSITIVE = new PrecursorIonType(PROTONATION, MolecularFormula.emptyFormula(), MolecularFormula.emptyFormula(), 1, PrecursorIonType.SPECIAL_TYPES.INTRINSICAL_CHARGED);
        loadKnownIonTypes();
    }

    public void loadKnownIonTypes() {
        //clear maps
        knownIonTypes.clear();
        ionizationToAdduct.clear();

        // ADDUCTS
        //create positives
        String ss = PropertyManager.getProperty("de.unijena.bioinf.sirius.chem.adducts.positive");
        final String[] adductsPositive = ss.split(",");
        for (String pos : adductsPositive) {
            try {
                PrecursorIonType type = ionByName(pos);
                if (type.getIonization().getCharge() <= 0) {
                    LoggerFactory.getLogger(getClass()).warn("Positive IonType with wrong charge: " + pos + " Skipping this Entry!");
                    continue;
                }
                addCommonIonType(type);
            } catch (UnknownElementException e) {
                LoggerFactory.getLogger(getClass()).warn("Positive IonIonType with contains unknown Elements: " + pos + " Skipping this Entry!", e);
            }
        }


        //create negatives
        final String[] adductsNegative = PropertyManager.getProperty("de.unijena.bioinf.sirius.chem.adducts.negative").split(",");
        for (String neg : adductsNegative) {
            try {
                PrecursorIonType type = ionByName(neg);
                if (type.getIonization().getCharge() >= 0) {
                    LoggerFactory.getLogger(getClass()).warn("Negative IonType with wrong charge: " + neg + " Skipping this Entry!");
                    continue;
                }
                addCommonIonType(type);
            } catch (UnknownElementException e) {
                LoggerFactory.getLogger(getClass()).warn("Negative IonIonType with contains unknown Elements: " + neg + " Skipping this Entry!", e);

            }
        }

        // add common misspelled aliases...
        final PrecursorIonType hplus = ionByNameOrThrow("[M+H]+");
        final PrecursorIonType hminus = ionByNameOrThrow("[M-H]-");
        PROTONATION_PRECURSOR = hplus;
        DEPROTONATION_PRECURSOR = hminus;
        knownIonTypes.put("M+H", hplus);
        knownIonTypes.put("M+H+", hplus);
        knownIonTypes.put("[M+H]", hplus);
        knownIonTypes.put("M-H", hminus);
        knownIonTypes.put("M-H-", hminus);
        knownIonTypes.put("[M-H]", hminus);
        knownIonTypes.put("M+", ionByNameOrThrow("[M]+"));
        knownIonTypes.put("M-", ionByNameOrThrow("[M]-"));
    }

    protected Pattern MULTIMERE_PATTERN = Pattern.compile("\\[\\s*(\\d+)M(?:\\s*[+-]|])");

    private PrecursorIonType parseIonType(String name) throws UnknownElementException {
        int multimereCount = 1;
        Matcher multimereMatcher = MULTIMERE_PATTERN.matcher(name);
        if (multimereMatcher.find()) {
            multimereCount = Integer.parseInt(multimereMatcher.group(1));
        }
        // tokenize String
        final String ACN = "CH3CN"; // ACN
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
        if (lastPos < name.length()) tokens.add(name.substring(lastPos));

        final ArrayList<MolecularFormula> possibleNewIonTypes = new ArrayList<MolecularFormula>();
        final ArrayList<MolecularFormula> adducts = new ArrayList<>();
        final ArrayList<MolecularFormula> insourceFrags = new ArrayList<MolecularFormula>();

        boolean isAdd = true;
        int number = 1;

        for (int k = 0; k < tokens.size(); ++k) {
            final String token = tokens.get(k).trim();
            final char c = token.charAt(0);
            switch (c) {
                case '(':
                    if (number != 1) {
                        throw new MultimereException("Do not support multiplier before a molecular formula: '" + name + "'");
                    } else break;
                case ')':
                    break;
                case '[':
                    break; // ignore
                case ']':
                    break; // ignore
                case '+':
                    if (number != 1) {
                        throw new MultipleChargeException("Do not support multiple charges: '" + name + "'");
                    } else {
                        isAdd = true;
                        break;
                    }
                case '-':
                    if (number != 1) {
                        throw new MultipleChargeException("Do not support multiple charges: '" + name + "'");
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

                        possibleNewIonTypes.add(f);

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
        final IonMode[] addModes = Arrays.stream((charge > 0) ? POSITIVE_ION_MODES : NEGATIVE_ION_MODES)
                .filter((ion) -> ion.getAtoms().atomCount() > 0).toArray(IonMode[]::new);
        final IonMode[] insourceFragModes = Arrays.stream((charge > 0) ? POSITIVE_ION_MODES : NEGATIVE_ION_MODES)
                .filter((ion) -> ion.getAtoms().atomCount() < 0).toArray(IonMode[]::new);


        for (MolecularFormula possibleNewIonType : possibleNewIonTypes.reversed()) {

            if (adducts.contains(possibleNewIonType)) {
                // search for adduct containing this ion
                for (IonMode ion : addModes) {
                    if (ion.getAtoms().equals(possibleNewIonType)) {
                        usedIonMode = ion;
                        adducts.remove(possibleNewIonType);
                        break;
                    }
                }

                if (usedIonMode != null) break;

                for (IonMode ion : addModes) {
                    if (possibleNewIonType.isSubtractable(ion.getAtoms())) {//check for subset
                        usedIonMode = ion;
                        adducts.replaceAll(elements -> elements == possibleNewIonType ? possibleNewIonType.subtract(ion.getAtoms()) : elements);
                        break;
                    }
                }

                if (usedIonMode != null) break;

                if (possibleNewIonType.getNumberOfElements() == 1) {
                    //search for possible new ionmode
                    String ionModeName = "[M + " + possibleNewIonType.toString() + "]" + ((charge < 0) ? "-" : "+");
                    IonMode im = new IonMode(charge, ionModeName, possibleNewIonType);
                    addCommonIonMode(im);
                    usedIonMode = im;
                    adducts.remove(possibleNewIonType);
                    break;
                }

            } else if (insourceFrags.contains(possibleNewIonType)) {
                // search for loss containing this ion
                for (IonMode ion : insourceFragModes) {
                    final MolecularFormula neg = ion.getAtoms().negate();
                    if (neg.equals(possibleNewIonType)) {
                        insourceFrags.remove(possibleNewIonType);
                        usedIonMode = ion;
                        break;
                    }
                }

                if (usedIonMode != null) break;

                for (IonMode ion : insourceFragModes) {
                    final MolecularFormula neg = ion.getAtoms().negate();
                    if (possibleNewIonType.isSubtractable(neg)) {
                        usedIonMode = ion;
                        insourceFrags.replaceAll(elements -> elements == possibleNewIonType ? possibleNewIonType.subtract(neg) : elements);
                        break;
                    }
                }

                if (usedIonMode != null) break;
            }
        }

        //possible ionModes was empty or a negative we could not add as a new one
        if (usedIonMode == null) {
            if (charge < 0 && adducts.size() > 0) {
                adducts.add(MolecularFormula.getHydrogen());
                usedIonMode = DEPROTONATION;
            } else if (charge > 0 && !insourceFrags.isEmpty()) {
                insourceFrags.add(MolecularFormula.getHydrogen());
                usedIonMode = PROTONATION;
            }
        }


        MolecularFormula adduct = MolecularFormula.emptyFormula();
        for (
                MolecularFormula f : adducts)
            adduct = adduct.add(f);
        MolecularFormula insource = MolecularFormula.emptyFormula();
        for (
                MolecularFormula f : insourceFrags)
            insource = insource.add(f);

        if (usedIonMode == null && adduct.isEmpty() && insource.isEmpty()) {
            return charge > 0 ? INTRINSICALLY_CHARGED_POSITIVE : INTRINSICALLY_CHARGED_NEGATIVE;
        } else if (usedIonMode == null) {
            throw new RuntimeException("Cannot parse " + name);
        } else return new

                PrecursorIonType(usedIonMode, insource, adduct, multimereCount, PrecursorIonType.SPECIAL_TYPES.REGULAR);
    }


    public IonMode getProtonation() {
        return PROTONATION;
    }

    public IonMode getDeprotonation() {
        return DEPROTONATION;
    }

    private PrecursorIonType PROTONATION_PRECURSOR, DEPROTONATION_PRECURSOR;

    public PrecursorIonType getPrecursorProtonation() {
        return PROTONATION_PRECURSOR;
    }

    public PrecursorIonType getPrecursorDeprotonation() {
        return DEPROTONATION_PRECURSOR;
    }

    public PrecursorIonType getPrecursorIonTypeFromIonization(Ionization ion) {
        if (ion instanceof Charge) {
            if (ion.getCharge() == 1) return UNKNOWN_POSITIVE_IONTYPE;
            else if (ion.getCharge() == -1) return UNKNOWN_NEGATIVE_IONTYPE;
            else if (ion.getCharge() == 0) return UNKNOWN_IONTYPE;

            else throw new MultipleChargeException("Multiple charges are not supported yet");
        }

        for (PrecursorIonType i : knownIonTypes.values()) {
            if (!i.isIntrinsicalCharged() && i.getIonization().equals(ion) && i.getAdduct().atomCount() == 0 && i.getInSourceFragmentation().atomCount() == 0 && !i.isMultimere())
                return i;
        }
        return new PrecursorIonType(ion, MolecularFormula.emptyFormula(), MolecularFormula.emptyFormula(), 1, PrecursorIonType.SPECIAL_TYPES.REGULAR);
    }

    public PrecursorIonType unknownPositivePrecursorIonType() {
        return UNKNOWN_POSITIVE_IONTYPE;
    }

    public PrecursorIonType unknownNegativePrecursorIonType() {
        return UNKNOWN_NEGATIVE_IONTYPE;
    }

    public Ionization neutralIonization() {
        return NEUTRAL_IONIZATION_DUMMY;
    }

    public PrecursorIonType getUnknownPrecursorIonType(int charge) {
        if (charge == 1) return UNKNOWN_POSITIVE_IONTYPE;
        else if (charge == -1) return UNKNOWN_NEGATIVE_IONTYPE;
        else if (charge == 0) throw new IllegalArgumentException("unknown ion type with unknown charge");
        throw new MultipleChargeException("Do not support multiple charges yet.");
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
    private final HashMap<String, Set<PrecursorIonType>> ionizationToAdduct;
    /**
     * Cache which is used to build molecular formulas with shared structure
     */
    final TableSelectionCache cache;

    PeriodicTable() {
        this.elements = new ArrayList<>();
        this.nameMap = new HashMap<>();
        this.cache = new TableSelectionCache(this, TableSelectionCache.DEFAULT_MAX_COMPOMERE_SIZE);
        this.knownIonTypes = new HashMap<>();
        this.ionizationToAdduct = new HashMap<>();
        this.distribution = new IsotopicDistribution(this);
        this.emptyFormula = null;
    }

    PeriodicTable(PeriodicTable pt) {
        this.elements = new ArrayList<>(pt.elements);
        this.nameMap = new HashMap<>(pt.nameMap);
        this.pattern = pt.pattern;
        this.knownIonTypes = new HashMap<>();
        this.ionizationToAdduct = new HashMap<>();
        // new cache =(
        this.cache = new TableSelectionCache(this, TableSelectionCache.DEFAULT_MAX_COMPOMERE_SIZE);
        this.emptyFormula = null;
        this.distribution = new IsotopicDistribution(this);
        distribution.merge(pt.distribution);
    }


    // add TableSelection into cache or reuse an existing cached table selection
    // returns the table selection from the cache (new or already existing). It is
    // guaranteed that the returned table selection is compatible to the given selection
    public TableSelection tryToAddTableSelectionIntoCache(TableSelection selection) {
        return cache.addToCache(selection);
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
        name = canonicalizeIonName(name);
        if (knownIonTypes.containsKey(name)) {
            if (ionType.equals(knownIonTypes.get(name))) return false;
            else throw new IllegalArgumentException("There is already an ionization with name '" + name + "'");
        }

        //add the iontype to known knownIonTypes
        knownIonTypes.put(name, ionType);

        //check if the ionisation is already in knownIonTypes and add it if not
        final String ionName = canonicalizeIonName(ionType.getIonization().getName());
        addCommonIonType(ionName, ionByNameOrThrow(ionName));

        //add adduct to list of adducts with common ionisation
        Set<PrecursorIonType> adducts = ionizationToAdduct.get(ionName);
        if (adducts == null) {
            adducts = new LinkedHashSet<>();
            ionizationToAdduct.put(ionName, adducts);
        }
        adducts.add(ionType);
        return true;
    }

    /**
     * @see PeriodicTable#addCommonIonType(String, PrecursorIonType)
     * uses the normalized name of the ion type
     */
    public boolean addCommonIonType(PrecursorIonType ionType) {
        return addCommonIonType(ionType.toString(), ionType);
    }

    public boolean addCommonIonType(String name) throws UnknownElementException {
        return addCommonIonType(ionByName(name));
    }

    public boolean addCommonIonMode(IonMode ionMode) {
//        name = canonicalizeIonName(name);

        int charge = ionMode.getCharge();

        if (Math.abs(charge) != 1) {
            throw new IllegalArgumentException("Currently, only ion modes with single positive/netagive charge are supported");
        }

        IonMode[] knownModes = charge > 0 ? POSITIVE_ION_MODES : NEGATIVE_ION_MODES;

        if (arrayContains(knownModes, ionMode)) {
            return false;
        }

        knownModes = Arrays.copyOf(knownModes, knownModes.length + 1);

        knownModes[knownModes.length - 1] = knownModes[knownModes.length - 2];
        knownModes[knownModes.length - 2] = ionMode;

        if (charge > 0) POSITIVE_ION_MODES = knownModes;
        else NEGATIVE_ION_MODES = knownModes;

        return true;
    }

    private boolean arrayContains(Object[] array, Object element) {
        for (Object o : array) {
            if (o == null) {
                if (element == null) return true;
                continue;
            }
            if (o.equals(element)) {
                return true;
            }
        }
        return false;
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
        if (elements.length==0) return BitSet.valueOf(new long[0]);
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
     * @return an immutable list of iondetection
     */
    public Collection<PrecursorIonType> getIons() {
        return new HashSet<>(knownIonTypes.values());
    }

    /**
     * @return the set of different Ionization types
     */
    public Set<String> getIonizationsAsString() {
        return ionizationToAdduct.keySet().stream().map((it) -> knownIonTypes.get(it).getIonization().getName()).collect(Collectors.toSet());
    }

    /**
     * @return the set of different Ionization types
     */
    public Set<PrecursorIonType> getIonizations() {
        return ionizationToAdduct.keySet().stream().map(this::ionByNameOrThrow).collect(Collectors.toSet());
    }

    public Set<PrecursorIonType> getIonizations(final int charge) {
        if (charge > 0)
            return getPositiveIonizations();
        else if (charge < 0)
            return getNegativeIonizations();
        else
            return getIonizations();
    }

    /**
     * @return the set of different Ionization types inlcuding the 3 different unknown types ([M+?]+,[M+?]-,[M+?])
     */
    public Collection<String> getIonizationsAndUnknowns() {
        Set<String> result = new HashSet<>(getIonizationsAsString());
        result.add(unknownPositivePrecursorIonType().getIonization().getName());
        result.add(unknownNegativePrecursorIonType().getIonization().getName());
        return result;
    }


    /**
     * @return the set of different positive Ionization types
     */
    public Set<String> getPositiveIonizationsAsString() {
        Set<String> positives = new HashSet<>();
        for (String name : ionizationToAdduct.keySet()) {
            PrecursorIonType ionType = knownIonTypes.get(name);
            if (ionType.getIonization().getCharge() > 0)
                positives.add(ionType.getIonization().getName());
        }
        return positives;
    }

    /**
     * @return the set of different positive Ionization types
     */
    public Set<PrecursorIonType> getPositiveIonizations() {
        Set<PrecursorIonType> positives = new HashSet<>();
        for (String ionType : ionizationToAdduct.keySet()) {
            if (knownIonTypes.get(ionType).getIonization().getCharge() > 0)
                positives.add(ionByNameOrThrow(ionType));
        }
        return positives;
    }

    /**
     * @return the set of different positive Ionization types
     */
    public Set<String> getNegativeIonizationsAsString() {
        Set<String> negatives = new HashSet<>();
        for (String name : ionizationToAdduct.keySet()) {
            PrecursorIonType ionType = knownIonTypes.get(name);
            if (ionType.getIonization().getCharge() < 0)
                negatives.add(ionType.getIonization().getName());
        }
        return negatives;
    }

    /**
     * @return the set of different positive Ionization types
     */
    public Set<PrecursorIonType> getNegativeIonizations() {
        Set<PrecursorIonType> negatives = new HashSet<>();
        for (String ionType : ionizationToAdduct.keySet()) {
            if (knownIonTypes.get(ionType).getIonization().getCharge() < 0)
                negatives.add(ionByNameOrThrow(ionType));
        }
        return negatives;
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
     * given mass error. If there multiple iondetection in the mass window, the method returns the ion with
     * the lowest mass error.
     *
     * @param mass
     * @param absError
     * @return an ion with the given mass or null if no ion is found
     */
    public PrecursorIonType ionByMass(double mass, double absError, int charge) {
        if (charge > 0 && Math.abs(mass - PROTONATION.getMass()) < absError)
            return PROTONATION_PRECURSOR;
        else if (charge < 0 && Math.abs(mass - DEPROTONATION.getMass()) < absError)
            return DEPROTONATION_PRECURSOR;
        if (Math.abs(mass) < absError) {
            if (charge>0) return INTRINSICALLY_CHARGED_POSITIVE;
            else return INTRINSICALLY_CHARGED_NEGATIVE;
        }
        PrecursorIonType minIon = null;
        double minDistance = Double.MAX_VALUE;
        for (PrecursorIonType iontype : knownIonTypes.values()) {
            final Ionization ion = iontype.getIonization();
            if (charge != 0 && ion.getCharge() != charge) continue;
            if (iontype.isMultimere()) continue;
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
    public PrecursorIonType ionByName(String name) throws UnknownElementException {
        PrecursorIonType re = ionByNameFromTableOrNull(name);
        return re != null ? re : parseIonType(name);
    }

    public PrecursorIonType ionByNameOrThrow(String name) {
        try {
            return ionByName(name);
        } catch (UnknownElementException e) {
            throw new IllegalArgumentException("Could not parse IonType: " + name, e);
        }
    }

    public PrecursorIonType ionByNameOrNull(String name) {
        try {
            return ionByName(name);
        } catch (UnknownElementException e) {
            LoggerFactory.getLogger(MolecularFormula.class).warn("Cannot parse Formula `" + name + "`.", e);
            return null;
        }
    }


    public boolean hasIon(String name) {
        return ionByNameFromTableOrNull(name) != null;
    }

    private PrecursorIonType ionByNameFromTableOrNull(String name) {
        name = canonicalizeIonName(name);
        if (name.equals(canonicalizeIonName(Charge.POSITIVE_CHARGE)) || name.equals("M+?+"))
            return PrecursorIonType.unknownPositive();
        if (name.equals(canonicalizeIonName(Charge.NEGATIVE_CHARGE)) || name.equals("M+?-") || name.equals("[M-?]-") || name.equals("M-?-"))
            //[M-?]- is actually an incorrect use of [M+?]-. However, we still want it to be parse correctly
            return PrecursorIonType.unknownNegative();

        return knownIonTypes.get(name);
    }


    public Set<PrecursorIonType> getAdducts() {
        Set<PrecursorIonType> adducts = new HashSet<>(knownIonTypes.values().size() + 3);
        adducts.addAll(knownIonTypes.values());
        return adducts;
    }

    /**
     * @return the set of different positive adducts
     */
    public Set<String> getPositiveAdductsAsString() {
        Set<String> adducts = new HashSet<>(knownIonTypes.values().size());
        for (PrecursorIonType ionType : knownIonTypes.values()) {
            if (ionType.getCharge() > 0)
                adducts.add(ionType.toString());
        }
        return adducts;
    }

    public Set<PrecursorIonType> getPositiveAdducts() {
        Set<PrecursorIonType> adducts = new HashSet<>(knownIonTypes.values().size());
        for (PrecursorIonType ionType : knownIonTypes.values()) {
            if (ionType.getCharge() > 0)
                adducts.add(ionType);
        }
        return adducts;
    }

    public Set<String> getNegativeAdductsAsString() {
        Set<String> adducts = new HashSet<>(knownIonTypes.values().size());
        for (PrecursorIonType ionType : knownIonTypes.values()) {
            if (ionType.getCharge() < 0)
                adducts.add(ionType.toString());
        }
        return adducts;
    }

    public Set<PrecursorIonType> getNegativeAdducts() {
        Set<PrecursorIonType> adducts = new HashSet<>(knownIonTypes.values().size());
        for (PrecursorIonType ionType : knownIonTypes.values()) {
            if (ionType.getCharge() < 0)
                adducts.add(ionType);
        }
        return adducts;
    }

    public Set<PrecursorIonType> getAdductsAndUnKnowns() {
        Set<PrecursorIonType> adducts = getAdducts();
        adducts.add(PrecursorIonType.unknownPositive());
        adducts.add(PrecursorIonType.unknownNegative());
        return adducts;
    }


    public Set<PrecursorIonType> adductsByIonisation(Collection<PrecursorIonType> ionMode) {
        Set<PrecursorIonType> adducts = new HashSet<>();
        for (PrecursorIonType ionization : ionMode) {
            adducts.addAll(adductsByIonisation(ionization));
        }
        return adducts;
    }

    public Set<PrecursorIonType> adductsByIonisation(PrecursorIonType ionMode) {
        if (ionMode.isIonizationUnknown()) {
            if (ionMode.isUnknownPositive()) {
                adductsFromIonizationNames(getPositiveIonizationsAsString());
            } else if (ionMode.isUnknownNegative()) {
                adductsFromIonizationNames(getNegativeIonizationsAsString());
            } else {
                adductsFromIonizationNames(getIonizationsAsString());
            }
        }
        return adductsFromIonizationName(ionMode.getIonization().toString());
    }


    private Set<PrecursorIonType> adductsFromIonizationNames(Collection<String> name) {
        Set<PrecursorIonType> adducts = new HashSet<>();
        for (String ionization : name) {
            adducts.addAll(adductsFromIonizationName(ionization));
        }
        return adducts;
    }

    private Set<PrecursorIonType> adductsFromIonizationName(String name) {
        name = canonicalizeIonName(name);
        PrecursorIonType p = knownIonTypes.get(name);
        if (p == null) return Collections.emptySet();
        Set<PrecursorIonType> r = ionizationToAdduct.get(canonicalizeIonName(p.getIonization().getName()));
        return r == null ? Collections.<PrecursorIonType>emptySet() : Collections.unmodifiableSet(r);
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
            if (iso != null) { //todo @Kai null check correct??
                for (int k = 1; k < iso.getNumberOfIsotopes(); ++k) {
                    final int i = iso.getIntegerMass(k) - e.getIntegerMass();
                    if (i > peakOffset) break;
                    double diff = iso.getMassDifference(k) - i;
                    diff *= (peakOffset / i);
                    minmz = Math.min(minmz, diff);
                    maxmz = Math.max(maxmz, diff);
                }
            }
        }
        final double a = monomz + peakOffset + minmz;
        final double b = monomz + peakOffset + maxmz;
        return Range.of(a - deviation.absoluteFor(a), b + deviation.absoluteFor(b));
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
    public void parse(String formula, FormulaVisitor<?> visitor) throws UnknownElementException {
        if (formula.indexOf('(') < 0) {
            parseUnstackedFormula(formula, visitor);
        } else {
            parseStackedFormula(formula, visitor);
        }
    }

    private void parseStackedFormula(String formula, FormulaVisitor<?> visitor) throws UnknownElementException {
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
                    if (element == null)
                        throw new UnknownElementException("\"" + elementName + "\" could not be found in periodic table!");
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

    private void parseUnstackedFormula(String formula, FormulaVisitor<?> visitor) throws UnknownElementException {
        final int multiplier;
        if (formula.isEmpty()) return;
        if (Character.isDigit(formula.charAt(0))) {
            int lastnum = 0;
            while (lastnum < formula.length() && Character.isDigit(formula.charAt(lastnum))) ++lastnum;
            multiplier = Integer.parseInt(formula.substring(0, lastnum));
            formula = formula.substring(lastnum, formula.length());
        } else multiplier = 1;
        final Matcher matcher = getPattern().matcher(formula);
        while (matcher.find()) {
            final String elementName = matcher.group(1);
            final String elementAmount = matcher.group(2);
            final Element element = getByName(elementName);
            if (element == null)
                throw new UnknownElementException("\"" + elementName + "\" could not be found in periodic table!");

            final int amount = multiplier * (elementAmount != null && elementAmount.length() > 0 ?
                    Integer.parseInt(elementAmount) : 1);
            visitor.visit(element, amount);
        }
    }
}
