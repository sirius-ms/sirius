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

import de.unijena.bioinf.ms.middleware.model.features.AlignedFeature;
import de.unijena.bioinf.ms.middleware.model.features.Run;
import de.unijena.bioinf.ms.middleware.model.search.SearchableField;
import de.unijena.bioinf.ms.middleware.service.search.mappers.GenericPojoMapper;
import de.unijena.bioinf.ms.middleware.service.search.description.ApiDocFieldDescriptions;
import de.unijena.bioinf.ms.middleware.service.search.description.SearchableFieldDescriber;
import de.unijena.bioinf.ms.middleware.service.search.mappers.FieldMapper;
import de.unijena.bioinf.ms.middleware.service.search.mappers.IndexFieldWithMapper;
import de.unijena.bioinf.ms.middleware.service.search.mappers.IndexSchema;
import de.unijena.bioinf.ms.middleware.service.search.mappers.LuceneMappingUtils;
import de.unijena.bioinf.ms.middleware.service.search.mappers.TagMapper;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueType;
import de.unijena.bioinf.projectspace.IndexField;
import io.swagger.v3.oas.annotations.media.Schema;
import de.unijena.bioinf.projectspace.QueryRewriter;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.queryparser.flexible.standard.config.PointsConfig;
import org.apache.lucene.search.SortField;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the searchable-field introspection ({@link GenericPojoMapper#describeSearchableFields()}) that backs
 * the searchable-fields API endpoint. Users need this metadata to design their lucene queries.
 */
public class SearchableFieldsDescriptionTest {

    public enum TestQuality {GOOD, DECENT, BAD}

    public static class TestNested {
        @IndexField(sortable = true)
        public Double score;
    }

    public static class TestPojo {
        @IndexField(documentId = true, sortable = true, defaultSearchField = true)
        public String id;

        @IndexField(fullTextSearch = true, defaultSearchField = true, sortable = true)
        public String name;

        @IndexField
        public String adduct;

        /**
         * Javadoc that must NOT win over the explicit schema annotation.
         */
        @IndexField(name = "mz", sortable = true)
        @Schema(description = "Precursor mass to charge ratio (m/z)")
        public Double ionMass;

        /**
         * Summed intensity of the feature across all runs,
         * given as {@code apexIntensity} of the merged trace.
         * <p>
         * Zero &amp; negative intensities are filtered beforehand.
         */
        @IndexField
        public Double summedIntensity;

        @IndexField
        public int charge;

        @IndexField
        public boolean hasMs1;

        @IndexField
        public TestQuality quality;

        @IndexField
        public Date measuredAt;

        @IndexField
        public List<String> detectedAdducts;

        @IndexField
        public Map<String, Integer> peakCounts;

        @IndexField
        public TestNested annotation;

        // not annotated, must not be searchable
        public String comment;
    }

    private static Map<String, SearchableField> describeAsMap(Class<?> pojoClass) {
        return DescribedFields.asMap(pojoClass);
    }

    @Test
    public void testFieldNamesAndUnannotatedFieldsExcluded() {
        Map<String, SearchableField> fields = describeAsMap(TestPojo.class);

        assertEquals(Set.of("id", "name", "adduct", "mz", "summedIntensity", "charge", "hasMs1", "quality",
                        "measuredAt", "detectedAdducts", "peakCounts.*", "annotation.score"),
                fields.keySet());
    }

    @Test
    public void testTextFields() {
        Map<String, SearchableField> fields = describeAsMap(TestPojo.class);

        SearchableField id = fields.get("id");
        assertEquals(SearchableField.FieldType.TEXT, id.getFieldType());
        assertFalse(id.isFullTextSearch());
        assertTrue(id.isSortable());
        assertTrue(id.isDefaultSearchField());

        SearchableField name = fields.get("name");
        assertEquals(SearchableField.FieldType.TEXT, name.getFieldType());
        assertTrue(name.isFullTextSearch());
        assertTrue(name.isSortable());
        assertTrue(name.isDefaultSearchField());

        SearchableField adduct = fields.get("adduct");
        assertEquals(SearchableField.FieldType.TEXT, adduct.getFieldType());
        assertFalse(adduct.isFullTextSearch());
        assertFalse(adduct.isSortable());
        assertFalse(adduct.isDefaultSearchField());
    }

    @Test
    public void testNumericBooleanAndDateFields() {
        Map<String, SearchableField> fields = describeAsMap(TestPojo.class);

        // renamed via @IndexField(name = "mz")
        SearchableField mz = fields.get("mz");
        assertEquals(SearchableField.FieldType.DOUBLE, mz.getFieldType());
        assertTrue(mz.isSortable());
        assertFalse(mz.isFullTextSearch());

        assertEquals(SearchableField.FieldType.INTEGER, fields.get("charge").getFieldType());
        assertEquals(SearchableField.FieldType.BOOLEAN, fields.get("hasMs1").getFieldType());
        assertEquals(SearchableField.FieldType.DATE, fields.get("measuredAt").getFieldType());
    }

    private static Map<String, SearchableField> describeWithApiDocs(Class<?> pojoClass) {
        return DescribedFields.asMap(pojoClass, ApiDocFieldDescriptions.PROVIDER);
    }

    /**
     * The lucene machinery is framework neutral: field descriptions (OpenAPI annotations/javadoc) are not read
     * by the machinery itself but provided by the REST layer via an injected description provider.
     */
    @Test
    public void testDescriptionsRequireInjectedProvider() {
        // without a provider there are no descriptions, even though the fields are documented
        assertNull(describeAsMap(TestPojo.class).get("mz").getDescription());
        assertNull(describeAsMap(TestPojo.class).get("summedIntensity").getDescription());
    }

    /**
     * An explicit schema annotation wins over field javadoc - same precedence as the OpenAPI documentation.
     */
    @Test
    public void testSchemaAnnotationWinsOverJavadoc() {
        assertEquals("Precursor mass to charge ratio (m/z)", describeWithApiDocs(TestPojo.class).get("mz").getDescription());
    }

    /**
     * Without a schema annotation the description is the field javadoc (as in the OpenAPI documentation),
     * normalized to plain text: source line wrapping is joined, {@code ...} and HTML entities are resolved,
     * and only deliberate paragraph breaks remain (as newline).
     */
    @Test
    public void testJavadocDescriptionsAreNormalizedPlainText() {
        assertEquals("Summed intensity of the feature across all runs, given as apexIntensity of the merged trace.\n"
                        + "Zero & negative intensities are filtered beforehand.",
                describeWithApiDocs(TestPojo.class).get("summedIntensity").getDescription());
    }

    /**
     * The descriptions of the real API models come from their field javadoc - the same text the OpenAPI
     * documentation shows.
     */
    @Test
    public void testRealModelDescriptionsMatchApiDoc() {
        assertEquals("Informative, human-readable name of this run",
                describeWithApiDocs(Run.class).get("name").getDescription());
    }

    @Test
    public void testEnumFieldListsPossibleValues() {
        SearchableField quality = describeAsMap(TestPojo.class).get("quality");

        assertEquals(SearchableField.FieldType.ENUM, quality.getFieldType());
        assertEquals(List.of("GOOD", "DECENT", "BAD"), quality.getPossibleValues());
    }

    @Test
    public void testCollectionMapAndNestedFields() {
        Map<String, SearchableField> fields = describeAsMap(TestPojo.class);

        // collections are searchable like single-valued fields of their element type
        assertEquals(SearchableField.FieldType.TEXT, fields.get("detectedAdducts").getFieldType());

        // maps are searchable per key: <fieldName>.<key>
        assertEquals(SearchableField.FieldType.INTEGER, fields.get("peakCounts.*").getFieldType());

        // nested objects are searchable via dot notation
        SearchableField nestedScore = fields.get("annotation.score");
        assertEquals(SearchableField.FieldType.DOUBLE, nestedScore.getFieldType());
        assertTrue(nestedScore.isSortable());
    }

    @Test
    public void testSpotChecksOnRealApiModels() {
        Map<String, SearchableField> features = describeAsMap(AlignedFeature.class);

        SearchableField ionMass = features.get("ionMass");
        assertNotNull(ionMass);
        assertEquals(SearchableField.FieldType.DOUBLE, ionMass.getFieldType());
        assertTrue(ionMass.isSortable());

        SearchableField featureName = features.get("name");
        assertNotNull(featureName);
        assertTrue(featureName.isFullTextSearch());
        assertTrue(featureName.isDefaultSearchField());

        // nested annotation fields
        assertNotNull(features.get("topAnnotations.structureAnnotation.inchiKey"));
        // mapper-derived fields (CompoundClassesMapper) must show up as well
        SearchableField cfClass = features.get("topAnnotations.compoundClassAnnotation.cfClass");
        assertNotNull(cfClass);
        assertEquals(SearchableField.FieldType.TEXT, cfClass.getFieldType());
        assertTrue(cfClass.isFullTextSearch());
        assertTrue(cfClass.isDefaultSearchField());
        // mapper-derived formula element counts, e.g. molecularFormula.C:6
        assertNotNull(features.get("topAnnotations.formulaAnnotation.molecularFormula.*"));

        Map<String, SearchableField> runs = describeAsMap(Run.class);
        assertNotNull(runs.get("runId"));
        assertTrue(runs.get("name").isFullTextSearch());
    }

    /**
     * A mapper may register a field with BOTH a points config and an analyzer (as the index manager does for
     * dynamic tag fields). It must be described once, as the numeric field it is.
     */
    public static class DualConfigMapper implements FieldMapper<String> {
        @Override
        public Iterable<org.apache.lucene.index.IndexableField> toIndexableFields(String rootFieldName, String pojo) {
            return List.of();
        }

        @Override
        public String toPojo(String rootFieldName, Iterable<org.apache.lucene.index.IndexableField> document) {
            return null;
        }

        @Override
        public void applyAnalyzersAndPointConfigs(String rootFieldName, Map<String, PointsConfig> pointsConfigMap,
                                                  Map<String, Analyzer> analyzerMap, List<CharSequence> defaultSearchFields,
                                                  Map<String, SortField.Type> sortTypes,
                                                  Map<String, QueryRewriter> queryRewriters) {
            pointsConfigMap.put(rootFieldName + ".dual", LuceneMappingUtils.getPointsConfigForType(Double.class));
            analyzerMap.put(rootFieldName + ".dual", new KeywordAnalyzer());
        }
    }

    public static class DualConfigPojo {
        @IndexField(documentId = true)
        public String id;

        @IndexFieldWithMapper(mapper = DualConfigMapper.class)
        public String stats;
    }

    @Test
    public void testFieldInBothConfigMapsIsDescribedOnce() {
        List<SearchableField> fields = DescribedFields.of(DualConfigPojo.class).stream()
                .filter(field -> field.getName().startsWith("stats."))
                .toList();

        assertEquals(1, fields.size());
        assertEquals("stats.dual", fields.get(0).getName());
        assertEquals(SearchableField.FieldType.DOUBLE, fields.get(0).getFieldType());
        assertFalse(fields.get(0).isFullTextSearch(), "a numeric field is not searched word by word");
    }

    /**
     * Everything the query parser is configured with has to be described, and with the same flags. One walk
     * produces both now, so this guards the describer against silently dropping or relabelling a field.
     */
    @Test
    public void testDescriptionIsConsistentWithQueryParserConfiguration() {
        for (Class<?> pojoClass : List.of(AlignedFeature.class, Run.class, TestPojo.class)) {
            Map<String, PointsConfig> pointsConfigMap = new HashMap<>();
            Map<String, Analyzer> analyzerMap = new HashMap<>();
            List<CharSequence> defaultSearchFields = new java.util.ArrayList<>();
            Map<String, SortField.Type> sortTypes = new HashMap<>();
            IndexSchema schema = new GenericPojoMapper<>(pojoClass, new TagMapper(tagName -> ValueType.TEXT))
                    .detectAnalyzersAndPointConfigs(pointsConfigMap, analyzerMap, defaultSearchFields, sortTypes, new HashMap<>());

            Map<String, SearchableField> described = new SearchableFieldDescriber().describe(schema).stream()
                    .collect(Collectors.toMap(SearchableField::getName, Function.identity()));

            Set<String> configuredFields = new HashSet<>();
            configuredFields.addAll(pointsConfigMap.keySet());
            configuredFields.addAll(analyzerMap.keySet());
            assertEquals(configuredFields, described.keySet(),
                    "described fields diverge from query parser configuration for " + pojoClass.getSimpleName());

            assertEquals(defaultSearchFields.stream().map(CharSequence::toString).collect(Collectors.toSet()),
                    described.values().stream().filter(SearchableField::isDefaultSearchField)
                            .map(SearchableField::getName).collect(Collectors.toSet()),
                    "default search fields diverge for " + pojoClass.getSimpleName());

            assertEquals(sortTypes.keySet(),
                    described.values().stream().filter(SearchableField::isSortable)
                            .map(SearchableField::getName).collect(Collectors.toSet()),
                    "sortable fields diverge for " + pojoClass.getSimpleName());
        }
    }
}
