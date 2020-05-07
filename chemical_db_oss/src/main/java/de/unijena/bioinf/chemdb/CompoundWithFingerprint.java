package de.unijena.bioinf.chemdb;

import de.unijena.bioinf.ChemistryBase.chem.CompoundWithAbstractFP;
import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;

public class CompoundWithFingerprint extends CompoundWithAbstractFP<Fingerprint> {

    // optionally: annotations
    private CompoundCandidate annotated;

    public CompoundWithFingerprint(InChI inchi, Fingerprint fingerprint) {
        super(inchi, fingerprint);
    }

    public CompoundCandidate getAnnotated() {
        return annotated;
    }

    public void setAnnotated(CompoundCandidate annotated) {
        this.annotated = annotated;
    }

}
