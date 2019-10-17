package de.unijena.bioinf.retention;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.fingerid.fingerprints.FixedFingerprinter;
import org.openscience.cdk.interfaces.IAtomContainer;

public class SimpleCompound implements PredictableCompound {

    protected final InChI inchi;
    protected final IAtomContainer molecule;

    public SimpleCompound(InChI inchi, IAtomContainer molecule) {
        this.inchi = inchi;
        this.molecule = molecule;
    }

    public SimpleCompound(InChI inchi) {
        this.inchi = inchi;
        this.molecule = FixedFingerprinter.parseInchi(inchi.in2D,true);
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
