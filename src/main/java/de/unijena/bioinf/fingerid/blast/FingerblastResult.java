package de.unijena.bioinf.fingerid.blast;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.ms.annotations.ResultAnnotation;

import java.util.List;

/**
 * Result of a fingerblast job
 * We might add additional information in future like:
 * - used database
 * - used scoring method
 */
public class FingerblastResult implements ResultAnnotation {

    protected final List<Scored<? extends CompoundCandidate>> results;

    public FingerblastResult(List<Scored<? extends CompoundCandidate>> results) {
        this.results = results;
    }

    public List<Scored<? extends CompoundCandidate>> getResults() {
        return results;
    }
}
