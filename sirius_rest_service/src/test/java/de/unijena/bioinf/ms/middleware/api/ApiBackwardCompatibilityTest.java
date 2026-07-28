/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2024 Bright Giant GmbH
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

package de.unijena.bioinf.ms.middleware.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guards the stability contract of the checked-in OpenAPI specs.
 * <p>
 * The {@code operationId} of a non-experimental operation is a publicly facing name: it becomes the method name in
 * every generated SDK. Renaming or experimental-marking one silently breaks downstream users, which is exactly what
 * happened to the {@code /page} endpoints during the Lucene work. These assertions run against the committed spec so
 * such a regression fails the build instead of shipping.
 */
public class ApiBackwardCompatibilityTest {

    private static final String FEATURES_PAGE = "/api/projects/{projectId}/aligned-features/page";
    private static final String COMPOUNDS_PAGE = "/api/projects/{projectId}/compounds/page";
    private static final String FEATURES_COLLECTION = "/api/projects/{projectId}/aligned-features";
    private static final String COMPOUNDS_COLLECTION = "/api/projects/{projectId}/compounds";

    private static JsonNode enumsAsRef;
    private static JsonNode enumsAsString;

    @BeforeAll
    static void loadSpecs() throws IOException {
        enumsAsRef = load("openapi-spec-enums-as-ref.json");
        enumsAsString = load("openapi-spec-enums-as-string.json");
    }

    private static JsonNode load(String fileName) throws IOException {
        // tests run with the module dir as working dir; fall back to an explicit module-relative path
        Path path = Path.of(fileName);
        if (Files.notExists(path))
            path = Path.of("sirius_rest_service").resolve(fileName);
        assertTrue(Files.exists(path), () -> "OpenAPI spec not found: " + fileName
                + ". Regenerate it with ':sirius_rest_service:updateUnstableExclusions' and ':sirius_rest_service:generateOpenApiDocs'.");
        return new ObjectMapper().readTree(Files.readString(path));
    }

    private static JsonNode operation(JsonNode spec, String path, String method) {
        JsonNode item = spec.path("paths").path(path);
        assertFalse(item.isMissingNode(), () -> "path missing from spec: " + method.toUpperCase(Locale.ROOT) + " " + path);
        JsonNode op = item.path(method);
        assertFalse(op.isMissingNode(), () -> "operation missing from spec: " + method.toUpperCase(Locale.ROOT) + " " + path);
        return op;
    }

    private static boolean isExperimental(JsonNode op) {
        String text = op.path("summary").asText("") + op.path("description").asText("");
        return op.path("operationId").asText("").endsWith("Experimental")
                || text.toUpperCase(Locale.ROOT).contains("[EXPERIMENTAL]");
    }

    private static List<JsonNode> specs() {
        return List.of(enumsAsRef, enumsAsString);
    }

    @Test
    @DisplayName("paged endpoints keep their corrected, non-experimental operationIds")
    void pagedEndpointsUseCorrectedOperationIds() {
        for (JsonNode spec : specs()) {
            assertEquals("getAlignedFeaturesPage", operation(spec, FEATURES_PAGE, "get").path("operationId").asText());
            assertEquals("getCompoundsPage", operation(spec, COMPOUNDS_PAGE, "get").path("operationId").asText());
        }
    }

    @Test
    @DisplayName("paged endpoints are part of the stable API surface")
    void pagedEndpointsAreNotExperimentalOrDeprecated() {
        for (JsonNode spec : specs()) {
            for (String path : List.of(FEATURES_PAGE, COMPOUNDS_PAGE)) {
                JsonNode op = operation(spec, path, "get");
                assertFalse(isExperimental(op), () -> path + " must not be marked experimental: paging is a stable "
                        + "contract, only the searchQuery parameter is experimental");
                assertFalse(op.path("deprecated").asBoolean(false), () -> path + " must not be deprecated");
            }
        }
    }

    @Test
    @DisplayName("non-paginated collection endpoints are deprecated but still present")
    void collectionEndpointsAreDeprecated() {
        for (JsonNode spec : specs()) {
            for (String path : List.of(FEATURES_COLLECTION, COMPOUNDS_COLLECTION)) {
                JsonNode op = operation(spec, path, "get");
                assertTrue(op.path("deprecated").asBoolean(false),
                        () -> path + " must be marked deprecated in favour of its /page variant");
            }
        }
    }

    @Test
    @DisplayName("deprecated job-config-names endpoint is still served")
    void deprecatedJobConfigNamesIsPreserved() {
        for (JsonNode spec : specs()) {
            JsonNode op = operation(spec, "/api/job-config-names", "get");
            assertEquals("getJobConfigNames", op.path("operationId").asText());
            assertTrue(op.path("deprecated").asBoolean(false), "job-config-names must stay marked deprecated");
        }
    }

    @Test
    @DisplayName("bulk tag-write endpoints are consistently experimental")
    void bulkTagEndpointsAreExperimental() {
        for (JsonNode spec : specs()) {
            for (String owner : List.of("aligned-features", "compounds", "runs")) {
                String path = "/api/projects/{projectId}/" + owner + "/tags";
                assertTrue(isExperimental(operation(spec, path, "put")),
                        () -> path + " must be experimental like its sibling tag endpoints");
            }
        }
    }

    @Test
    @DisplayName("both spec variants expose the same set of operationIds")
    void specVariantsAgreeOnOperationIds() {
        assertEquals(operationIds(enumsAsRef), operationIds(enumsAsString),
                "enums-as-ref and enums-as-string must describe the same operations; "
                        + "a mismatch means one of them was not regenerated");
    }

    private static List<String> operationIds(JsonNode spec) {
        List<String> ids = new ArrayList<>();
        spec.path("paths").fields().forEachRemaining(pathEntry ->
                pathEntry.getValue().fields().forEachRemaining(methodEntry -> {
                    JsonNode id = methodEntry.getValue().path("operationId");
                    if (id.isTextual())
                        ids.add(id.asText());
                }));
        ids.sort(String::compareTo);
        return ids;
    }
}
