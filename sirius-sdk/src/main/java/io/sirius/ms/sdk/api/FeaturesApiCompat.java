/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package io.sirius.ms.sdk.api;

import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.model.AlignedFeatureOptField;
import io.sirius.ms.sdk.model.FormulaCandidateOptField;
import io.sirius.ms.sdk.model.PagedModelAlignedFeature;
import io.sirius.ms.sdk.model.PagedModelFormulaCandidate;
import io.sirius.ms.sdk.model.PagedModelSpectralLibraryMatch;
import io.sirius.ms.sdk.model.PagedModelStructureCandidateFormula;
import io.sirius.ms.sdk.model.PagedModelStructureCandidateScored;
import io.sirius.ms.sdk.model.SpectralLibraryMatchOptField;
import io.sirius.ms.sdk.model.StructureCandidateOptField;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;

/**
 * Adds the deprecated pre-rename method names to the generated {@link FeaturesApi}.
 * <p>
 * Several {@code ...Paged} operationIds were a naming mistake on publicly facing method names and have been corrected
 * to {@code ...Page}. The HTTP routes are unchanged, so this is purely a source-level compatibility shim: existing
 * call sites keep compiling for one release while they migrate to the corrected names.
 * <p>
 * These aliases are scheduled for removal in the next major release. They are hand written because everything under
 * {@code sirius-sdk.openapi} is overwritten by {@code openApiGenerate}.
 */
public class FeaturesApiCompat extends FeaturesApi {

    public FeaturesApiCompat(ApiClient apiClient) {
        super(apiClient);
    }

    /**
     * @deprecated renamed to {@link #getAlignedFeaturesPage(String, Integer, Integer, List, String, Boolean, List)}.
     * Will be removed in the next major release.
     */
    @Deprecated(forRemoval = true)
    public PagedModelAlignedFeature getAlignedFeaturesPaged(
            @jakarta.annotation.Nonnull String projectId,
            @jakarta.annotation.Nullable Integer page,
            @jakarta.annotation.Nullable Integer size,
            @jakarta.annotation.Nullable List<String> sort,
            @jakarta.annotation.Nullable Boolean msDataSearchPrepared,
            @jakarta.annotation.Nullable List<AlignedFeatureOptField> optFields
    ) throws WebClientResponseException {
        return getAlignedFeaturesPage(projectId, page, size, sort, null, msDataSearchPrepared, optFields);
    }

    /**
     * @deprecated renamed to {@link #getAlignedFeaturesPageWithHttpInfo(String, Integer, Integer, List, String, Boolean, List)}.
     * Will be removed in the next major release.
     */
    @Deprecated(forRemoval = true)
    public ResponseEntity<PagedModelAlignedFeature> getAlignedFeaturesPagedWithHttpInfo(
            @jakarta.annotation.Nonnull String projectId,
            @jakarta.annotation.Nullable Integer page,
            @jakarta.annotation.Nullable Integer size,
            @jakarta.annotation.Nullable List<String> sort,
            @jakarta.annotation.Nullable Boolean msDataSearchPrepared,
            @jakarta.annotation.Nullable List<AlignedFeatureOptField> optFields
    ) throws WebClientResponseException {
        return getAlignedFeaturesPageWithHttpInfo(projectId, page, size, sort, null, msDataSearchPrepared, optFields);
    }

    /**
     * @deprecated renamed to {@link #getAlignedFeaturesPageWithResponseSpec(String, Integer, Integer, List, String, Boolean, List)}.
     * Will be removed in the next major release.
     */
    @Deprecated(forRemoval = true)
    public ResponseSpec getAlignedFeaturesPagedWithResponseSpec(
            @jakarta.annotation.Nonnull String projectId,
            @jakarta.annotation.Nullable Integer page,
            @jakarta.annotation.Nullable Integer size,
            @jakarta.annotation.Nullable List<String> sort,
            @jakarta.annotation.Nullable Boolean msDataSearchPrepared,
            @jakarta.annotation.Nullable List<AlignedFeatureOptField> optFields
    ) throws WebClientResponseException {
        return getAlignedFeaturesPageWithResponseSpec(projectId, page, size, sort, null, msDataSearchPrepared, optFields);
    }

