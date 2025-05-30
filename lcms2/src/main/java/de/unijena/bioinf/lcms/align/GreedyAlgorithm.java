package de.unijena.bioinf.lcms.align;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class GreedyAlgorithm implements AlignmentAlgorithm{
    @Override
    public void align(AlignmentStatistics stats, AlignmentThresholds thresholds, AlignmentScorer scorer, AlignWithRecalibration rec, MoI[] left, MoI[] right, CallbackForAlign align,CallbackForLeftOver leftOver) {
        final double maxAllowedRtDiff = thresholds.hasRetentionTimeThreshold() ? thresholds.getMaximalAllowedRetentionTimeError() : 5*stats.getExpectedRetentionTimeDeviation();
        final double maxmz = Math.max(left[left.length-1].getMz(),right[right.length-1].getMz());
        final double maxAllowedMzDiff = thresholds.hasMassThreshold() ? thresholds.getMaximalAllowedMassError().absoluteFor(maxmz) : 4*stats.expectedMassDeviationBetweenSamples.absoluteFor(maxmz);
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
        final BitSet alignedLeft = new BitSet(left.length), alignedRight = new BitSet(right.length);
        for (int k=0; k < possibleAlignments.size(); ++k) {
            PossibleAlignment A = possibleAlignments.get(k);
            if (alignedLeft.get(A.left) || alignedRight.get(A.right)) continue;
            align.alignWith(rec, left,right,A.left,A.right);
            alignedLeft.set(A.left);
            alignedRight.set(A.right);
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
        public int compareTo(@NotNull GreedyAlgorithm.PossibleAlignment o) {
            return Float.compare(o.score, score);
        }
    }
}
