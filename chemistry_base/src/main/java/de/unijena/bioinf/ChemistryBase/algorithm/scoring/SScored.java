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

public class SScored<T, S extends Score> implements Comparable<SScored<T, S>> {

    private final T candidate;
    private final S score;

    public SScored(T candidate, S score) {
        this.candidate = candidate;
        this.score = score;
    }

    public T getCandidate() {
        return candidate;
    }

    public S getScoreObject() {
        return score;
    }

    public double getScore() {
        return score.score();
    }

    @Override
    public String toString() {
        return String.format("%s (%.3f)", candidate.toString(), score.score());
    }

    @Override
    public int compareTo(SScored<T, S> o) {
        return score.compareTo(o.score);
    }
}
