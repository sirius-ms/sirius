/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.middleware.model.annotations;

import de.unijena.bioinf.ms.middleware.model.spectra.BasicSpectrum;
import de.unijena.bioinf.spectraldb.SpectralSearchResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Getter
@Builder
@Jacksonized
public class SpectralLibraryMatch {

    @Schema(enumAsRef = true, name = "SpectralLibraryMatchOptField", nullable = true)
    public enum OptField {none, referenceSpectrum}

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    public final Double similarity;

    public final Integer sharedPeaks;


    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private final Integer querySpectrumIndex;


    private final String dbName;

    private final String dbId;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private final long uuid;

    private final String splash;

    private final String molecularFormula;
    private final String adduct;
    private final String exactMass;
    private final String smiles;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private final String candidateInChiKey;

    @Schema(nullable = true)
    @Setter
    private BasicSpectrum referenceSpectrum;

    public static SpectralLibraryMatch of(@NotNull SpectralSearchResult.SearchResult result){
        return SpectralLibraryMatch.builder()
                .similarity(result.getSimilarity().similarity)
                .sharedPeaks(result.getSimilarity().sharedPeaks)
                .dbName(result.getDbName())
                .dbId(result.getDbId())
                .uuid(result.getUuid())
                .splash(result.getSplash())
                .smiles(result.getSmiles())
                .candidateInChiKey(result.getCandidateInChiKey())
                .querySpectrumIndex(result.getQuerySpectrumIndex())
                .build();
    }
    public static List<SpectralLibraryMatch> of(@NotNull SpectralSearchResult result){
        return of(result, null);
    }

    public static List<SpectralLibraryMatch> of(@NotNull SpectralSearchResult result, @Nullable String candidateInChiKey){
        return result.getResults().stream()
                .filter(s -> candidateInChiKey == null || candidateInChiKey.equals(s.getCandidateInChiKey()))
                .map(SpectralLibraryMatch::of)
                .toList();
    }
}
