package de.unijena.bioinf.ChemistryBase.chem;

import de.unijena.bioinf.ChemistryBase.chem.utils.FormulaVisitor;

import java.util.*;

/**
 * Basic class for molecular formulas. All algorithm should use this abstract class instead of
 * it's concrete implementations.
 *
 * A molecular formula describes a sum formula, which is a multiset or compomere containing
 * elements and their amount.
 */
public abstract class MolecularFormula implements Cloneable, Iterable<Element> {
		
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
    
    /**
     * returns a new immutable molecular formula of the given formula
     */
    public static MolecularFormula from(MolecularFormula formula) {
    	return new ImmutableMolecularFormula(formula);
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
    	for (int i=0; i < buffer.length; ++i) {
    		final int x = compomer[i];
    		if (x < Short.MIN_VALUE || x > Short.MAX_VALUE) {
    			throw new IllegalArgumentException();
    		}
    		buffer[i] = (short)x;
    	}
    	return fromCompomer(selection, buffer);
    }
    
    /**
     * creates a new molecular formula from a given string. This should be the preferred way
     * to create molecular formulas. Typical strings which are recognized are
     * "CH4", "NOH(CH2)4COOH", "CH4(C(H)2)8CH4", ""
     * Modifiers as ions or isotopes are not recognized. For example "Fe+3" or "13C" are no valid
     * molecular formulas.
     */
    public static MolecularFormula parse(String text) {
    	return parse(text, PeriodicTable.getInstance());
    }
    