    /**
     * @deprecated renamed to {@link #getStructureCandidatesPage(String, String, Integer, Integer, List<String>, List<StructureCandidateOptField>)}.
     * Will be removed in the next major release.
     */
    @Deprecated(forRemoval = true)
    public PagedModelStructureCandidateFormula getStructureCandidatesPaged(
            @jakarta.annotation.Nonnull String projectId,
            @jakarta.annotation.Nonnull String alignedFeatureId,
            @jakarta.annotation.Nullable Integer page,
            @jakarta.annotation.Nullable Integer size,
            @jakarta.annotation.Nullable List<String> sort,
            @jakarta.annotation.Nullable List<StructureCandidateOptField> optFields
    ) throws WebClientResponseException {
        return getStructureCandidatesPage(projectId, alignedFeatureId, page, size, sort, optFields);
    }

    /**
     * @deprecated renamed to {@link #getStructureCandidatesPageWithHttpInfo(String, String, Integer, Integer, List<String>, List<StructureCandidateOptField>)}.
     * Will be removed in the next major release.
     */
    @Deprecated(forRemoval = true)
    public ResponseEntity<PagedModelStructureCandidateFormula> getStructureCandidatesPagedWithHttpInfo(
            @jakarta.annotation.Nonnull String projectId,
            @jakarta.annotation.Nonnull String alignedFeatureId,
            @jakarta.annotation.Nullable Integer page,
            @jakarta.annotation.Nullable Integer size,
            @jakarta.annotation.Nullable List<String> sort,
            @jakarta.annotation.Nullable List<StructureCandidateOptField> optFields
    ) throws WebClientResponseException {
        return getStructureCandidatesPageWithHttpInfo(projectId, alignedFeatureId, page, size, sort, optFields);
    }

    /**
     * @deprecated renamed to {@link #getStructureCandidatesPageWithResponseSpec(String, String, Integer, Integer, List<String>, List<StructureCandidateOptField>)}.
     * Will be removed in the next major release.
     */
    @Deprecated(forRemoval = true)
    public ResponseSpec getStructureCandidatesPagedWithResponseSpec(
            @jakarta.annotation.Nonnull String projectId,
            @jakarta.annotation.Nonnull String alignedFeatureId,
            @jakarta.annotation.Nullable Integer page,
            @jakarta.annotation.Nullable Integer size,
            @jakarta.annotation.Nullable List<String> sort,
            @jakarta.annotation.Nullable List<StructureCandidateOptField> optFields
    ) throws WebClientResponseException {
        return getStructureCandidatesPageWithResponseSpec(projectId, alignedFeatureId, page, size, sort, optFields);
    }

    /**
     * @deprecated renamed to {@link #getDeNovoStructureCandidatesPage(String, String, Integer, Integer, List<String>, List<StructureCandidateOptField>)}.
     * Will be removed in the next major release.
     */
    @Deprecated(forRemoval = true)
    public PagedModelStructureCandidateFormula getDeNovoStructureCandidatesPaged(
            @jakarta.annotation.Nonnull String projectId,
            @jakarta.annotation.Nonnull String alignedFeatureId,
            @jakarta.annotation.Nullable Integer page,
            @jakarta.annotation.Nullable Integer size,
            @jakarta.annotation.Nullable List<String> sort,
            @jakarta.annotation.Nullable List<StructureCandidateOptField> optFields
    ) throws WebClientResponseException {
        return getDeNovoStructureCandidatesPage(projectId, alignedFeatureId, page, size, sort, optFields);
    }

    /**
     * @deprecated renamed to {@link #getDeNovoStructureCandidatesPageWithHttpInfo(String, String, Integer, Integer, List<String>, List<StructureCandidateOptField>)}.
     * Will be removed in the next major release.
     */
    @Deprecated(forRemoval = true)
    public ResponseEntity<PagedModelStructureCandidateFormula> getDeNovoStructureCandidatesPagedWithHttpInfo(
            @jakarta.annotation.Nonnull String projectId,
            @jakarta.annotation.Nonnull String alignedFeatureId,
            @jakarta.annotation.Nullable Integer page,
            @jakarta.annotation.Nullable Integer size,
            @jakarta.annotation.Nullable List<String> sort,
            @jakarta.annotation.Nullable List<StructureCandidateOptField> optFields
    ) throws WebClientResponseException {
        return getDeNovoStructureCandidatesPageWithHttpInfo(projectId, alignedFeatureId, page, size, sort, optFields);
    }

