/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ChemistryBase.algorithm.scoring;

import com.google.common.base.Equivalence;
import com.google.common.base.Predicate;
import com.google.common.collect.Range;
import gnu.trove.list.array.TIntArrayList;

import java.util.List;

/**
 *
 */
public class Ranking {

    private int[] fromRank, toRank, maxRank;
    private double[] rankDistribution, randomDistribution;

    public static Builder build(int maxRanking) {
        return new Builder(maxRanking);
    }

    private Ranking(int maxRanking, int[] fromRank, int[] toRank, int[] maxRank) {
        this.fromRank = fromRank;
        this.toRank = toRank;
        this.maxRank = maxRank;
        this.rankDistribution = new double[maxRanking];
        this.randomDistribution = new double[maxRanking];
        for (int k=0; k < fromRank.length; ++k) {
            final int from = fromRank[k];
            final int to = toRank[k];
            final double x = 1d/(1+to-from);
            final double random = 1d/maxRank[k];
            for (int i=from; i < Math.min(to+1, rankDistribution.length); ++i) {
                rankDistribution[i] += x;
            }
            for (int i=0; i < Math.min(maxRank[k]+1, maxRanking); ++i) randomDistribution[i]+=random;
        }
        int sum = fromRank.length;
        double cumsum = 0d;
        for (int k=0; k < rankDistribution.length; ++k) {
            cumsum += rankDistribution[k];
            rankDistribution[k] = cumsum / sum;
        }
        cumsum = 0d;
        for (int k=0; k < randomDistribution.length; ++k) {
            cumsum += randomDistribution[k];
            randomDistribution[k] = cumsum / sum;
        }
    }

    public int size() {
        return fromRank.length;
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

    public double withinTopByRandom(int k) {
        return randomDistribution[k];
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

        public <T> Builder update(List<Scored<T>> orderedList, T candidate) {
            return update(orderedList, candidate, (Equivalence<T>) Equivalence.equals());
        }

        public <T> Builder update(List<Scored<T>> orderedList, T candidate, Equivalence<T> equiv) {
            if (orderedList.isEmpty()) return this;
            final Range<Integer> ranks = getRankFor(orderedList,candidate,equiv);
            update(ranks.lowerEndpoint(), ranks.upperEndpoint(), orderedList.size());
            return this;
        }

        public <T> Builder update(List<Scored<T>> orderedList, Predicate<T> search) {
            if (orderedList.isEmpty()) return this;
            final Range<Integer> ranks = getRankFor(orderedList,search);
            update(ranks.lowerEndpoint(), ranks.upperEndpoint(), orderedList.size());
            return this;
        }

        public <T> Range<Integer> getRankFor(List<Scored<T>> orderedList, Predicate<T> search) {
            for (int k=0; k < orderedList.size(); ++k) {
                if (search.apply(orderedList.get(k).getCandidate())) {
                    // search for other candidates which are equivalent
                    final double optScore = orderedList.get(k).getScore();
                    int before=k-1, next=k+1;
                    while (before >= 0 && (Math.abs(orderedList.get(before).getScore()-optScore)<1e-12)) --before;
                    ++before;
                    while (next < orderedList.size() && (Math.abs(orderedList.get(next).getScore()-optScore)<1e-12)) ++next;
                    --next;
                    //update(before, next, orderedList.size());
                    return Range.closed(before, next);
                }
            }
            return Range.closed(1, orderedList.size());
        }

        public <T> Range<Integer> getRankFor(List<Scored<T>> orderedList, T candidate, Equivalence<T> equiv) {
            for (int k=0; k < orderedList.size(); ++k) {
                if (equiv.equivalent(candidate, orderedList.get(k).getCandidate())) {
                    // search for other candidates which are equivalent
                    final double optScore = orderedList.get(k).getScore();
                    int before=k-1, next=k+1;
                    while (before >= 0 && (Math.abs(orderedList.get(before).getScore()-optScore)<1e-12)) --before;
                    ++before;
                    while (next < orderedList.size() && (Math.abs(orderedList.get(next).getScore()-optScore)<1e-12)) ++next;
                    --next;
                    //update(before, next, orderedList.size());
                    return Range.closed(before, next);
                }
            }
            return Range.closed(1, orderedList.size());
        }

        public Ranking done() {
            return new Ranking(mx, from.toArray(), to.toArray(), max.toArray());
        }
    }

}
