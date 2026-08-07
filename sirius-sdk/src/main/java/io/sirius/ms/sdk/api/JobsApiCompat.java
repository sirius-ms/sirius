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
import io.sirius.ms.sdk.model.JobOptField;
import io.sirius.ms.sdk.model.PagedModelJob;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;

/**
 * Adds the deprecated pre-rename method names to the generated {@link JobsApi}.
 * <p>
 * The {@code getJobsPaged*} operationIds were a naming mistake on a publicly facing method name and have been
 * corrected to {@code getJobsPage*}. The HTTP route ({@code GET /api/projects/{projectId}/jobs/page}) is unchanged,
 * so this is purely a source-level compatibility shim: existing call sites keep compiling for one release while they
 * migrate to the corrected names.
 * <p>
 * These aliases are scheduled for removal in the next major release. They are hand written because everything under
 * {@code sirius-sdk.openapi} is overwritten by {@code openApiGenerate}.
 */
public class JobsApiCompat extends JobsApi {

    public JobsApiCompat(ApiClient apiClient) {
        super(apiClient);
    }

    /**
     * @deprecated renamed to {@link #getJobsPage(String, Integer, Integer, List, List)}.
     * Will be removed in the next major release.
     */
    @Deprecated(forRemoval = true)
    public PagedModelJob getJobsPaged(
            @jakarta.annotation.Nonnull String projectId,
            @jakarta.annotation.Nullable Integer page,
            @jakarta.annotation.Nullable Integer size,
            @jakarta.annotation.Nullable List<String> sort,
            @jakarta.annotation.Nullable List<JobOptField> optFields
    ) throws WebClientResponseException {
        return getJobsPage(projectId, page, size, sort, optFields);
    }

    /**
     * @deprecated renamed to {@link #getJobsPageWithHttpInfo(String, Integer, Integer, List, List)}.
     * Will be removed in the next major release.
     */
    @Deprecated(forRemoval = true)
    public ResponseEntity<PagedModelJob> getJobsPagedWithHttpInfo(
            @jakarta.annotation.Nonnull String projectId,
            @jakarta.annotation.Nullable Integer page,
            @jakarta.annotation.Nullable Integer size,
            @jakarta.annotation.Nullable List<String> sort,
            @jakarta.annotation.Nullable List<JobOptField> optFields
    ) throws WebClientResponseException {
        return getJobsPageWithHttpInfo(projectId, page, size, sort, optFields);
    }

    /**
     * @deprecated renamed to {@link #getJobsPageWithResponseSpec(String, Integer, Integer, List, List)}.
     * Will be removed in the next major release.
     */
    @Deprecated(forRemoval = true)
    public ResponseSpec getJobsPagedWithResponseSpec(
            @jakarta.annotation.Nonnull String projectId,
            @jakarta.annotation.Nullable Integer page,
            @jakarta.annotation.Nullable Integer size,
            @jakarta.annotation.Nullable List<String> sort,
            @jakarta.annotation.Nullable List<JobOptField> optFields
    ) throws WebClientResponseException {
        return getJobsPageWithResponseSpec(projectId, page, size, sort, optFields);
    }
}
