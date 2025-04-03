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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import de.unijena.bioinf.ChemistryBase.utils.FastUtilJson;
import it.unimi.dsi.fastutil.ints.*;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@Builder
@Jacksonized
public class SpectralSimilarity implements Comparable<SpectralSimilarity> {
    public final double similarity;
    public final int sharedPeaks;

    /**
     * matched peak pairs
     * left spec index ->  right spec index
     * query spec index -> reference spec index
     * Empty if no matching peaks or NULL if similarity measure does not support assignments
     */
    @JsonDeserialize(using = FastUtilJson.IntListDeserializer.class)
    @JsonSerialize(using = FastUtilJson.IntCollectionSerializer.class)
    @Nullable
    @Getter
    private final IntList sharedPeakPairs;

    public Int2IntMap getSharedPeakPairsMap() {
        if (sharedPeakPairs == null)
            return null;
        Int2IntMap sharedPeakPairsMap = new Int2IntOpenHashMap(sharedPeakPairs.size() >> 1);
        for (int i = 0; i < sharedPeakPairs.size(); i += 2)
             sharedPeakPairsMap.put(sharedPeakPairs.getInt(i), sharedPeakPairs.getInt(i + 1));

        return sharedPeakPairsMap;
    }

    public SpectralSimilarity() {
        this.similarity = 0;
        this.sharedPeaks = 0;
        this.sharedPeakPairs = IntLists.EMPTY_LIST;
    }

    public SpectralSimilarity(double similarity, @Nullable IntList sharedPeakPairs) {
        this(similarity, sharedPeakPairs == null ? 0 : sharedPeakPairs.size() >> 1, sharedPeakPairs);
    }

    public SpectralSimilarity(double similarity, int sharedPeaks, @Nullable IntList sharedPeakPairs) {
        this.similarity = similarity;
        this.sharedPeaks = sharedPeaks;
        this.sharedPeakPairs = sharedPeakPairs;
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

    @Override
    public int compareTo(@NotNull SpectralSimilarity o) {
        int c = Double.compare(similarity,o.similarity);
        if (c==0) return Integer.compare(sharedPeaks,o.sharedPeaks);
        else return c;
    }
}
