package de.unijena.bioinf.ConfidenceScore.confidenceScore;

import de.unijena.bioinf.fingerid.Candidate;

import java.util.Comparator;

/**
 * Created by Marcus Ludwig on 09.03.16.
 */
public class ScoredCandidate extends Candidate {
    public final double score;
    public final double[] additionalScores;

    public ScoredCandidate(String inchi, boolean[] fingerprint, double scoreToSort, double... additionalScores) {
        super(inchi, fingerprint);
        this.score = scoreToSort;
        this.additionalScores = additionalScores;
    }

    public ScoredCandidate(Candidate candidate, double score, double... additionalScores) {
        this(candidate.inchi, candidate.fingerprint, score, additionalScores);
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
