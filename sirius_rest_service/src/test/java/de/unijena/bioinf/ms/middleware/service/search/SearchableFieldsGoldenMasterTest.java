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

package de.unijena.bioinf.ms.middleware.service.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.unijena.bioinf.ChemistryBase.utils.DataQuality;
import de.unijena.bioinf.ms.middleware.model.annotations.FeatureAnnotations;
import de.unijena.bioinf.ms.middleware.model.annotations.FormulaCandidate;
import de.unijena.bioinf.ms.middleware.model.features.AlignedFeature;
import de.unijena.bioinf.ms.middleware.model.features.Run;
import de.unijena.bioinf.ms.middleware.model.search.SearchableField;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.DetectedAdductPossibleValues;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.PerPojoSearchContext;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.TagDefinitionPossibleValues;
import de.unijena.bioinf.ms.persistence.model.core.tags.TagDefinition;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueDefinition;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueType;
import de.unijena.bioinf.ms.middleware.service.search.description.FieldVocabulary;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Pins the complete searchable-field description of the API models against a checked-in fixture.
 * <p>
 * The description is what clients build their queries from - the SDK, and the GUI search bar that offers fields
 * and values - so it is public behaviour, not an implementation detail. This test exists so that moving the
 * description out of the search engine can be shown to change nothing: every field name, type, flag, vocabulary
 * and description text has to come out exactly as before.
 * <p>
 * Vocabularies are recorded as "&lt;count&gt; values sha256:&lt;digest&gt;" once they get long, so that the whole
 * ClassyFire ontology does not end up in the fixture while still being pinned exactly.
 * <p>
 * Run with {@code -Dsearchable.golden.update=true} to rewrite the fixture after a deliberate change, then read
 * the diff before committing it.
 */
public class SearchableFieldsGoldenMasterTest {

    private static final String FIXTURE = "/golden/searchable-fields.json";
    private static final Path FIXTURE_SOURCE = Path.of("src/test/resources/golden/searchable-fields.json");
    private static final int INLINE_VALUE_LIMIT = 8;

    /**
     * The project state a description depends on: tag definitions of every value type, some with a restricted
     * vocabulary, and a set of detected adducts.
     */
    private static PerPojoSearchContext projectContext() {
        Map<String, ValueType> tagValueTypes = new TreeMap<>(Map.of(
                "sampleType", ValueType.TEXT,
                "comment", ValueType.TEXT,
                "concentration", ValueType.REAL,
                "replicate", ValueType.INTEGER,
                "measured", ValueType.DATE,
                "injectedAt", ValueType.TIME,
                "isBlank", ValueType.BOOLEAN,
                "pfas", ValueType.NONE));

        Map<String, TagDefinition> definitions = Map.of(
                "sampleType", tagDefinition("sampleType", ValueType.TEXT, List.of("Sample", "Blank", "Standard")),
                "comment", tagDefinition("comment", ValueType.TEXT, List.of()),
                "concentration", tagDefinition("concentration", ValueType.REAL, List.of()),
                "replicate", tagDefinition("replicate", ValueType.INTEGER, List.of(1, 2, 3)),
                "measured", tagDefinition("measured", ValueType.DATE, List.of(0L)),
                "injectedAt", tagDefinition("injectedAt", ValueType.TIME, List.of()),
                "isBlank", tagDefinition("isBlank", ValueType.BOOLEAN, List.of()),
                "pfas", tagDefinition("pfas", ValueType.NONE, List.of()));

        Set<String> detectedAdducts = Set.of("[M + H]+", "[M + Na]+", "[M - H]-");

        return new PerPojoSearchContext(null, tagValueTypes, ApiDocFieldDescriptions.PROVIDER,
                FieldVocabulary.firstOf(
                        new TagDefinitionPossibleValues(name -> Optional.ofNullable(definitions.get(name))),
                        new DetectedAdductPossibleValues(() -> detectedAdducts)));
    }

    private static TagDefinition tagDefinition(String tagName, ValueType valueType, List<?> possibleValues) {
        return TagDefinition.builder()
                .tagName(tagName)
                .tagType("TEST")
                .valueDefinition(new ValueDefinition<>(valueType, possibleValues, null, null))
                .build();
    }

