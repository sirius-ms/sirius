package de.unijena.bioinf.ChemistryBase.fp;

import java.util.Iterator;

/**
 * Pairwise iterator
 */
public interface FPIter2 extends Iterable<FPIter2>, Iterator<FPIter2> {

    FPIter2 clone();

    double getLeftProbability();

    double getRightProbability();

    boolean isLeftSet();

    boolean isRightSet();

    int getIndex();

    MolecularProperty getMolecularProperty();

}
