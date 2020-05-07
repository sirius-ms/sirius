package de.unijena.bioinf.retention;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import org.openscience.cdk.interfaces.IAtomContainer;

public interface PredictableCompound {
    public InChI getInChI();
    public IAtomContainer getMolecule();
}
