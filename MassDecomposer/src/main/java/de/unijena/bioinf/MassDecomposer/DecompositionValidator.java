package de.unijena.bioinf.MassDecomposer;

import java.util.List;

/**
 * Filter out all decompositions which can't be valid/meaningful
 */
public interface DecompositionValidator<T> {

    /**
     * @param compomere a decomposition. compomere[i] is corresponding to character c_{characterIds[i]} in the alphabet
     * @param characterIds an immutable(!!!) array with indizes of the used characters in the same order as in the compomere
     * @return true, if the decomposition should be contained in the decomposition output
     */
    public boolean validate(int[] compomere, int[] characterIds, Alphabet<T> alphabet);

}
