package de.unijena.bioinf.ChemistryBase.fp;

import java.util.Iterator;

/**
 * Pairwise iterator
 */
public interface FPIter2 extends Iterable<FPIter2>, Iterator<FPIter2> {

    public FPIter2 clone();

    public double getLeftProbability();

    public double getRightProbability();

    public boolean isLeftSet();

    public boolean isRightSet();

    public int getIndex();

    public MolecularProperty getMolecularProperty();

}
