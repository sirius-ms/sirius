package de.unijena.bioinf.projectspace.fingerid;

import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.projectspace.ProjectSpaceProperty;

/**
 * Has to be set by CLI when first running CSI via WebAPI call
 */
public class CSIClientData implements ProjectSpaceProperty  {

    protected MaskedFingerprintVersion fingerprintVersion;
    protected CdkFingerprintVersion cdkFingerprintVersion;
    protected PredictionPerformance[] performances;

    public CSIClientData(MaskedFingerprintVersion fingerprintVersion, PredictionPerformance[] performances) {
        this.fingerprintVersion = fingerprintVersion;
        this.cdkFingerprintVersion = (CdkFingerprintVersion) fingerprintVersion.getMaskedFingerprintVersion();
        this.performances = performances;
    }

    public MaskedFingerprintVersion getFingerprintVersion() {
        return fingerprintVersion;
    }

    public CdkFingerprintVersion getCdkFingerprintVersion() {
        return cdkFingerprintVersion;
    }

    public PredictionPerformance[] getPerformances() {
        return performances;
    }
}