    /**
     * @deprecated renamed to {@link #getDeNovoStructureCandidatesPageWithResponseSpec(String, String, Integer, Integer, List<String>, List<StructureCandidateOptField>)}.
     * Will be removed in the next major release.
     */
    @Deprecated(forRemoval = true)
    public ResponseSpec getDeNovoStructureCandidatesPagedWithResponseSpec(
            @jakarta.annotation.Nonnull String projectId,
            @jakarta.annotation.Nonnull String alignedFeatureId,
            @jakarta.annotation.Nullable Integer page,
            @jakarta.annotation.Nullable Integer size,
            @jakarta.annotation.Nullable List<String> sort,
            @jakarta.annotation.Nullable List<StructureCandidateOptField> optFields
    ) throws WebClientResponseException {
        return getDeNovoStructureCandidatesPageWithResponseSpec(projectId, alignedFeatureId, page, size, sort, optFields);
    }

    /**
     * @deprecated renamed to {@link #getFormulaCandidatesPage(String, String, Integer, Integer, List<String>, Boolean, List<FormulaCandidateOptField>)}.
     * Will be removed in the next major release.
     */
    @Deprecated(forRemoval = true)
    public PagedModelFormulaCandidate getFormulaCandidatesPaged(
            @jakarta.annotation.Nonnull String projectId,
            @jakarta.annotation.Nonnull String alignedFeatureId,
            @jakarta.annotation.Nullable Integer page,
            @jakarta.annotation.Nullable Integer size,
            @jakarta.annotation.Nullable List<String> sort,
            @jakarta.annotation.Nullable Boolean msDataSearchPrepared,
            @jakarta.annotation.Nullable List<FormulaCandidateOptField> optFields
    ) throws WebClientResponseException {
        return getFormulaCandidatesPage(projectId, alignedFeatureId, page, size, sort, msDataSearchPrepared, optFields);
    }

    /**
     * @deprecated renamed to {@link #getFormulaCandidatesPageWithHttpInfo(String, String, Integer, Integer, List<String>, Boolean, List<FormulaCandidateOptField>)}.
     * Will be removed in the next major release.
     */
    @Deprecated(forRemoval = true)
    public ResponseEntity<PagedModelFormulaCandidate> getFormulaCandidatesPagedWithHttpInfo(
            @jakarta.annotation.Nonnull String projectId,
            @jakarta.annotation.Nonnull String alignedFeatureId,
            @jakarta.annotation.Nullable Integer page,
            @jakarta.annotation.Nullable Integer size,
            @jakarta.annotation.Nullable List<String> sort,
            @jakarta.annotation.Nullable Boolean msDataSearchPrepared,
            @jakarta.annotation.Nullable List<FormulaCandidateOptField> optFields
    ) throws WebClientResponseException {
        return getFormulaCandidatesPageWithHttpInfo(projectId, alignedFeatureId, page, size, sort, msDataSearchPrepared, optFields);
    }

    /**
     * @deprecated renamed to {@link #getFormulaCandidatesPageWithResponseSpec(String, String, Integer, Integer, List<String>, Boolean, List<FormulaCandidateOptField>)}.
     * Will be removed in the next major release.
     */
    @Deprecated(forRemoval = true)
    public ResponseSpec getFormulaCandidatesPagedWithResponseSpec(
            @jakarta.annotation.Nonnull String projectId,
            @jakarta.annotation.Nonnull String alignedFeatureId,
            @jakarta.annotation.Nullable Integer page,
            @jakarta.annotation.Nullable Integer size,
            @jakarta.annotation.Nullable List<String> sort,
            @jakarta.annotation.Nullable Boolean msDataSearchPrepared,
            @jakarta.annotation.Nullable List<FormulaCandidateOptField> optFields
    ) throws WebClientResponseException {
        return getFormulaCandidatesPageWithResponseSpec(projectId, alignedFeatureId, page, size, sort, msDataSearchPrepared, optFields);
    }

    /**
     * @deprecated renamed to {@link #getStructureCandidatesByFormulaPage(String, String, String, Integer, Integer, List<String>, List<StructureCandidateOptField>)}.
     * Will be removed in the next major release.
     */
    @Deprecated(forRemoval = true)
    public PagedModelStructureCandidateScored getStructureCandidatesByFormulaPaged(
            @jakarta.annotation.Nonnull String projectId,
            @jakarta.annotation.Nonnull String alignedFeatureId,
            @jakarta.annotation.Nonnull String formulaId,
            @jakarta.annotation.Nullable Integer page,
            @jakarta.annotation.Nullable Integer size,
            @jakarta.annotation.Nullable List<String> sort,
            @jakarta.annotation.Nullable List<StructureCandidateOptField> optFields
    ) throws WebClientResponseException {
        return getStructureCandidatesByFormulaPage(projectId, alignedFeatureId, formulaId, page, size, sort, optFields);
    }

