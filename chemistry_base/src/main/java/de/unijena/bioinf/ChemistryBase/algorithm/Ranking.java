package de.unijena.bioinf.ChemistryBase.algorithm;

import com.google.common.collect.Range;
import gnu.trove.list.array.TIntArrayList;

/**
 *
 */
public class Ranking {

    private int[] fromRank, toRank, maxRank;
    private double[] rankDistribution;

    public static Builder build(int maxRanking) {
        return new Builder(maxRanking);
    }

    private Ranking(int maxRanking, int[] fromRank, int[] toRank, int[] maxRank) {
        this.fromRank = fromRank;
        this.toRank = toRank;
        this.maxRank = maxRank;
        this.rankDistribution = new double[maxRanking];
        double restBin = 0d;
        for (int k=0; k < fromRank.length; ++k) {
            final int from = fromRank[k];
            final int to = toRank[k];
            final double x = 1d/(1+to-from);
            for (int i=from; i < Math.min(to, rankDistribution.length); ++i) {
                rankDistribution[i] += x;
            }
            restBin += Math.max(0, to - Math.min(to, rankDistribution.length))*x;
        }
        double sum = restBin;
        for (double val : rankDistribution)  sum += val;
        double cumsum = 0d;
        for (int k=0; k < rankDistribution.length; ++k) {
            cumsum += rankDistribution[k];
            rankDistribution[k] = cumsum / sum;
        }
    }

    public Range<Integer> getRanking(int index) {
        return Range.closed(fromRank[index], toRank[index]);
    }

    public int getMinRank(int index) {
        return fromRank[index];
    }

    public int getMaxRank(int index) {
        return toRank[index];
    }

    public double getAverageRank(int index) {
        return fromRank[index] + (toRank[index]-fromRank[index])/2;
    }

    public double withinTop(int k) {
        return rankDistribution[k];
    }

    public static class Builder {

        private TIntArrayList from,to,max;
        private int mx;

        private Builder(int mx) {
            this.from = new TIntArrayList();
            this.to = new TIntArrayList();
            this.max = new TIntArrayList();
            this.mx = mx;
        }

        public Builder update(int from, int to, int max) {
            this.from.add(from);
            this.to.add(to);
            this.max.add(max);
            return this;
        }

        public Ranking done() {
            return new Ranking(mx, from.toArray(), to.toArray(), max.toArray());
        }
    }

}
