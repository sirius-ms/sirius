package de.unijena.bioinf.ChemistryBase.chem;

import de.unijena.bioinf.ChemistryBase.chem.utils.ElementMap;
import org.apache.commons.collections.primitives.ArrayIntList;
import org.apache.commons.collections.primitives.adapters.IntListList;

import java.util.*;

/**
 * A chemical alphabet is a subset of the Periodic Table
 * - The PeriodicTable contains information about (probably) all possible characters in
 * the given alphabet (usually: metabolites -> atoms, peptides -> amino acids)
 * - The TableSelection is for internal use: It improves performance and memory efficient by bundling elements which
 *  often occur together. It has no meaning for the application and should not be used by the user.
 * - in contrast, the chemical alphabet said which elements from the periodic table should be considered in an specific
 *   application. Obviously, the chemical alphabet and table selection may be identical sometimes, but this is no requirement
 *
 * In addition to the selection of elements, the chemical alphabet contains also the number how much from which element
 * should be expected.
 * The chemical alphabet is implemented such that is works together well with the MassDecomposer. But there are other
 * use-cases for this class.
 *
 *
 */
public class ChemicalAlphabet {
	
	private final TableSelection selection;
	private final Element[] allowedElements;
    private final int[] upperBoundOfElements;
	private final int[] orderOfElements;
	private final int maxLen;

    /**
     * A factory method which provides a nice way to instantiate chemical alphabets, but which is not type-safe. So
     * use it carefully and only if your alphabet is a "literal" or constant.
     *
     * <pre>ChemicalAlphabet.create(myTableSelection, "C", "H", "N", "O", "P", 5, "S", 3, "F", 6);</pre>
     * <pre>ChemicalAlphabet.create("C",20, "H", "Fe", 3);</pre>
     *
     * This will compile but throw a runtime exception
     * <pre>ChemicalAlphabet.create(321.02, new int[0], "Bla");</pre>
     *
     * @param varargs parameters in the order: [selection], {elementName, [elementUpperbound]}
     * @return chemical alphabet
     */
    public static ChemicalAlphabet create(Object... varargs) {
        if (varargs.length == 0) return new ChemicalAlphabet();
        final PeriodicTable t = PeriodicTable.getInstance();
        int i=0;
        final TableSelection sel = (varargs[i] instanceof TableSelection) ? (TableSelection)varargs[i++] : null;
        final ArrayList<Element> elements = new ArrayList<Element>();
        final ArrayIntList bounds = new ArrayIntList();
        for (; i < varargs.length; ++i) {
            if (varargs[i] instanceof Element) {
                elements.add((Element)varargs[i]);
                bounds.add(Integer.MIN_VALUE);
            } else if ((varargs[i] instanceof String)) {
                elements.add(t.getByName((String)varargs[i]));
                bounds.add(Integer.MIN_VALUE);
            } else if (varargs[i] instanceof Integer) {
                final int k=bounds.size()-1;
                if (k < 0 || bounds.get(k) != Integer.MIN_VALUE)
                    throw new IllegalArgumentException("Illegal format of parameters. Allowed is: [tableselection], {element, [number]}");
                bounds.set(k, (Integer)varargs[i]);
            }
        }
        final Element[] elems = elements.toArray(new Element[elements.size()]);
        final int[] upperbounds = bounds.toArray();
        final TableSelection s = (sel == null) ? t.getSelectionFor(elems) : sel;
        return new ChemicalAlphabet(s, elems, upperbounds);
    }

    /**
     * Construct a chemical alphabet containing the CHNOPS alphabet.
     * TODO: This method fails if the periodic table contains no CHNOPS: We should consider to change this sometimes.
     */
    public ChemicalAlphabet() {
        this(PeriodicTable.getInstance().getAllByName("C", "H", "N", "O", "P", "S"));
    }

    public ChemicalAlphabet(ChemicalAlphabet alpha) {
        this.allowedElements = alpha.allowedElements;
        this.upperBoundOfElements = alpha.upperBoundOfElements.clone();
        this.selection = alpha.selection;
        this.orderOfElements = alpha.orderOfElements;
        this.maxLen = alpha.maxLen;
    }

    /**
     * Construct a chemical alphabet with the given selection and elements.
     * Remark: The selection is not important for the function of this class. But you can improve the performance
     * of your application if your algorithm uses only ONE table selection (or precisely, if all molecular formulas
     * which interact together are from the same table selection).
     * @param selection
     * @param elements
     * @throws NoSuchElementException if the selection does not contain all elements of this chemical alphabet
     */
	public ChemicalAlphabet(TableSelection selection, Element... elements) {
		final SortedSet<Element> elems = new TreeSet<Element>(Arrays.asList(elements));
        this.selection = selection;
		this.allowedElements = elems.toArray(new Element[elems.size()]);
		this.orderOfElements = new int[allowedElements.length];
		int maxId = 0;
		for (int i=0; i < allowedElements.length; ++i) {
			orderOfElements[i] = selection.indexOf(allowedElements[i]);
			maxId = Math.max(maxId, orderOfElements[i]);
		}
		this.maxLen = maxId+1;
        this.upperBoundOfElements = new int[elements.length];
	}

    /*
   construct a chemical alphabet with the given elements
    */
    public ChemicalAlphabet(Element... elements) {
        this(PeriodicTable.getInstance().getSelectionFor(elements), elements);
    }

    /*
   construct a chemical alphabet with the given elements and upperbounds
    */
    public ChemicalAlphabet(TableSelection selection, Element[] elements, int[] upperbounds) {
        this(selection, elements);
        System.arraycopy(upperbounds, 0, upperBoundOfElements, 0, upperBoundOfElements.length);
    }

    public int getUpperboundOf(Element e) {
        return upperBoundOfElements[selection.indexOf(e)];
    }

    public void setUpperboundOf(Element e, int upperbound) {
        upperBoundOfElements[selection.indexOf(e)] = upperbound;
    }

    /**
     * @return an unmodifiable list of the elements in this collection
     */
    public List<Element> getElements() {
        return Collections.unmodifiableList(Arrays.asList(allowedElements));
    }
	
	public TableSelection getTableSelection() {
		return selection;
	}

    public MolecularFormula decompositionToFormula(int[] compomer) {
        final short[] buffer = new short[maxLen];
        for (int i=0; i < compomer.length; ++i) {
            buffer[orderOfElements[i]] = (short)compomer[i];
        }
        return MolecularFormula.fromCompomer(selection, buffer);
    }

	public int size() {
		return allowedElements.length;
	}

	public double weightOf(int i) {
		return allowedElements[i].getMass();
	}

	public Element get(int i) {
		return allowedElements[i];
	}

	public int indexOf(Element character) {
		for (int i=0; i < allowedElements.length; ++i)
			if (allowedElements[i].equals(character)) return i;
		return -1;
	}

	public <S> Map<Element, S> toMap() {
		return new ElementMap<S>(selection);
	}

	public int valenceOf(int i) {
		return allowedElements[i].getValence();
	}

    @Override
    public ChemicalAlphabet clone() {
        return new ChemicalAlphabet(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ChemicalAlphabet that = (ChemicalAlphabet) o;

        if (!Arrays.equals(allowedElements, that.allowedElements)) return false;
        if (!selection.equals(that.selection)) return false;
        if (!Arrays.equals(upperBoundOfElements, that.upperBoundOfElements)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = selection.hashCode();
        result = 31 * result + Arrays.hashCode(allowedElements);
        result = 31 * result + Arrays.hashCode(upperBoundOfElements);
        return result;
    }
}
