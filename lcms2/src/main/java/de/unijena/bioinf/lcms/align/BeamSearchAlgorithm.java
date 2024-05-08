package de.unijena.bioinf.lcms.align;

import de.unijena.bioinf.lcms.utils.AlignmentBeamSearch;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class BeamSearchAlgorithm implements AlignmentAlgorithm{
    @Override
    public void align(AlignmentStatistics stats, AlignmentScorer scorer, AlignWithRecalibration rec, MoI[] left, MoI[] right, CallbackForAlign align,CallbackForLeftOver leftOver) {
        final double maxAllowedRtDiff = 4*stats.getExpectedRetentionTimeDeviation();
        final double maxAllowedMzDiff = 4*stats.expectedMassDeviationBetweenSamples.absoluteFor(Math.max(left[left.length-1].getMz(),right[right.length-1].getMz()));
        final List<PossibleAlignment> possibleAlignments = new ArrayList<>();
        int rinit = 0;
        eachLeft:
        for (int l=0; l < left.length; ++l) {
            final MoI L = left[l];
            eachR:
            for (int r = rinit; r < right.length; ++r) {
                final MoI R = right[r];
                final double mzDelta = L.getMz() - R.getMz();
                if (mzDelta>maxAllowedMzDiff) {
                    rinit=r;
                    continue eachR;
                }
                if (mzDelta<-maxAllowedMzDiff) continue eachLeft;
                if (Math.abs(L.getRetentionTime()-R.getRetentionTime())<maxAllowedRtDiff) {
                    final double score = scorer.score(stats, L, R);
                    possibleAlignments.add(new PossibleAlignment(l,r,(float)score));
                }
            }
        }
        possibleAlignments.sort(null);

        AlignmentBeamSearch beamSearch = new AlignmentBeamSearch(10);
        possibleAlignments.forEach(x-> beamSearch.add(x.left,x.right,x.score + 5 + 10*(left[x.left].getIntensity() + right[x.right].getIntensity())));

        final BitSet alignedLeft = new BitSet(left.length), alignedRight = new BitSet(right.length);

        for (AlignmentBeamSearch.MatchNode match : beamSearch.getTopSolution()) {
            align.alignWith(rec, left,right,match.leftIndex(), match.rightIndex());
            alignedLeft.set(match.leftIndex());
            alignedRight.set(match.rightIndex());
        }
        for (int i=alignedRight.nextClearBit(0); i < right.length; i = alignedRight.nextClearBit(i+1) ) {
            leftOver.leftOver(rec, right, i);
        }
    }

    private static class PossibleAlignment implements Comparable<PossibleAlignment> {
        private final int left, right;
        private final float score;

        public PossibleAlignment(int left, int right, float score) {
            this.left = left;
            this.right = right;
            this.score = score;
        }

        @Override
        public int compareTo(@NotNull BeamSearchAlgorithm.PossibleAlignment o) {
            return Float.compare(o.score, score);
        }
    }
}
