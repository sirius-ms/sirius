package de.unijena.bioinf.lcms.utils;

import java.util.*;

public class AlignmentBeamSearch {

    public record MatchNode (MatchNode predecessor, int leftIndex, int rightIndex, BitSet usedL, BitSet usedR, double score) {

        static MatchNode init(int leftIndex, int rightIndex, double score) {
            BitSet lb = new BitSet();
            lb.set(leftIndex);
            BitSet rb = new BitSet();
            rb.set(rightIndex);
            return new MatchNode(null, leftIndex, rightIndex, lb, rb, score);
        }

        public MatchNode[] ancestors() {
            ArrayList<MatchNode> xs = new ArrayList<>();
            MatchNode m = this;
            while (m!=null) {
                xs.add(m);
                m = m.predecessor;
            }
            return xs.toArray(MatchNode[]::new);
        }

        public boolean isCompatible(int l, int r) {
            return !usedL.get(l) && !usedR.get(r);
        }
        public MatchNode extend(int l, int r, double score) {
            BitSet lb = (BitSet) usedL.clone();
            lb.set(l);
            BitSet rb = (BitSet) usedR.clone();
            rb.set(r);
            return new MatchNode(this, l, r, lb, rb, this.score+score);
        }
    };

    private static BitSet EMPTY_BITSET = new BitSet(0);

    private MatchNode[] beams;
    private int nbeams, stored;

    public AlignmentBeamSearch(int nbeams) {
        this.beams = new MatchNode[nbeams*2 + 1];
        this.nbeams = nbeams;
        this.stored = 0;
    }

    public class Buffer {
        private final ArrayList<MatchNode> matches;
        private Buffer() {
            this.matches = new ArrayList<>();
        }
        public void add(int leftIndex, int rightIndex, double score) {
            matches.add(new MatchNode(null, leftIndex, rightIndex,null,null,score));
        }

        public AlignmentBeamSearch release() {
            matches.sort(Comparator.comparingDouble(x->-x.score));
            for (MatchNode m : matches) {
                AlignmentBeamSearch.this.add(m.leftIndex,m.rightIndex,m.score);
            }
            return AlignmentBeamSearch.this;
        }
    }

    /**
     * Beam search is a greedy method, so there is no guarantee that it finds the best solution.
     * With buffering, all potential matches are sorted by score before feeded into the beam search.
     * There is no guarantee this will give a better solution, but the method becomes somewhat more greedy by doing so.
     */
    public Buffer buffer() {
        return new Buffer();
    }

    public void add(int leftIndex, int rightIndex, double score) {
        int newcap = stored;
        for (int k=0; k < stored; ++k) {
            MatchNode m = beams[k];
            if (m.isCompatible(leftIndex, rightIndex)) {
                beams[newcap++] = (m.extend(leftIndex, rightIndex, score));
            }
        }
        beams[newcap++] = MatchNode.init(leftIndex, rightIndex, score);
        Arrays.sort(beams, 0, newcap, Comparator.comparingDouble(x->-x.score));
        stored = Math.min(newcap, nbeams);
    }

    public MatchNode[] getTopSolution() {
        if (stored==0) return new MatchNode[0];
        if (stored < nbeams) {
            Arrays.sort(beams, 0, stored, Comparator.comparingDouble(x->-x.score));
        }
        return beams[0].ancestors();
    }

    public MatchNode[][] getTopSolutions() {
        if (stored < nbeams) {
            Arrays.sort(beams, 0, stored, Comparator.comparingDouble(x->-x.score));
        }
        MatchNode[][] mx = new MatchNode[stored][];
        for (int k=0; k < stored; ++k) {
            mx[k] = beams[k].ancestors();
        }
        return mx;
    }

    public Iterator<MatchNode[]> iterateTopSolutions() {
        if (stored < nbeams) {
            Arrays.sort(beams, 0, stored, Comparator.comparingDouble(x->-x.score));
        }
        return new Iterator<MatchNode[]>() {
            int currentIndex = 0;
            @Override
            public boolean hasNext() {
                return currentIndex < stored;
            }

            @Override
            public MatchNode[] next() {
                return beams[currentIndex++].ancestors();
            }
        };
    }

}
