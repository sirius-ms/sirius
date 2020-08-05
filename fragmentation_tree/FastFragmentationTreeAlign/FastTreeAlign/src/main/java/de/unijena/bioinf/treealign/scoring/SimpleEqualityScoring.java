
package de.unijena.bioinf.treealign.scoring;

public interface SimpleEqualityScoring<T> {

    /**
     * @param left the incomming edge of this vertex is matched
     * @param right the incomming edge of this vertex is matched
     * @return score for this match operation
     */
    public boolean isMatching(T left, T right);

}
