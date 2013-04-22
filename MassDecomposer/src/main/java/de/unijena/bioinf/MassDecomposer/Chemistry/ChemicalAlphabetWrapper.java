package de.unijena.bioinf.MassDecomposer.Chemistry;


import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.MassDecomposer.ValencyAlphabet;

import java.util.Map;

public class ChemicalAlphabetWrapper implements ValencyAlphabet<Element> {

    private final ChemicalAlphabet chemicalAlphabet;

    public ChemicalAlphabetWrapper(ChemicalAlphabet chemicalAlphabet) {
        this.chemicalAlphabet = chemicalAlphabet;
    }

    @Override
    public int valenceOf(int i) {
        return chemicalAlphabet.get(i).getValence();
    }

    @Override
    public int size() {
        return chemicalAlphabet.size();
    }

    @Override
    public double weightOf(int i) {
        return chemicalAlphabet.get(i).getMass();
    }

    @Override
    public Element get(int i) {
        return chemicalAlphabet.get(i);
    }

    @Override
    public int indexOf(Element character) {
        return chemicalAlphabet.indexOf(character);
    }

    @Override
    public <S> Map<Element, S> toMap() {
        return chemicalAlphabet.toMap();
    }

    public ChemicalAlphabet getAlphabet() {
        return chemicalAlphabet;
    }
}
