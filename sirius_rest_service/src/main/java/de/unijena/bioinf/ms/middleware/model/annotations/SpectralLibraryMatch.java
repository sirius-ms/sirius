/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.middleware.model.annotations;

import de.unijena.bioinf.ms.middleware.model.spectra.BasicSpectrum;
import de.unijena.bioinf.ms.persistence.model.sirius.SpectraMatch;
import de.unijena.bioinf.spectraldb.SpectralSearchResult;
import de.unijena.bioinf.spectraldb.SpectrumType;
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

    @Schema(name = "SpectralLibraryMatchOptField", nullable = true)
    public enum OptField {none, referenceSpectrum}

    public final String specMatchId;

    public final Integer rank;

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

    @Schema(defaultValue = "SPECTRUM")
    private final TargetType target;

    @Schema(defaultValue = "COSINE")
    private final MatchType type;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private final String inchiKey;

    @Schema(nullable = true)
    @Setter
    private BasicSpectrum referenceSpectrum;

    public static SpectralLibraryMatch of(@NotNull SpectraMatch match){
        return of(match.getSearchResult(), String.valueOf(match.getSpecMatchId()));
    }
    public static SpectralLibraryMatch of(@NotNull SpectralSearchResult.SearchResult result, String id){
        SpectralLibraryMatch.SpectralLibraryMatchBuilder builder = SpectralLibraryMatch.builder();
        if (result.getSimilarity() != null) {
            builder.similarity(result.getSimilarity().similarity);
            builder.sharedPeaks(result.getSimilarity().sharedPeaks);
        }

        builder.rank(result.getRank())
                .specMatchId(id)
                .querySpectrumIndex(result.getQuerySpectrumIndex())
                .dbName(result.getDbName())
                .dbId(result.getDbId())
                .uuid(result.getUuid())
                .splash(result.getSplash())
                .exactMass(Double.toString(result.getExactMass()))
                .smiles(result.getSmiles())
                .target(result.getSpectrumType()== SpectrumType.MERGED_SPECTRUM ? TargetType.MERGED : TargetType.SPECTRUM)
                .type(result.isAnalog() ? MatchType.ANALOG : MatchType.COSINE)
                .inchiKey(result.getCandidateInChiKey());

        if (result.getMolecularFormula() != null) {
            builder.molecularFormula(result.getMolecularFormula().toString());
        }
        if (result.getAdduct() != null) {
            builder.adduct(result.getAdduct().toString());
        }
        return builder.build();
    }
    @Deprecated
    public static List<SpectralLibraryMatch> of(@NotNull SpectralSearchResult result){
        return of(result, null);
    }

    @Deprecated
    public static List<SpectralLibraryMatch> of(@NotNull SpectralSearchResult result, @Nullable String candidateInChiKey){
        return result.getResults().stream()
                .filter(s -> candidateInChiKey == null || candidateInChiKey.equals(s.getCandidateInChiKey()))
                .map(m-> SpectralLibraryMatch.of(m, null))
                .toList();
    }

    public static enum MatchType {
        COSINE,
        ANALOG;
    }

    public static enum TargetType {
        SPECTRUM,
        MERGED;

        public SpectrumType asSpectrumType() {
            if (this==SPECTRUM) return SpectrumType.SPECTRUM;
            if (this==MERGED) return SpectrumType.MERGED_SPECTRUM;
            throw new IllegalArgumentException("Unknown spectrum type");
        }
    }
}
