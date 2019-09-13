package de.unijena.bioinf.projectspace.fingerid;

import de.unijena.bioinf.ChemistryBase.fp.ClassyFireFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion;
import de.unijena.bioinf.canopus.Canopus;
import de.unijena.bioinf.projectspace.ProjectSpaceProperty;
import org.jetbrains.annotations.NotNull;

public class CanopusClientData implements ProjectSpaceProperty {

    protected final MaskedFingerprintVersion maskedFingerprintVersion;
    protected final ClassyFireFingerprintVersion classyFireFingerprintVersion;

    public CanopusClientData(@NotNull Canopus canopus) {
        this(canopus.getCanopusMask());
    }

    public CanopusClientData(@NotNull MaskedFingerprintVersion maskedFingerprintVersion) {
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