    /**
     * @deprecated renamed to {@link #getStructureCandidatesByFormulaPageWithHttpInfo(String, String, String, Integer, Integer, List<String>, List<StructureCandidateOptField>)}.
     * Will be removed in the next major release.
     */
    @Deprecated(forRemoval = true)
    public ResponseEntity<PagedModelStructureCandidateScored> getStructureCandidatesByFormulaPagedWithHttpInfo(
            @jakarta.annotation.Nonnull String projectId,
            @jakarta.annotation.Nonnull String alignedFeatureId,
            @jakarta.annotation.Nonnull String formulaId,
            @jakarta.annotation.Nullable Integer page,
            @jakarta.annotation.Nullable Integer size,
            @jakarta.annotation.Nullable List<String> sort,
            @jakarta.annotation.Nullable List<StructureCandidateOptField> optFields
    ) throws WebClientResponseException {
        return getStructureCandidatesByFormulaPageWithHttpInfo(projectId, alignedFeatureId, formulaId, page, size, sort, optFields);
    }

    /**
     * @deprecated renamed to {@link #getStructureCandidatesByFormulaPageWithResponseSpec(String, String, String, Integer, Integer, List<String>, List<StructureCandidateOptField>)}.
     * Will be removed in the next major release.
     */
    @Deprecated(forRemoval = true)
    public ResponseSpec getStructureCandidatesByFormulaPagedWithResponseSpec(
            @jakarta.annotation.Nonnull String projectId,
            @jakarta.annotation.Nonnull String alignedFeatureId,
            @jakarta.annotation.Nonnull String formulaId,
            @jakarta.annotation.Nullable Integer page,
            @jakarta.annotation.Nullable Integer size,
            @jakarta.annotation.Nullable List<String> sort,
            @jakarta.annotation.Nullable List<StructureCandidateOptField> optFields
    ) throws WebClientResponseException {
        return getStructureCandidatesByFormulaPageWithResponseSpec(projectId, alignedFeatureId, formulaId, page, size, sort, optFields);
    }

    /**
     * @deprecated renamed to {@link #getDeNovoStructureCandidatesByFormulaPage(String, String, String, Integer, Integer, List<String>, List<StructureCandidateOptField>)}.
     * Will be removed in the next major release.
     */
    @Deprecated(forRemoval = true)
    public PagedModelStructureCandidateScored getDeNovoStructureCandidatesByFormulaPaged(
            @jakarta.annotation.Nonnull String projectId,
            @jakarta.annotation.Nonnull String alignedFeatureId,
            @jakarta.annotation.Nonnull String formulaId,
            @jakarta.annotation.Nullable Integer page,
            @jakarta.annotation.Nullable Integer size,
            @jakarta.annotation.Nullable List<String> sort,
            @jakarta.annotation.Nullable List<StructureCandidateOptField> optFields
    ) throws WebClientResponseException {
        return getDeNovoStructureCandidatesByFormulaPage(projectId, alignedFeatureId, formulaId, page, size, sort, optFields);
    }

    /**
     * @deprecated renamed to {@link #getDeNovoStructureCandidatesByFormulaPageWithHttpInfo(String, String, String, Integer, Integer, List<String>, List<StructureCandidateOptField>)}.
     * Will be removed in the next major release.
     */
    @Deprecated(forRemoval = true)
    public ResponseEntity<PagedModelStructureCandidateScored> getDeNovoStructureCandidatesByFormulaPagedWithHttpInfo(
            @jakarta.annotation.Nonnull String projectId,
            @jakarta.annotation.Nonnull String alignedFeatureId,
            @jakarta.annotation.Nonnull String formulaId,
            @jakarta.annotation.Nullable Integer page,
            @jakarta.annotation.Nullable Integer size,
            @jakarta.annotation.Nullable List<String> sort,
            @jakarta.annotation.Nullable List<StructureCandidateOptField> optFields
    ) throws WebClientResponseException {
        return getDeNovoStructureCandidatesByFormulaPageWithHttpInfo(projectId, alignedFeatureId, formulaId, page, size, sort, optFields);
    }

