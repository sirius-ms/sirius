package de.unijena.bioinf.sirius.gui.fingerid.fingerprints;

import de.unijena.bioinf.ChemistryBase.fp.*;
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

    public CdkFingerprintVersion.USED_FINGERPRINTS getFingerprintType() {
        final CdkFingerprintVersion v;
        FingerprintVersion vv = underlyingFingerprint.getFingerprintVersion();
        if (vv instanceof MaskedFingerprintVersion) vv = ((MaskedFingerprintVersion) vv).getMaskedFingerprintVersion();
        if (vv instanceof CdkFingerprintVersion) v = (CdkFingerprintVersion)vv;
        else throw new RuntimeException("Can only deal with CDK fingerprints");
        return v.getFingerprintTypeFor(absoluteIndex);
    }

    public String getFingerprintTypeName() {
        switch (getFingerprintType()) {
            case ECFP: return "ECFP";
            case KLEKOTA_ROTH: return "Klekota Roth";
            case MACCS: return "MACCS";
            case OPENBABEL: return "Open Babel FP4";
            case PUBCHEM: return "PubChem";
            case SUBSTRUCTURE: return "CDK Substructure";
            default:return null;
        }
    }


    @Override
    public int compareTo(MolecularPropertyTableEntry o) {
        return absoluteIndex-o.absoluteIndex;
    }
}
