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

package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ms.annotations.ResultAnnotation;
import de.unijena.bionf.spectral_alignment.SpectralAlignmentType;
import de.unijena.bionf.spectral_alignment.SpectralSimilarity;
import lombok.*;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;

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
    }
}
