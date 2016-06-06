package de.unijena.bioinf.ChemistryBase.ms.fp;

import de.unijena.bioinf.ChemistryBase.chem.InChI;

public class CompoundWithFingerprint {

    private final InChI inchi;
    private final Fingerprint fingerprint;

    public CompoundWithFingerprint(InChI inchi, Fingerprint fingerprint) {
        this.inchi = inchi;
        this.fingerprint = fingerprint;
    }

    public InChI getInchi() {
        return inchi;
    }

    public Fingerprint getFingerprint() {
        return fingerprint;
    }
}
