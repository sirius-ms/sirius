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

package de.unijena.bioinf.ChemistryBase.ms.ft;

import de.unijena.bioinf.ms.annotations.TreeAnnotation;

import java.util.Objects;

/**
 * Annotation to save the number of molecular formula candidates which are not further considered.
 * E.g. all which are not in the Top100.
 */
//todo this should be a Experiment Annotation!
public final class UnconsideredCandidatesUpperBound implements TreeAnnotation {
    private final int numberOfUnconsideredCandidates;
    private final double lowestConsideredCandidateScore;

    private static UnconsideredCandidatesUpperBound NO_RANKING_INVOLVED = new UnconsideredCandidatesUpperBound(-1, Double.NEGATIVE_INFINITY);
    public static UnconsideredCandidatesUpperBound noRankingInvolved() {
        return NO_RANKING_INVOLVED;
    }

    public UnconsideredCandidatesUpperBound(int numberOfUnconsideredCandidates, double lowestConsideredCandidateScore) {
        if (numberOfUnconsideredCandidates <0 && Double.isFinite(lowestConsideredCandidateScore)) throw new IllegalArgumentException("the number of unconsidered candidates must be positive.");
        this.numberOfUnconsideredCandidates = numberOfUnconsideredCandidates;
        this.lowestConsideredCandidateScore = lowestConsideredCandidateScore;
    }

    public int getNumberOfUnconsideredCandidates() {
        return numberOfUnconsideredCandidates;
    }

    public double getLowestConsideredCandidateScore() {
        return lowestConsideredCandidateScore;
    }

    public boolean isNoRankingInvolved() {
        return this.equals(NO_RANKING_INVOLVED);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UnconsideredCandidatesUpperBound that = (UnconsideredCandidatesUpperBound) o;
        return numberOfUnconsideredCandidates == that.numberOfUnconsideredCandidates &&
                Double.compare(that.lowestConsideredCandidateScore, lowestConsideredCandidateScore) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(numberOfUnconsideredCandidates, lowestConsideredCandidateScore);
    }
}
