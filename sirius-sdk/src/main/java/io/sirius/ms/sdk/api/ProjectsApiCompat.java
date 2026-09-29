/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2026 Bright Giant GmbH
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Adds the deprecated pre-{@code compactFull} method signatures to the generated {@link ProjectsApi}.
 * <p>
 * {@code closeProject} gained a third parameter when the repairing compaction was added, which replaced the
 * two-argument form rather than joining it. The HTTP route is unchanged and the new query parameter is optional
 * with a default, so nothing about the wire contract broke - this is purely a source-level compatibility shim so
 * existing Java call sites keep compiling for one release while they migrate.
 * <p>
 * These aliases are scheduled for removal in the next major release. They are hand written because everything under
 * {@code sirius-sdk.openapi} is overwritten by {@code openApiGenerate}.
 */
public class ProjectsApiCompat extends ProjectsApi {

    public ProjectsApiCompat(ApiClient apiClient) {
        super(apiClient);
    }

    /**
     * @deprecated gained a {@code compactFull} parameter, see
     * {@link #closeProject(String, Boolean, Boolean)}. Will be removed in the next major release.
     */
    @Deprecated(forRemoval = true)
    public void closeProject(
            @jakarta.annotation.Nonnull String projectId,
            @jakarta.annotation.Nullable Boolean compact
    ) throws WebClientResponseException {
        closeProject(projectId, compact, false);
    }

    /**
     * @deprecated gained a {@code compactFull} parameter, see
     * {@link #closeProjectWithHttpInfo(String, Boolean, Boolean)}. Will be removed in the next major release.
     */
    @Deprecated(forRemoval = true)
    public ResponseEntity<Void> closeProjectWithHttpInfo(
            @jakarta.annotation.Nonnull String projectId,
            @jakarta.annotation.Nullable Boolean compact
    ) throws WebClientResponseException {
        return closeProjectWithHttpInfo(projectId, compact, false);
    }

    /**
     * @deprecated gained a {@code compactFull} parameter, see
     * {@link #closeProjectWithResponseSpec(String, Boolean, Boolean)}. Will be removed in the next major release.
     */
    @Deprecated(forRemoval = true)
    public ResponseSpec closeProjectWithResponseSpec(
            @jakarta.annotation.Nonnull String projectId,
            @jakarta.annotation.Nullable Boolean compact
    ) throws WebClientResponseException {
        return closeProjectWithResponseSpec(projectId, compact, false);
    }
}
