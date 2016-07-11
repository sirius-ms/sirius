package de.unijena.bioinf.ConfidenceScore.confidenceScore;

import de.unijena.bioinf.ChemistryBase.chem.CompoundWithAbstractFP;
import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;


import java.util.Comparator;

/**
 * Created by Marcus Ludwig on 09.03.16.
 */
public class ScoredCandidate extends CompoundWithAbstractFP<Fingerprint> {
    public final double score;

    public ScoredCandidate(InChI inChI, Fingerprint fingerprint, double score) {
        super(inChI, fingerprint);
        this.score = score;
    }

    public ScoredCandidate(CompoundWithAbstractFP<Fingerprint> candidate, double score) {
        super(candidate.getInchi(), candidate.getFingerprint());
        this.score = score;
    }

    public static final class MinBestComparator implements Comparator<ScoredCandidate> {
        @Override
        public int compare(ScoredCandidate o1, ScoredCandidate o2) {
            return Double.compare(o1.score, o2.score);
        }
    }

    public static final class MaxBestComparator implements Comparator<ScoredCandidate>{
        @Override
        public int compare(ScoredCandidate o1, ScoredCandidate o2) {
            return Double.compare(o2.score, o1.score);
        }
    }
}
