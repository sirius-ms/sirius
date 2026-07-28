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
import io.sirius.ms.sdk.model.PagedModelAlignedFeature;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;

/**
 * Adds the deprecated pre-rename method names to the generated {@link FeaturesApi}.
 * <p>
 * The {@code getAlignedFeaturesPaged*} operationIds were a naming mistake on a publicly facing method name and have
 * been corrected to {@code getAlignedFeaturesPage*}. The HTTP route ({@code GET /api/projects/{projectId}/aligned-features/page})
 * is unchanged, so this is purely a source-level compatibility shim: existing call sites keep compiling for one
 * release while they migrate to the corrected names.
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
}
