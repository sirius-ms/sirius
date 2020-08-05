
package de.unijena.bioinf.counting;


import de.unijena.bioinf.graphUtils.tree.TreeAdapter;
import de.unijena.bioinf.treealign.scoring.SimpleEqualityScoring;

public class DPSubtreeCounter<T> extends DPPathCounting<T> {

    public DPSubtreeCounter(SimpleEqualityScoring<T> scoring, T left, T right, TreeAdapter<T> adapter) {
        super(scoring, left, right, adapter);
    }

    @Override
    protected long recurrence(long counter, int a, int b) {
        final long x = (((a < 0 || b < 0) ? 0 : D[a][b]));
        return counter * (2 + x);
    }
}
