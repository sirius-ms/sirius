package de.unijena.bioinf.fingerid.fingerprints;

import de.unijena.bioinf.ChemistryBase.fp.*;
import org.jdesktop.beans.AbstractBean;

/**
 * TODO: this class is immutable. Does it make sense to have it extend AbstractBean?
 */
public class MolecularPropertyTableEntry extends AbstractBean implements Comparable<MolecularPropertyTableEntry> {

    protected final ProbabilityFingerprint underlyingFingerprint;
    protected final int absoluteIndex;
    protected final FingerprintVisualization visualization;
    protected final double fscore;

    public MolecularPropertyTableEntry(ProbabilityFingerprint underlyingFingerprint, FingerprintVisualization viz, double fscore, int absoluteIndex) {
        this.visualization = viz;
        this.underlyingFingerprint = underlyingFingerprint;
        this.absoluteIndex = absoluteIndex;
        this.fscore = fscore;
    }

    public double getProbability() {
        return underlyingFingerprint.getProbability(absoluteIndex);
    }

    public MolecularProperty getMolecularProperty() {
        return underlyingFingerprint.getFingerprintVersion().getMolecularProperty(absoluteIndex);
    }

    public double getFScore() {
        return fscore;
    }

    public int getMatchSize() {
        if (visualization==null) return 0;
        return visualization.numberOfMatchesAtoms;
    }

    public int getMatchSizeDescription() {
        return Math.max(0, getMatchSize());
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

    @Override
    public String toString() {
        return  absoluteIndex + ": " + getMolecularProperty().toString();
    }
}
