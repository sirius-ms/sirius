package de.unijena.bioinf.ChemistryBase.chem;

import de.unijena.bioinf.ChemistryBase.chem.utils.ElementMap;
import de.unijena.bioinf.ChemistryBase.chem.utils.FormulaVisitor;

import java.util.*;

/**
 * A chemical alphabet is a subset of the Periodic Table
 * - The PeriodicTable contains information about (probably) all possible characters in
 * the given alphabet (usually: metabolites -> atoms, peptides -> amino acids)
 * - The TableSelection is for internal use: It improves performance and memory efficient by bundling elements which
 * often occur together. It has no meaning for the application and should not be used by the user.
 * - in contrast, the chemical alphabet says which elements from the periodic table should be considered in an specific
 * application. Obviously, the chemical alphabet and table selection may be identical sometimes, but this is no requirement
 * <p/>
 * The chemical alphabet is implemented such that is works together well with the MassDecomposer. This means, that
 * elements are internally always ordered by mass. But there are other
 * use-cases for this class. In general: Whenever you want the user to submit a subset of the periodic table, use this class.
 */
public class ChemicalAlphabet {

    private final TableSelection selection;
    private final Element[] allowedElements;
    private final int[] orderOfElements;
    private final int maxLen;

    public static ChemicalAlphabet getExtendedAlphabet() {
        return new ChemicalAlphabet(PeriodicTable.getInstance().getAllByName("C", "H", "N", "O", "P", "S", "Cl", "Br", "I", "F", "Na", "Si"));
    }

    public static ChemicalAlphabet alphabetFor(Iterable<MolecularFormula> formulas) {
        final PeriodicTable table = PeriodicTable.getInstance();
        final BitSet set = new BitSet(table.numberOfElements() + 1);
        final ArrayList<Element> elements = new ArrayList<Element>();
        for (MolecularFormula f : formulas) {
            f.visit(new FormulaVisitor<Object>() {
                @Override
                public Object visit(Element element, int amount) {
                    if (amount > 0) set.set(element.getId());
                    return null;
                }
            });
        }
        int k = 0;
        while ((k = set.nextSetBit(k)) >= 0) {
            elements.add(table.get(k++));
        }
        return new ChemicalAlphabet(table.getSelectionFor(set), elements.toArray(new Element[elements.size()]));

    }

    public static ChemicalAlphabet alphabetFor(MolecularFormula... formulas) {
        return alphabetFor(Arrays.asList(formulas));
    }

    /**
     * Construct a chemical alphabet containing the CHNOPS alphabet.
     */
    public ChemicalAlphabet() {
        this(PeriodicTable.getInstance().getAllByName("C", "H", "N", "O", "P", "S"));
    }

    public ChemicalAlphabet(ChemicalAlphabet alpha) {
        this.allowedElements = alpha.allowedElements;
        this.selection = alpha.selection;
        this.orderOfElements = alpha.orderOfElements;
        this.maxLen = alpha.maxLen;
    }

    /**
     * Construct a chemical alphabet with the given selection and elements.
     * Remark: The selection is not important for the function of this class. But you can improve the performance
     * of your application if your algorithm uses only ONE table selection (or precisely, if all molecular formulas
     * which interact together are from the same table selection).
     *
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
        for (int i = 0; i < allowedElements.length; ++i) {
            orderOfElements[i] = selection.indexOf(allowedElements[i]);
            maxId = Math.max(maxId, orderOfElements[i]);
        }
        this.maxLen = maxId + 1;
    }

    /*
   construct a chemical alphabet with the given elements
    */
    public ChemicalAlphabet(Element... elements) {
        this(PeriodicTable.getInstance().getSelectionFor(elements), elements);
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
        for (int i = 0; i < compomer.length; ++i) {
            buffer[orderOfElements[i]] = (short) compomer[i];
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
        for (int i = 0; i < allowedElements.length; ++i)
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
        if (!selection.equals(that.selection)) return false;
        if (!Arrays.equals(allowedElements, that.allowedElements)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = selection.hashCode();
        result = 31 * result + Arrays.hashCode(allowedElements);
        return result;
    }


    @Override
    public String toString() {
        final short[] buffer = new short[maxLen];
        for (Element e : allowedElements) buffer[selection.indexOf(e)] = 1;
        return MolecularFormula.fromCompomer(selection, buffer).formatByHill();
    }
}
