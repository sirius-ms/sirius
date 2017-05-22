package de.unijena.bioinf.sirius.gui.fingerid.fingerprints;

import de.unijena.bioinf.ChemistryBase.fp.MolecularProperty;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import org.jdesktop.beans.AbstractBean;

/**
 * TODO: this class is immutable. Does it make sense to have it extend AbstractBean?
 */
public class MolecularPropertyTableEntry extends AbstractBean implements Comparable<MolecularPropertyTableEntry> {

    protected final ProbabilityFingerprint underlyingFingerprint;
    protected final int absoluteIndex;

    public MolecularPropertyTableEntry(ProbabilityFingerprint underlyingFingerprint, int absoluteIndex) {
        this.underlyingFingerprint = underlyingFingerprint;
        this.absoluteIndex = absoluteIndex;
    }

    public double getProbability() {
        return underlyingFingerprint.getProbability(absoluteIndex);
    }

    public MolecularProperty getMolecularProperty() {
        return underlyingFingerprint.getFingerprintVersion().getMolecularProperty(absoluteIndex);
    }


    @Override
    public int compareTo(MolecularPropertyTableEntry o) {
        return absoluteIndex-o.absoluteIndex;
    }
}
