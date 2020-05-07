package de.unijena.bioinf.fingerid.blast;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.ms.annotations.ResultAnnotation;

import java.util.Collections;
import java.util.List;

/**
 * Result of a fingerblast job
 * We might add additional information in future like:
 * - used database
 * - used scoring method
 */
public class FBCandidates implements ResultAnnotation {

    protected final List<Scored<CompoundCandidate>> results;

    public FBCandidates(List<Scored<CompoundCandidate>> results) {
        this.results = results;
    }

    public List<Scored<CompoundCandidate>> getResults() {
        return Collections.unmodifiableList(results);
    }

    public TopCSIScore getTopHitScore() {
        if (results == null || results.isEmpty())
            return null;
        return new TopCSIScore(results.get(0).getScore());
    }
}