    static MolecularFormula parse(String text, PeriodicTable pt) {
    	int labelindex = Arrays.binarySearch(Static.MOLECULE_NAME, text);
    	if (labelindex > -1) {
    		text = Static.MOLECULE_FORMULA[labelindex];
    	}
    	
    	final ArrayList<Pair> pairs = new ArrayList<Pair>();
    	pt.parse(text, new FormulaVisitor<Object>() {
			@Override
			public Object visit(Element element, int amount) {
				pairs.add(new Pair(element, amount));
				return null;
			}
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
    
    private static class Pair {
    	private final Element element;
    	private final int amount;
    	private Pair(Element element, int amount) {
    		this.element = element;
    		this.amount = amount;
    	}
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
        for (int i=0; i < amounts.length; ++i) {
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
    	for (int i=0; i < buffer.length; ++i) {
    		if (buffer[i] > 0) elements.add(selection.get(i));
    	}
    	return elements;
    }
    
    /**
     * build an array of elements this formula contains. Each call of this method builds a new array.
     */
    public Element[] elementArray() {
    	final TableSelection selection = getTableSelection();
    	final ArrayList<Element> elements = new ArrayList<Element>(selection.size());
    	final short[] buffer = buffer();
    	for (int i=0; i < buffer.length; ++i) {
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
     * The ring-double-bond-equation is the maximal number of free electron/valence-pairs in any molecular
     * graph of this formula. It is the halve of the {{@link #doubledRDBE()}.
     * @return
     */
    public float rdbe() {
        return doubledRDBE()/2f;
    }
    
    /**
     * the doubled ring-double-bond-equation is the maximal number of 
     * not-satisfied valences in the molecular graph.
     * 
     * 
     * For example: In C2H6 each C atom has 4 valences, each H atom has 1 valence. We do not know
     * the molecular structure, but we know that the sum of all valences is 14, while each but one 
     * atom consumes at least two valences (for a bond to another atom) such that the graph is fully connected. Therefore
     * the number of not satisfied valence-pairs is 14-(7*2) = 0.
     * 
     * If the number is odd, then the molecule is charged because there is one free electron. If the number
     * is negative, then there are free atoms which cannot be connected to the graph. Usually, we forbid
     * such molecules in our application.
     */
    public int doubledRDBE() {
        int rdbe = 2;
        final short[] amounts = buffer();
        final TableSelection selection = getTableSelection();
        for (int i=0; i < amounts.length; ++i) {
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
        for (int i=0; i < amounts.length; ++i) {
            sum += amounts[i];
        }
        return sum;
    }

    /**
     * A formula is charged if its rdbe value is not a whole-number (for example if it is 0.5)
     * or if its doubled-rdbe is an odd number.
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
        return (float)hydrogen / (carbon == 0 ? 0.8f : (float)carbon);
    }

    /**
     * @return the ratio of the non-hydrogen and non-carbon atoms to the number carbon atoms
     */
    public float hetero2CarbonRatio() {
        final int carbon = numberOfCarbons();
        final int hetero = atomCount() - carbon - numberOfHydrogens();
        return (float)hetero / (carbon == 0 ? 0.8f : (float)carbon);
    }

    /**
     * @return the ratio of the non-hydrogen and non-oxygen to the number of oxygens
     */
    public float hetero2OxygenRatio() {
        final int oxygen = numberOfOxygens();
        final int hetero = atomCount() - oxygen - numberOfHydrogens();
        return (float)hetero / (oxygen == 0 ? 0.8f : (float)oxygen);
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
        if (buffer.length-offset < amounts.length) throw new IndexOutOfBoundsException("buffer is to small");
        System.arraycopy(amounts, 0, buffer, offset, amounts.length);
    }

    /**
     * a formula is subtractable from another formula, if for each element in the
     * periodic table the amount of atoms of this element is greater or equal to the other formula.
     */
    public boolean isSubtractable(MolecularFormula other) {
    	final short[] amounts = buffer();
        final TableSelection selection = getTableSelection();
        if (selection != other.getTableSelection()) return isSubtractableInc(other);
        final short[] otheram = other.buffer();
        if (otheram.length > amounts.length) {
            int i=otheram.length-1;
            while (otheram[i] == 0) --i;
            if (i >= amounts.length) return false;
        }
        final int n = Math.min(otheram.length, amounts.length);
        for (int i=0; i < n; ++i) {
            if (amounts[i] < otheram[i]) return false;
        }
        return true;
    }

    private boolean isSubtractableInc(MolecularFormula other) {
        final short[] amounts = buffer();
        final short[] amounts2 = other.buffer();
        final TableSelection sel = getTableSelection();
        final TableSelection sel2 = other.getTableSelection();
        for (int i=0; i < amounts2.length; ++i ) {
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
    	for (int i=0; i < amounts.length; ++i) {
    		if (amounts[i] > 0) return false;
    	}
    	return true;
    }
    
    public boolean isAllPositiveOrZero() {
    	final short[] amounts = buffer();
    	for (int i=0; i < amounts.length; ++i) {
    		if (amounts[i] < 0) return false;
    	}
    	return true;
    }
    
    /**
     * returns a new formula containing the atoms of both formulas
     */
    public MolecularFormula add(MolecularFormula other) {
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
            for (int i=0; i < amounts.length; ++i) {
                nrs[newSelection.indexOf(selection.get(i))] = amounts[i];
            }
            for (int i=0; i < otherAmounts.length; ++i) {
                nrs[newSelection.indexOf(otherSelection.get(i))] += otherAmounts[i];
            }
        } else {
            newSelection = selection;
            if (amounts.length < otherAmounts.length) return other.add(this);
            nrs = Arrays.copyOf(amounts, amounts.length);
            for (int i=0; i < otherAmounts.length; ++i) {
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
     * the number in other. If both formulas are {{@link #isSubtractable(MolecularFormula)}},
     * the result is a subformula which appears after other is cut of from self.
     */
    public MolecularFormula subtract(MolecularFormula other) {
    	final short[] amounts = buffer();
        final TableSelection selection = getTableSelection();
        final short[] otherAmounts = other.buffer();
        final TableSelection otherSelection = other.getTableSelection();
        if (selection != otherSelection) return add(other.negate()); // TODO: improve performance
        if (amounts.length < otherAmounts.length) return other.subtract(this);
        final short[] nrs = Arrays.copyOf(amounts, amounts.length);
        for (int i=0; i < otherAmounts.length; ++i) {
            nrs[i] -= otherAmounts[i];
        }
        return new ImmutableMolecularFormula(selection, nrs);
    }

    /**
     * returns a new molecular formula in which the amount of each element 
     * is multiplied with the given scalar.
     */
    public MolecularFormula multiply(int scalar) {
        final short[] nrs = Arrays.copyOf(buffer(), buffer().length);
        for (int i=0; i < nrs.length; ++i) nrs[i] *= scalar;
        return new ImmutableMolecularFormula(getTableSelection(), nrs);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (o instanceof MolecularFormula) {
            boolean aus = equals((MolecularFormula)o);
            return aus;

        };
        return false;
    }

    @Override
    public int hashCode() {
        int hash = (int)getMass();
        final short[] amounts = buffer();
        final TableSelection selection = getTableSelection();
        for (int i=0; i < amounts.length; ++i) {
            if (amounts[i] != 0) hash ^= (amounts[i]<<(selection.get(i).getId()%16)); // Bits nicht gleichmäßig verteilt. Überarbeiten!
        }
        return hash;
    }

    public boolean equals(MolecularFormula formula) {
        if (formula == this) return true;
        if (formula == null) return false;
        final short[] amounts = buffer();
        final TableSelection selection = getTableSelection();
        final short[] otherAmounts = formula.buffer();
        final TableSelection otherSelection = formula.getTableSelection();
        if ((long)getMass() != (long)formula.getMass()) return false;
        if (selection == otherSelection) {
            return Arrays.equals(amounts, otherAmounts);
        } else {
            for (int i=0; i < amounts.length; ++i) {
                if (amounts[i] != 0 && amounts[i] != formula.numberOf(selection.get(i))) return false;
            }
            for (int i=0; i < otherAmounts.length; ++i) {
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
     */
    public String formatByHill() {
    	final short[] amounts = buffer();
        final TableSelection selection = getTableSelection();
    	final StringBuilder buffer = new StringBuilder(3*amounts.length);
    	final Element[] elements = new Element[Math.max(0, amounts.length - 2)];
    	int k=0;
    	for (int i=0; i < amounts.length; ++i) {
    		if (i == selection.hydrogenIndex() || i == selection.carbonIndex()) continue;
    		if (amounts[i] != 0) {
    			elements[k++] = selection.get(i);
    		}
    	}
    	Arrays.sort(elements, 0, k, new Comparator<Element>(){
			@Override
			public int compare(Element o1, Element o2) {
				return o1.getSymbol().compareTo(o2.getSymbol());
			}
    	});
    	final int h = numberOfHydrogens();
    	final int c = numberOfCarbons();
    	if (c != 0) {
    		if (c < 0) buffer.append("-");
    		buffer.append(selection.get(selection.carbonIndex()).getSymbol());
    	}
    	if (Math.abs(c) > 1) {
    		buffer.append(c);
    	}
    	if (h != 0) {
    		if (h < 0) buffer.append("-");
    		buffer.append(selection.get(selection.hydrogenIndex()).getSymbol());
    	}
    	if (Math.abs(h) > 1) {
    		buffer.append(h);
    	}
    	for (int i=0; i < k; ++i) {
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
    	for (int i=0; i < buffer.length; ++i) {
    		visitor.visit(sel.get(i), buffer[i]);
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
				for (;index<buffer.length;index++) {
					if (buffer[index]>0)
						return true;
				}
				return false;
			}

			@Override
			public Element next() {
				for (;index<buffer.length;index++) {
					if (buffer[index]>0) 
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
	
}
