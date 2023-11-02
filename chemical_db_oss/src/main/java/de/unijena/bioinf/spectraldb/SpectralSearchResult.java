/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.spectraldb;

import com.google.common.collect.Streams;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ms.annotations.ResultAnnotation;
import de.unijena.bionf.spectral_alignment.SpectralSimilarity;
import lombok.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class SpectralSearchResult implements Iterable<SpectralSearchResult.SearchResult>, ResultAnnotation {

    private Deviation precursorDeviation;

    private Deviation peakDeviation;

    private SpectralAlignmentType alignmentType;

    private List<SearchResult> results;

    @NotNull
    @Override
    public Iterator<SearchResult> iterator() {
        return results.iterator();
    }

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class SearchResult {

        private String dbName;

        private String dbId;

        int querySpectrumIndex;

        @Builder.Default
        private int rank = -1;

        private SpectralSimilarity similarity;

        private String referenceUUID;

        private String referenceSplash;

    }

    public <Result extends SpectralSearchResult> void join(Result other) {

        if (precursorDeviation != other.getPrecursorDeviation()) {
            LoggerFactory.getLogger(this.getClass()).warn("Mismatching precursor deviation! [Expected: " + precursorDeviation + ", actual: " + other.getPrecursorDeviation() + "]");
        }
        if (peakDeviation != other.getPeakDeviation()) {
            LoggerFactory.getLogger(this.getClass()).warn("Mismatching peak deviation! [Expected: " + peakDeviation + ", actual: " + other.getPeakDeviation() + "]");
        }
        if (alignmentType != other.getAlignmentType()) {
            LoggerFactory.getLogger(this.getClass()).warn("Mismatching alignment type! [Expected: " + alignmentType + ", actual: " + other.getAlignmentType() + "]");
        }

        Stream<SearchResult> sortedStream = Stream.concat(results.stream(), other.getResults().stream()).sorted((a, b) -> Double.compare(b.getSimilarity().similarity, a.getSimilarity().similarity));

        results = Streams.mapWithIndex(sortedStream, (r, index) -> {
            r.setRank((int) index + 1);
            return r;
        }).toList();

    }

}
