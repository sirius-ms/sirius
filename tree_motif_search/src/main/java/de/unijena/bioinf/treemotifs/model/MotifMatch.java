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

package de.unijena.bioinf.treemotifs.model;

public class MotifMatch {

    protected final double TotalProbability, maxProbability;
    protected final long[] matchingFragments, matchingRootLosses;

    MotifMatch(double totalProbability, double maxProbability, long[] matchingFragments, long[] matchingRootLosses) {
        TotalProbability = totalProbability;
        this.maxProbability = maxProbability;
        this.matchingFragments = matchingFragments;
        this.matchingRootLosses = matchingRootLosses;
    }

    public double getTotalProbability() {
        return TotalProbability;
    }

    public double getMaxProbability() {
        return maxProbability;
    }

    public long[] getMatchingFragments() {
        return matchingFragments;
    }

    public long[] getMatchingRootLosses() {
        return matchingRootLosses;
    }
}
