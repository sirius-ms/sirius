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

import io.sirius.ms.sdk.SiriusClient;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pins the deprecated SDK call forms the {@code *ApiCompat} classes exist to keep alive.
 * <p>
 * The generated API under {@code sirius-sdk.openapi} is overwritten by {@code openApiGenerate}, and a
 * parameter added to an existing operation replaces the previous method signature rather than joining it -
 * which breaks every external Java caller even though the HTTP route stays compatible. The compat classes
 * put the old form back; without something holding on to them, the next regeneration quietly drops the
 * shim and nothing goes red until a user upgrades.
 * <p>
 * {@code ApiBackwardCompatibilityTest} does not cover this: it asserts against the committed OpenAPI spec,
 * which says nothing about Java signatures.
 */
public class SdkSourceCompatibilityTest {

    /** {@link SiriusClient#projects()} has to expose the compat type, or the shim is unreachable. */
    @Test
    void projectsAccessorExposesTheCompatType() throws Exception {
        Method accessor = SiriusClient.class.getMethod("projects");
        assertEquals(ProjectsApiCompat.class, accessor.getReturnType(),
                "SiriusClient.projects() must return the compat type, otherwise its deprecated overloads "
                        + "cannot be reached by callers");
    }

    /** The pre-{@code compactFull} forms of closeProject, as external callers still write them. */
    @Test
    void closeProjectKeepsItsTwoArgumentForms() {
        assertDeprecated(ProjectsApiCompat.class, "closeProject", void.class);
        assertDeprecated(ProjectsApiCompat.class, "closeProjectWithHttpInfo", ResponseEntity.class);
        assertDeprecated(ProjectsApiCompat.class, "closeProjectWithResponseSpec", ResponseSpec.class);
    }

    private static void assertDeprecated(Class<?> api, String name, Class<?> returnType) {
        Method m = assertDoesNotThrow(() -> api.getMethod(name, String.class, Boolean.class),
                () -> name + "(String, Boolean) is gone - an external caller written against the previous "
                        + "release no longer compiles. Restore it as a deprecated delegate in " + api.getSimpleName());
        assertEquals(returnType, m.getReturnType(), () -> name + " must keep its return type");
        assertTrue(m.isAnnotationPresent(Deprecated.class),
                () -> name + "(String, Boolean) must be deprecated so callers are told to migrate");
    }
}
