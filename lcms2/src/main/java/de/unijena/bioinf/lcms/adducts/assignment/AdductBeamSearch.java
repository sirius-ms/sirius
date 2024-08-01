package de.unijena.bioinf.lcms.adducts.assignment;

import java.util.*;

public class AdductBeamSearch {

    public record MatchNode (int[] assignment, double score) {

        static MatchNode init(int nsize, int position1, int value1, int position2, int value2, double score) {
            int[] assign = new int[nsize];
            assign[position1] = value1;
            assign[position2] = value2;
            return new MatchNode(assign, score);
        }

        public boolean isCompatible(int position1, int value1, int position2, int value2) {
            return (assignment[position1]==0 || assignment[position1]==value1) && (assignment[position2]==0 || assignment[position2]==value2);
        }
        public MatchNode extend(int position1, int value1, int position2, int value2, double score) {
            int[] assignment = this.assignment.clone();
            assignment[position1] = value1;
            assignment[position2] = value2;
            return new MatchNode(assignment, this.score + score);
        }
    };


    private MatchNode[] beams;

    private int numberOfNodes;
    private int nbeams, stored;

    public AdductBeamSearch(int numberOfNodes, int nbeams) {
        this.beams = new MatchNode[nbeams*2 + 1];
        this.nbeams = nbeams;
        this.stored = 0;
        this.numberOfNodes = numberOfNodes;
    }

    public void add(int position1, int value1, int position2, int value2, double score) {
        int newcap = stored;
        for (int k=0; k < stored; ++k) {
            MatchNode m = beams[k];
            if (m.isCompatible(position1, value1, position2, value2)) {
                beams[newcap++] = (m.extend(position1, value1, position2, value2, score));
            }
        }
        beams[newcap++] = MatchNode.init(numberOfNodes, position1, value1, position2, value2, score);
        Arrays.sort(beams, 0, newcap, Comparator.comparingDouble(x->-x.score));
        stored = Math.min(newcap, nbeams);
    }

    public MatchNode getTopSolution() {
        if (stored==0) return MatchNode.init(numberOfNodes, 0, 0, 0, 0, 0d);
        if (stored < nbeams) {
            Arrays.sort(beams, 0, stored, Comparator.comparingDouble(x->-x.score));
        }
        return beams[0];
    }

    public MatchNode[] getTopSolutions() {
        if (stored < nbeams) {
            Arrays.sort(beams, 0, stored, Comparator.comparingDouble(x->-x.score));
        }
        MatchNode[] mx = new MatchNode[stored];
        System.arraycopy(beams,0,mx,0,stored);
        return mx;
    }
}

