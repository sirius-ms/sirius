package de.unijena.bioinf.retention;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import org.openscience.cdk.interfaces.IAtomContainer;

public class SimpleCompound implements PredictableCompound {

    protected final InChI inchi;
    protected final String smiles;
    protected double retentionTime;
    protected final IAtomContainer molecule;

    public SimpleCompound(InChI inchi, String smiles, IAtomContainer molecule, double retentionTime) {
        this.inchi = inchi;
        this.molecule = molecule;
        this.smiles = smiles;
        this.retentionTime = retentionTime;
    }

    public double getRetentionTime() {
        return retentionTime;
    }

    @Override
    public InChI getInChI() {
        return inchi;
    }

    public String getSmiles() {
        return smiles;
    }

    @Override
    public IAtomContainer getMolecule() {
        return molecule;
    }
}
