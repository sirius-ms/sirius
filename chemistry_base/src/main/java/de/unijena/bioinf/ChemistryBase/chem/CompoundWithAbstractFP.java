package de.unijena.bioinf.ChemistryBase.chem;

import de.unijena.bioinf.ChemistryBase.fp.AbstractFingerprint;

/**
 * Created by Marcus Ludwig on 04.07.16.
 */
public class CompoundWithAbstractFP<T extends AbstractFingerprint> {

    private final InChI inchi;
    private final T fingerprint;

    public CompoundWithAbstractFP(InChI inchi, T fingerprint) {
        this.inchi = inchi;
        this.fingerprint = fingerprint;
    }

    public InChI getInchi() {
        return inchi;
    }

    public T getFingerprint() {
        return fingerprint;
    }
}
