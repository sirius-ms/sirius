package de.unijena.bioinf.fingerid.fingerprints;

import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import org.openscience.cdk.fingerprint.SubstructureFingerprinter;

import static de.unijena.bioinf.fingerid.fingerprints.BiosmartsFingerprinter.getSMARTSPATTERN;

/**
 * This fingerprint contains SMARTS pattern used in ClassyFire.
 * We only select SMARTS strings which occur at least 50 times in training data and have an F1 above 0.25.
 */
public class ClassyFireSmartsFingerprint extends SubstructureFingerprinter {

    private static String[] SMARTS_PATTERNS = getSMARTSPATTERN(CdkFingerprintVersion.USED_FINGERPRINTS.CLASSYFIRE_SMARTS);

    public ClassyFireSmartsFingerprint() {
        super(SMARTS_PATTERNS);
    }
}
