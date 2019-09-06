package de.unijena.bioinf.projectspace.fingerid;

import de.unijena.bioinf.ChemistryBase.fp.ClassyFireFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion;
import de.unijena.bioinf.projectspace.ProjectSpaceProperty;

public class CanopusClientData implements ProjectSpaceProperty {

    protected final MaskedFingerprintVersion maskedFingerprintVersion;
    protected final ClassyFireFingerprintVersion classyFireFingerprintVersion;

    public CanopusClientData(MaskedFingerprintVersion maskedFingerprintVersion) {
        this.maskedFingerprintVersion = maskedFingerprintVersion;
        this.classyFireFingerprintVersion = (ClassyFireFingerprintVersion)maskedFingerprintVersion.getMaskedFingerprintVersion();
    }

    public MaskedFingerprintVersion getFingerprintVersion() {
        return maskedFingerprintVersion;
    }

    public ClassyFireFingerprintVersion getClassyFireFingerprintVersion() {
        return classyFireFingerprintVersion;
    }
}
