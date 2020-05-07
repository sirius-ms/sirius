package de.unijena.bioinf.fingerid.blast;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.ms.annotations.ResultAnnotation;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Result of a fingerblast job
 * We might add additional information in future like:
 * - used database
 * - used scoring method
 */
public class FingerblastResult implements ResultAnnotation {

    protected final List<Scored<FingerprintCandidate>> results;

    public FingerblastResult(List<Scored<FingerprintCandidate>> results) {
        this.results = results;
    }

    public List<Scored<FingerprintCandidate>> getResults() {
        return Collections.unmodifiableList(results);
    }

    public TopCSIScore getTopHitScore() {
        if (results == null || results.isEmpty())
            return null;
        return new TopCSIScore(results.get(0).getScore());
    }

    public FBCandidateFingerprints getCandidateFingerprints(){
        return new FBCandidateFingerprints(
                results.stream().map(SScored::getCandidate).map(FingerprintCandidate::getFingerprint)
                        .collect(Collectors.toList()));
    }

    public FBCandidates getCandidates(){
        return new FBCandidates(results.stream().map(s -> new Scored<>(new CompoundCandidate(s.getCandidate()),s.getScore())).collect(Collectors.toList()));
    }
}
