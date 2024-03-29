/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
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

package de.unijena.bionf.spectral_alignment;

import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.util.Objects;

@Builder
@Jacksonized
public class SpectralSimilarity {
    public final double similarity;
    public final int sharedPeaks;

    public SpectralSimilarity() {
        this.similarity = 0;
        this.sharedPeaks = 0;
    }

    public SpectralSimilarity(double similarity, int sharedPeaks) {
        this.similarity = similarity;
        this.sharedPeaks = sharedPeaks;
    }

    @Override
    public String toString() {
        return "cosine = " + similarity + ", " + sharedPeaks + " shared peaks.";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpectralSimilarity that = (SpectralSimilarity) o;
        return Double.compare(similarity, that.similarity) == 0 && sharedPeaks == that.sharedPeaks;
    }

    @Override
    public int hashCode() {
        return Objects.hash(similarity, sharedPeaks);
    }
}
