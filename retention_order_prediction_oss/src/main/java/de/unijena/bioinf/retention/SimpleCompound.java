package de.unijena.bioinf.retention;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.Smiles;
import de.unijena.bioinf.fingerid.fingerprints.FixedFingerprinter;
import org.openscience.cdk.interfaces.IAtomContainer;

public class SimpleCompound implements PredictableCompound {

    protected final InChI inchi;
    protected double retentionTime;
    protected final IAtomContainer molecule;

    public SimpleCompound(InChI inchi, IAtomContainer molecule, double retentionTime) {
        this.inchi = inchi;
        this.molecule = molecule;
        this.retentionTime = retentionTime;
    }

    public SimpleCompound(InChI inchi, double retentionTime) {
        this.inchi = inchi;
        this.retentionTime = retentionTime;
        this.molecule = FixedFingerprinter.parseInchi(inchi.in2D,true);
    }

    public double getRetentionTime() {
        return retentionTime;
    }

    @Override
    public InChI getInChI() {
        return inchi;
    }

    @Override
    public IAtomContainer getMolecule() {
        return molecule;
    }
}