    /**
     * @deprecated renamed to {@link #getDeNovoStructureCandidatesByFormulaPageWithResponseSpec(String, String, String, Integer, Integer, List<String>, List<StructureCandidateOptField>)}.
     * Will be removed in the next major release.
     */
    @Deprecated(forRemoval = true)
    public ResponseSpec getDeNovoStructureCandidatesByFormulaPagedWithResponseSpec(
            @jakarta.annotation.Nonnull String projectId,
            @jakarta.annotation.Nonnull String alignedFeatureId,
            @jakarta.annotation.Nonnull String formulaId,
            @jakarta.annotation.Nullable Integer page,
            @jakarta.annotation.Nullable Integer size,
            @jakarta.annotation.Nullable List<String> sort,
            @jakarta.annotation.Nullable List<StructureCandidateOptField> optFields
    ) throws WebClientResponseException {
        return getDeNovoStructureCandidatesByFormulaPageWithResponseSpec(projectId, alignedFeatureId, formulaId, page, size, sort, optFields);
    }

    /**
     * @deprecated renamed to {@link #getSpectralLibraryMatchesPage(String, String, Integer, Integer, List<String>, Integer, Double, String, List<SpectralLibraryMatchOptField>)}.
     * Will be removed in the next major release.
     */
    @Deprecated(forRemoval = true)
    public PagedModelSpectralLibraryMatch getSpectralLibraryMatchesPaged(
            @jakarta.annotation.Nonnull String projectId,
            @jakarta.annotation.Nonnull String alignedFeatureId,
            @jakarta.annotation.Nullable Integer page,
            @jakarta.annotation.Nullable Integer size,
            @jakarta.annotation.Nullable List<String> sort,
            @jakarta.annotation.Nullable Integer minSharedPeaks,
            @jakarta.annotation.Nullable Double minSimilarity,
            @jakarta.annotation.Nullable String inchiKey,
            @jakarta.annotation.Nullable List<SpectralLibraryMatchOptField> optFields
    ) throws WebClientResponseException {
        return getSpectralLibraryMatchesPage(projectId, alignedFeatureId, page, size, sort, minSharedPeaks, minSimilarity, inchiKey, optFields);
    }

    /**
     * @deprecated renamed to {@link #getSpectralLibraryMatchesPageWithHttpInfo(String, String, Integer, Integer, List<String>, Integer, Double, String, List<SpectralLibraryMatchOptField>)}.
     * Will be removed in the next major release.
     */
    @Deprecated(forRemoval = true)
    public ResponseEntity<PagedModelSpectralLibraryMatch> getSpectralLibraryMatchesPagedWithHttpInfo(
            @jakarta.annotation.Nonnull String projectId,
            @jakarta.annotation.Nonnull String alignedFeatureId,
            @jakarta.annotation.Nullable Integer page,
            @jakarta.annotation.Nullable Integer size,
            @jakarta.annotation.Nullable List<String> sort,
            @jakarta.annotation.Nullable Integer minSharedPeaks,
            @jakarta.annotation.Nullable Double minSimilarity,
            @jakarta.annotation.Nullable String inchiKey,
            @jakarta.annotation.Nullable List<SpectralLibraryMatchOptField> optFields
    ) throws WebClientResponseException {
        return getSpectralLibraryMatchesPageWithHttpInfo(projectId, alignedFeatureId, page, size, sort, minSharedPeaks, minSimilarity, inchiKey, optFields);
    }

    /**
     * @deprecated renamed to {@link #getSpectralLibraryMatchesPageWithResponseSpec(String, String, Integer, Integer, List<String>, Integer, Double, String, List<SpectralLibraryMatchOptField>)}.
     * Will be removed in the next major release.
     */
    @Deprecated(forRemoval = true)
    public ResponseSpec getSpectralLibraryMatchesPagedWithResponseSpec(
            @jakarta.annotation.Nonnull String projectId,
            @jakarta.annotation.Nonnull String alignedFeatureId,
            @jakarta.annotation.Nullable Integer page,
            @jakarta.annotation.Nullable Integer size,
            @jakarta.annotation.Nullable List<String> sort,
            @jakarta.annotation.Nullable Integer minSharedPeaks,
            @jakarta.annotation.Nullable Double minSimilarity,
            @jakarta.annotation.Nullable String inchiKey,
            @jakarta.annotation.Nullable List<SpectralLibraryMatchOptField> optFields
    ) throws WebClientResponseException {
        return getSpectralLibraryMatchesPageWithResponseSpec(projectId, alignedFeatureId, page, size, sort, minSharedPeaks, minSimilarity, inchiKey, optFields);
    }
}
