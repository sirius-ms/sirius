package de.unijena.bioinf.ChemistryBase.ms.ft;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;

public interface FTLoss<F extends FTFragment> {

    public F getHead();
    public F getTail();
    public MolecularFormula getFormula();



}
