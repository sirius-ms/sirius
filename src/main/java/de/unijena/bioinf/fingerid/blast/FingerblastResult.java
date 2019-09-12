package de.unijena.bioinf.fingerid.blast;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.ms.annotations.ResultAnnotation;

import java.util.List;

/**
 * Result of a fingerblast job
 * We might add additional information in future like:
 * - used database
 * - used scoring method
 */
public class FingerblastResult implements ResultAnnotation {

    protected final List<Scored<CompoundCandidate>> results;

    public FingerblastResult(List<Scored<CompoundCandidate>> results) {
        this.results = results;
    }

    public List<Scored<CompoundCandidate>> getResults() {
        return results;
    }

    public TopFingerblastScore getTopHitScore() {
        if (results == null || results.isEmpty())
            return null;
        return new TopFingerblastScore(results.get(0).getScore());
    }
}
