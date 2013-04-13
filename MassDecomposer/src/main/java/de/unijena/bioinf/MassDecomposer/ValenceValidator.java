package de.unijena.bioinf.MassDecomposer;

import java.util.List;

/**
 * A decomposition validator. It allows only decomposition which form a fully connected graph using a valency alphabet
 */
public class ValenceValidator<T> implements DecompositionValidator<T> {

    private final int minValence;

    public ValenceValidator(double minValence) {
        this.minValence = (int)(2*minValence);
    }

    public ValenceValidator() {
        this(-0.5d);
    }

    public int getMinValence() {
        return minValence;
    }

    @Override
    public boolean validate(int[] compomere, int[] characterIds, Alphabet<T> alphabet) {
        if (!(alphabet instanceof ValencyAlphabet))
            throw new RuntimeException("Validator can only be used for valency alphabets");
        final ValencyAlphabet<T> characters = (ValencyAlphabet<T>)alphabet;
        // rdbe * 2 = number of unsaturated bounds in molecular graph.
        int rdbe = 2;
        for (int i=0; i < compomere.length; ++i) {
            final int valence = characters.valenceOf(characterIds[i]);
            rdbe += (valence-2) * compomere[i];
        }
        return rdbe >= minValence;
    }
}
