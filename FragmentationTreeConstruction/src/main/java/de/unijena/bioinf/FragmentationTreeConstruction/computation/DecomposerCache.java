package de.unijena.bioinf.FragmentationTreeConstruction.computation;

import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.MassDecomposer.Chemistry.MassToFormulaDecomposer;

/**
 * caches decomposer and corresponding alphabet. If a dataset contains a small number of different alphabets,
 * the cache creates for each such alphabet an own decomposer instead of creating a new one each time the alphabet changes.
 */
class DecomposerCache {

    private ChemicalAlphabet alphabets[];
    private MassToFormulaDecomposer decomposers[];
    private int useCounter[];
    private int size;
    private double precision;

    public DecomposerCache(double precision, int size) {
        this.alphabets = new ChemicalAlphabet[size];
        this.decomposers = new MassToFormulaDecomposer[size];
        this.useCounter = new int[size];
        this.size = 0;
        this.precision = precision;
    }

    public MassToFormulaDecomposer getDecomposer(ChemicalAlphabet alphabet) {
        for (int i=0; i < size; ++i) {
            if (alphabets[i].equals(alphabet)) {
                ++useCounter[i];
                return decomposers[i];
            }
        }
        if (size < alphabets.length) {
            decomposers[size] = new MassToFormulaDecomposer(precision, alphabet);
            alphabets[size] = alphabet;
            return decomposers[size++];
        } else {
            int mindex = 0;
            for (int i=1; i < useCounter.length; ++i)
                if (useCounter[i] < useCounter[mindex]) mindex = i;
            decomposers[mindex] =  new MassToFormulaDecomposer(precision, alphabet);
            alphabets[mindex] = alphabet;
            return decomposers[mindex];
        }
    }

    public DecomposerCache() {
        this(1e-4, 5);
    }

}
