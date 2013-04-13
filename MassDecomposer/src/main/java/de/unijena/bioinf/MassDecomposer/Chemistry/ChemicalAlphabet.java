package de.unijena.bioinf.MassDecomposer.Chemistry;

import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.TableSelection;
import de.unijena.bioinf.ChemistryBase.chem.utils.ElementMap;
import de.unijena.bioinf.MassDecomposer.ValencyAlphabet;

import java.util.*;

public class ChemicalAlphabet implements ValencyAlphabet<Element>{
	
	private final TableSelection selection;
	private final Element[] allowedElements;
	private final int[] orderOfElements;
	private final int maxLen;

    public ChemicalAlphabet() {
        this(PeriodicTable.getInstance().getAllByName("C", "H", "N", "O", "P", "S"));
    }

	public ChemicalAlphabet(TableSelection selection, Element...elements) {
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
	}

    public List<Element> getElements() {
        return Collections.unmodifiableList(Arrays.asList(allowedElements));
    }
	
	public ChemicalAlphabet(Element...elements) {
		this(PeriodicTable.getInstance().getSelectionFor(elements), elements);
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

	@Override
	public int size() {
		return allowedElements.length;
	}

	@Override
	public double weightOf(int i) {
		return allowedElements[i].getMass();
	}

	@Override
	public Element get(int i) {
		return allowedElements[i];
	}

	@Override
	public int indexOf(Element character) {
		for (int i=0; i < allowedElements.length; ++i)
			if (allowedElements[i].equals(character)) return i;
		return -1;
	}

	@Override
	public <S> Map<Element, S> toMap() {
		return new ElementMap<S>(selection);
	}

	@Override
	public int valenceOf(int i) {
		return allowedElements[i].getValence();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(allowedElements);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ChemicalAlphabet other = (ChemicalAlphabet) obj;
		if (!Arrays.equals(allowedElements, other.allowedElements))
			return false;
		if (!(selection == other.selection)) return false;
		return true;
	}
	
	

}