    /**
     * Documents that materialize the dynamic-key fields, which are only described once a key is in the index:
     * quality categories, matched databases and the elements of a molecular formula.
     */
    private static void indexDocuments(PerPojoSearchContext context) {
        context.addDocument(AlignedFeature.builder()
                .alignedFeatureId("1")
                .name("caffeine")
                .ionMass(195.0877)
                .charge(1)
                .detectedAdducts(Set.of("[M + H]+"))
                .quality(DataQuality.GOOD)
                .qualities(Map.of("peakShape", DataQuality.GOOD, "isotopePattern", DataQuality.DECENT))
                .topAnnotations(FeatureAnnotations.builder()
                        .formulaAnnotation(FormulaCandidate.builder().molecularFormula("C8H10N4O2").build())
                        .matchedDatabases(Map.of("GNPS", 1, "PubChem", 3))
                        .build())
                .build());

        context.addDocument(Run.builder()
                .runId("1")
                .name("blank-01")
                .chromatography("Liquid Chromatography")
                .build());
    }

    @Test
    public void testDescribedFieldsMatchTheFixture() throws IOException {
        String actual;
        try (PerPojoSearchContext context = projectContext()) {
            indexDocuments(context);
            actual = describeAll(context);
        }

        if (updateRequested()) {
            Files.createDirectories(FIXTURE_SOURCE.getParent());
            Files.writeString(FIXTURE_SOURCE, actual, StandardCharsets.UTF_8);
        }

        assertEquals(readFixture(), actual,
                "The searchable-field description changed. If that is intended, rerun with "
                        + "SEARCHABLE_GOLDEN_UPDATE=true and review the diff.");
    }

    /**
     * Gradle does not forward its own system properties to the test JVM, so the environment variable is the one
     * that works from the command line: {@code SEARCHABLE_GOLDEN_UPDATE=true ./gradlew :sirius_rest_service:test}.
     */
    private static boolean updateRequested() {
        return Boolean.getBoolean("searchable.golden.update")
                || Boolean.parseBoolean(System.getenv("SEARCHABLE_GOLDEN_UPDATE"));
    }

    private static String describeAll(PerPojoSearchContext context) throws IOException {
        Map<String, Object> described = new LinkedHashMap<>();
        described.put("AlignedFeature", describe(context, AlignedFeature.class));
        described.put("Run", describe(context, Run.class));
        return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(described) + "\n";
    }

    private static List<Map<String, Object>> describe(PerPojoSearchContext context, Class<?> modelClass) {
        return context.getSearchableFields(modelClass).stream()
                .map(SearchableFieldsGoldenMasterTest::canonical)
                .toList();
    }

    /**
     * One field as the fixture records it. Every property of the DTO is kept; only a long vocabulary is
     * summarized, since a checked-in copy of the ClassyFire ontology would drown the fixture.
     */
    private static Map<String, Object> canonical(SearchableField field) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("name", field.getName());
        entry.put("fieldType", String.valueOf(field.getFieldType()));
        entry.put("fullTextSearch", field.isFullTextSearch());
        entry.put("sortable", field.isSortable());
        entry.put("defaultSearchField", field.isDefaultSearchField());
        entry.put("significantSuffixLength", field.getSignificantSuffixLength());
        entry.put("possibleValues", summarize(field.getPossibleValues()));
        entry.put("description", field.getDescription());
        return entry;
    }

    private static Object summarize(List<String> possibleValues) {
        if (possibleValues == null)
            return null;
        if (possibleValues.size() <= INLINE_VALUE_LIMIT)
            return possibleValues;
        return possibleValues.size() + " values sha256:" + sha256(String.join(" ", possibleValues));
    }

    private static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String readFixture() throws IOException {
        try (InputStream in = SearchableFieldsGoldenMasterTest.class.getResourceAsStream(FIXTURE)) {
            assertNotNull(in, "missing fixture " + FIXTURE + " - create it with SEARCHABLE_GOLDEN_UPDATE=true");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
