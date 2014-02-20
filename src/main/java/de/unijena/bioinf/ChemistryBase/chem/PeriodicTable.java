package de.unijena.bioinf.ChemistryBase.chem;

import de.unijena.bioinf.ChemistryBase.chem.utils.*;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Give access to all chemical elements and ions. This class should be seen as singleton, although it's
 * possible to create multiple PeriodicTables
 * All this information are parsed from a json file in the ChemistryBase library.
 *
 * The PeriodicTable is not thread-safe, because in practice there should be only read-accesses. For write access,
 * you have to do the synchronisation yourself.
 * 
 * <pre>
 * PeriodicTable.getInstance().getByName("C").getMass();
 * PeriodicTable.getInstance().parse("C6H12O6", myAtomVisitor)
 * </pre>
 * 
 *
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
            if (st ==null) {
                stack = new ArrayList<PeriodicTable>();
                localInstanceStack.set(stack);
            } else stack = st;
            final PeriodicTable t = localInstance.get();
            if (t==null) before = instance; else before = t;
        } else {
            stack = instanceStack;
            before = instance;
        }
        stack.add(before);
        if (threadLocal) localInstance.set(pt); else instance = pt;
        return pt;
    }

    /**
     * Add a new empty periodic table to the stack and enable it. Can be used to change temporarily a periodic table
     * @return the added periodic table
     */
    public static PeriodicTable push() {
        return push(new PeriodicTable());
    }

    /**
     * add a copy of the current periodic table to the stack. Can be used to change temporarily a periodic table
     * @return the added periodic table
     */
    public static PeriodicTable pushCopy() {
        return push(instance.clone());
    }

    /**
     * removes the last periodic table which was added to the stack and enable the previous table. Use together with
     * push or pushCopy to change temporarily a periodic table.
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
                localInstance.set(stack.get(stack.size()-1));
                stack.remove(stack.size()-1);
            }
        } else {
            ret = instance;
            if (instanceStack.size() == 0) throw new RuntimeException("No further periodic table in stack");
            instance = instanceStack.get(instanceStack.size()-1);
            instanceStack.remove(instanceStack.size()-1);
        }
        return ret;
    }

    /**
     * returns true if the periodic table may be different in different threads.
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
            e.printStackTrace();
        }

    }

    private void addDefaultIons() {
        final String[] ions = new String[]{"[M+H]+", "[M-H]-", "[M]+", "[M]-", "[M-2H]-", "[M+K]+", "[M+K-2H]+", "[M+K-2H]-",
                "[M-OH]-", "[M+Na]+", "[M+Cl]-", "[M+H-H2O]+", "[M-H+Na]+", "[M+2Na-H]+","[M+Na2-H]+", "[M+NH3+H]+", "[(M+NH3)+H]+", "[M+NH4]+",
                "[M+H-C6H10O4]+", "[M+H-C6H10O5]+", "[M-H+OH]-", "[M+HCOO-]-", "[M+CH3COOH-H]-", "[(M+CH3COOH)-H]-"
        };
        final MolecularFormula[] formulas = new MolecularFormula[]{
                MolecularFormula.parse("H"),
                MolecularFormula.parse("H").negate(),
                MolecularFormula.parse(""),
                MolecularFormula.parse(""),
                MolecularFormula.parse("H2").negate(),
                MolecularFormula.parse("K"),
                MolecularFormula.parse("K").subtract(MolecularFormula.parse("H2")),
                MolecularFormula.parse("K").subtract(MolecularFormula.parse("H2")),
                MolecularFormula.parse("OH").negate(),
                MolecularFormula.parse("Na"),
                MolecularFormula.parse("Cl"),
                MolecularFormula.parse("HO").negate() ,
                MolecularFormula.parse("Na").subtract(MolecularFormula.parse("H")),
                MolecularFormula.parse("Na2").subtract(MolecularFormula.parse("H")),MolecularFormula.parse("Na2").subtract(MolecularFormula.parse("H")),
                MolecularFormula.parse("NH4"), MolecularFormula.parse("NH4"), MolecularFormula.parse("NH4"),
                MolecularFormula.parse("H").subtract(MolecularFormula.parse("C6H10O4")),
                MolecularFormula.parse("H").subtract(MolecularFormula.parse("C6H10O5")),
                MolecularFormula.parse("O"),
                MolecularFormula.parse("HCO2"),
                MolecularFormula.parse("C2H3O2"), MolecularFormula.parse("C2H3O2")
        };

        assert ions.length==formulas.length;
        for (int i=0; i < ions.length; ++i) {
            final String ion = ions[i];
            final MolecularFormula form = formulas[i];
            final int charge = ion.endsWith("+") ? 1 : -1;
            final Adduct adduct = new Adduct(form.getMass() - Charge.ELECTRON_MASS *charge, charge , ion, form );
            ionizations.add(adduct);
            ionNameMap.put(ion, adduct);
            ionMap.put(adduct.getMass(), adduct);
        }
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
    		this.amount = (short)amount;
    	}
    }
    

    /**
     * Regular Expression for molecular formulas regarding all chemical elements in PeriodicTable
     */
    private Pattern pattern;
    private final HashMap<String, Element> nameMap;
    private final ArrayList<Ionization> ionizations;
    private final NavigableMap<Double, Ionization> ionMap;
    private final HashMap<String, Ionization> ionNameMap;
    private final ArrayList<Element> elements;
    private IsotopicDistribution distribution;
    private MolecularFormula emptyFormula;
    /**
     * Cache which is used to build molecular formulas with shared structure
     */
    final TableSelectionCache cache;
    
    /**
     * build a periodic table with standard values.
     */
    /*
    PeriodicTable() {
        this.elements = new Element[Static.ELEMENT_SYMBOL.length];
        for (int i=0; i < elements.length; ++i) {            
            elements[i] = new Element(i);
        }
        this.nameMap = new HashMap<String, Element>((int)(elements.length*1.5));
        for (Element e : elements) {
            if (e != null) nameMap.put(e.getSymbol(), e);
        }
        final ArrayList<String> names = new ArrayList<String>(nameMap.keySet());
        Collections.sort(names, new Comparator<String>() {
            @Override public int compare(String s, String s1) {
                return s1.length() - s.length();
            }
        });
        final StringBuilder buffer = new StringBuilder();
        final Iterator<String> nameIterator = names.iterator();
        buffer.append("(\\)|");
        buffer.append(nameIterator.next());
        while (nameIterator.hasNext()) {
            buffer.append("|").append(nameIterator.next());
        }
        buffer.append(")(\\d*)|\\(");
        this.pattern = Pattern.compile(buffer.toString());
        this.ions = Arrays.copyOf(IONS, IONS.length + ION_FORMULAS.length);
        this.cache = new TableSelectionCache(this, TableSelectionCache.DEFAULT_MAX_COMPOMERE_SIZE);
        for (int i=IONS.length; i < ions.length; ++i) {
        	ions[i] = __parseIonFromString(ION_FORMULAS[i-IONS.length]);
        }
        Arrays.sort(ions);
    }
    */

    PeriodicTable() {
        this.elements = new ArrayList<Element>();
        this.nameMap = new HashMap<String, Element>();
        this.ionizations = new ArrayList<Ionization>();
        this.cache = new TableSelectionCache(this, TableSelectionCache.DEFAULT_MAX_COMPOMERE_SIZE);
        this.ionMap = new TreeMap<Double, Ionization>(); // TODO: Use MultiMaps
        this.ionNameMap = new HashMap<String, Ionization>();
        this.distribution = new IsotopicDistribution(this);
        this.emptyFormula = null;
    }

    PeriodicTable(PeriodicTable pt) {
        this.elements = new ArrayList<Element>(pt.elements);
        this.nameMap = new HashMap<String, Element>(pt.nameMap);
        this.ionizations = new ArrayList<Ionization>(pt.ionizations);
        this.pattern = pt.pattern;
        this.ionMap = new TreeMap<Double, Ionization>(pt.ionMap);
        // new cache =(
        this.cache = new TableSelectionCache(this, TableSelectionCache.DEFAULT_MAX_COMPOMERE_SIZE);
        this.ionNameMap = new HashMap<String, Ionization>(pt.ionNameMap);
        this.emptyFormula = null;
        this.distribution = new IsotopicDistribution(this);
        distribution.merge(pt.distribution);
    }

    public MolecularFormula emptyFormula() {
        if (emptyFormula==null) emptyFormula = MolecularFormula.fromCompomer(cache.getSelectionFor(new BitSet()), new short[0]);
        return emptyFormula;
    }

    public void addElement(String name, String symbol, double mass, int valence) {
        if (nameMap.containsKey(symbol)) throw new IllegalArgumentException("There is already an element with name '" + symbol + "'");
        elements.add(new Element(elements.size(), name, symbol, mass, valence));
        nameMap.put(symbol, elements.get(elements.size()-1));
        pattern = null;
    }

    public void addIonizationAdduct(Adduct adduct) {
        if (ionNameMap.containsKey(adduct.getName())) throw new IllegalArgumentException("There is already an ionization with name '" + adduct.getName() + "'");
        ionMap.put(adduct.getMass(), adduct);
        ionNameMap.put(adduct.getName(), adduct);
        ionizations.add(adduct);
    }

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
            for (int i=0; i < d.getNumberOfIsotopes(); ++i) {
                if (d.getAbundance(i) > 0 && d.getMass(i) < minMass) {
                    minMass = d.getMass(i);
                }
            }
            e.mass = minMass;
            e.nominalMass = (int)(Math.round(minMass));
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

    
    //private static final Pattern ION_PATTERN = Pattern.compile("\\s*\\A\\[M\\s*([+-])\\s*(.+)([+-]?)\\]\\s*([+-])\\s*(\\d*)\\Z");
    Adduct __parseIonFromString(String s) {
        if (true) throw new RuntimeException("The current implementation seems to be buggy!"); // TODO: FIX!!!
    	final Pattern ION_PATTERN = Pattern.compile("\\s*\\A\\[M\\s*(?:([+-])\\s*(.+))?([+-]?)\\]\\s*([+-])\\s*(\\d*)\\Z");
    	final Matcher m = ION_PATTERN.matcher(s);
    	if (!m.find()) throw new RuntimeException("Can't parse ion: '" + s + "'");
    	final int charge = (m.group(4).equals("+") ? 1 : -1);
    	final int numberOfCharges = m.group(5).isEmpty() ? 1 : Integer.parseInt(m.group(4));
    	final boolean add = m.group(1) == null || m.group(1).equals("+");
    	final MolecularFormula molecule = MolecularFormula.parse(m.group(2) == null ? "" : m.group(2), this);
    	//final char electron = m.group(3).isEmpty() ? '.' : m.group(3).charAt(0);
    	double mass = molecule.getMass();
    	//switch (electron) {
			/*case '+':*/if (charge > 0) mass -= Charge.ELECTRON_MASS *numberOfCharges;//break;
			/*case '-':*/else if (charge < 0) mass += Charge.ELECTRON_MASS *numberOfCharges;// break;
			//default:
		//}
    	return (add) 	? new Adduct(mass, numberOfCharges*charge, s, molecule) 
    					: new Adduct(-mass, numberOfCharges*charge, s, molecule.negate());
    }
    
    /**
     * build a bitset of an array of elements. The i-th bit is set if the i-th element is contained
     * in the array
     */
    BitSet bitsetOfElements(Element...elements ) {
    	final int maxId = Collections.max(Arrays.asList(elements), new Comparator<Element>() {
			@Override
			public int compare(Element o1, Element o2) {
				return o1.getId()-o2.getId();
			}
		}).getId();
    	final BitSet bitset = new BitSet(maxId+1);
    	for (Element e : elements) {
    		bitset.set(e.getId());
    	}
    	return bitset;
    }
    
    /**
     * @return an immutable list of ions
     */
    public List<Ionization> getIons() {
    	return Collections.unmodifiableList(ionizations);
    }
    
    /**
     * return the element with the given Id
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
     * @param names
     * @return array with Element instances or nulls.
     */
    public Element[] getAllByName(String... names) {
    	final Element[] elements = new Element[names.length];
    	for (int i=0; i < names.length; ++i) {
    		elements[i] = getByName(names[i]);
    	}
    	return elements;
    }
    
    /**
     * return the element with the given symbol.
     * @param name
     * @return an Element or null if there is no element with this symbol
     */
    public Element getByName(String name) {
        return nameMap.get(name);
    }
    
    /**
     * Search for a TableSelection which contains at least all the given elements. Use this, if you
     * want to control onto which TableSelection your molecules are built.
     * @param elements
     * @return
     */
    public TableSelection getSelectionFor(Element...elements) {
    	return getSelectionFor(bitsetOfElements(elements));
    }

    /**
     * Search for a TableSelection which contains at least all the given elements. Use this, if you
     * want to control onto which TableSelection your molecules are built.
     * @return
     */
    public TableSelection getSelectionFor(BitSet bitset) {
        return cache.getSelectionFor(bitset);
    }

    public Ionization ionByMass(double mass, double absError) {
        return ionByMass(mass, absError, 0);
    }

    /**
     * search for a known ion which mass is corresponding to the given mass while considering the
     * given mass error. If there multiple ions in the mass window, the method returns the ion with
     * the lowest mass error.
     * @param mass
     * @param absError
     * @return an ion with the given mass or null if no ion is found
     */
    public Ionization ionByMass(double mass, double absError, int charge) {
        Ionization minIon = null;
        double minDistance = Double.MAX_VALUE;
        for (Ionization ion : ionMap.subMap(mass-absError, mass+absError).values()) {
            if (charge!=0 && ion.getCharge() != charge) continue;
            final double abw = (ion.getMass()-mass);
            if (abw < minDistance) {
                minDistance = abw;
                minIon = ion;
            }
        }
        return minIon;
    }
    
    /**
     * search for an ion with the given name. Usually, the names are in the format '[M'[+-]X']'[+-] where
     * X is a molecular formula, for example [M+H2O]+.
     *
     * [M+H]+
     *
     */
    public Ionization ionByName(String name) {
    	if (ionNameMap.containsKey(name)) return ionNameMap.get(name);
    	Ionization a;
    	try {
    		a = __parseIonFromString(name);
    	} catch (RuntimeException e) {
    		return null;
    	}
    	for (Ionization ion : ionMap.subMap(a.getMass()-1e-2, a.getMass()+1e-2).values()) {
            if (ion.equals(a)) return ion;
        }
        return a;
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
    			final int multiply = (multiplyStr != null && multiplyStr.length()>0) ? 
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
    	final Matcher matcher = getPattern().matcher(formula);
    	while (matcher.find()) {
    		final String elementName = matcher.group(1);
			final String elementAmount = matcher.group(2);
			final Element element = getByName(elementName);
			final int amount = elementAmount != null && elementAmount.length() > 0 ? 
					Integer.parseInt(elementAmount) : 1;
			visitor.visit(element, amount);
    	}
    }
}
