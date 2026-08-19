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

import de.unijena.bioinf.ChemistryBase.utils.DataQuality;
import de.unijena.bioinf.ms.middleware.model.compounds.Compound;
import de.unijena.bioinf.ms.middleware.model.features.AlignedFeature;
import de.unijena.bioinf.ms.middleware.model.features.Run;
import de.unijena.bioinf.ms.middleware.model.search.SearchableField;
import de.unijena.bioinf.ms.middleware.service.projects.Project;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.PerPojoSearchContext;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.SearchServiceImpl;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import de.unijena.bioinf.ms.middleware.service.search.description.IndexFacts;
import de.unijena.bioinf.ms.middleware.service.search.description.SearchableFieldService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the searchable-fields lookup through the {@link SearchService}, including per-project dynamic tag
 * fields and the empty answer for objects without a search index.
 */
public class SearchServiceSearchableFieldsTest {

    private SearchService searchService;
    private Project<?> mockProject;
    private final String projectId = "test-project";

    @BeforeEach
    public void setup() throws IOException {
        mockProject = Mockito.mock(Project.class);
        Mockito.when(mockProject.getProjectId()).thenReturn(projectId);
        Mockito.when(mockProject.getSystemUID()).thenReturn("test-system-uid");

        // in-memory index; one tag definition already exists when the project is opened
        searchService = new SearchServiceImpl(project ->
                new PerPojoSearchContext(null, new HashMap<>(Map.of("sampleType", ValueType.TEXT))));
        searchService.openOrCreateProjectIndex(mockProject);
    }

    @AfterEach
    public void cleanup() throws IOException {
        searchService.closeProjectIndex(mockProject, true);
    }

    /** Describing is done outside the search engine, over the facts it reports. */
    private SearchableFieldService searchableFields() {
        return new SearchableFieldService(IndexFacts.of(searchService, projectId), null);
    }

    private Map<String, SearchableField> fieldsAsMap(Class<?> pojoClass) {
        return searchableFields().describe(pojoClass).stream()
                .collect(Collectors.toMap(SearchableField::getName, Function.identity()));
    }

    @Test
    public void testStaticModelFieldsAreListed() {
        Map<String, SearchableField> features = fieldsAsMap(AlignedFeature.class);
        assertEquals(SearchableField.FieldType.DOUBLE, features.get("ionMass").getFieldType());
        assertTrue(features.get("name").isFullTextSearch());

        Map<String, SearchableField> runs = fieldsAsMap(Run.class);
        assertNotNull(runs.get("runId"));
    }

    @Test
    public void testTagFieldsOfTheProjectAreListed() {
        // tag definition present at project open
        SearchableField initialTag = fieldsAsMap(Run.class).get("tags.sampleType");
        assertNotNull(initialTag, "tag definitions present at project open must be searchable");
        assertEquals(SearchableField.FieldType.TEXT, initialTag.getFieldType());
        assertTrue(initialTag.isFullTextSearch());

        // newly added tag definitions show up ...
        searchService.addTagValueType(projectId, "concentration", ValueType.REAL);
        SearchableField concentration = fieldsAsMap(Run.class).get("tags.concentration");
        assertNotNull(concentration, "newly added tag definitions must become searchable");
        assertEquals(SearchableField.FieldType.DOUBLE, concentration.getFieldType());
        assertFalse(concentration.isFullTextSearch());

        // tag fields are reported in deterministic (alphabetical) order
        List<String> tagFieldNames = searchableFields().describe(Run.class).stream()
                .map(SearchableField::getName)
                .filter(name -> name.startsWith("tags."))
                .toList();
        assertEquals(List.of("tags.concentration", "tags.sampleType"), tagFieldNames);

        // ... and disappear again when removed
        searchService.removeTagValueType(projectId, "concentration");
        assertNull(fieldsAsMap(Run.class).get("tags.concentration"),
                "removed tag definitions must no longer be searchable");
    }

    @Test
    public void testDynamicKeyFieldsAreExpandedToConcreteIndexedKeys() {
        // before any document is indexed, the dynamic-key template must NOT be reported as a bare ".*"
        assertNull(fieldsAsMap(AlignedFeature.class).get("qualities.*"),
                "the un-queryable 'qualities.*' template must not be exposed");

        // index a feature carrying a concrete quality-category key
        searchService.addDocument(projectId, AlignedFeature.builder()
                .alignedFeatureId("1")
                .qualities(Map.of("peakShape", DataQuality.GOOD))
                .build());

        Map<String, SearchableField> features = fieldsAsMap(AlignedFeature.class);
        // the materialized key is now offered as a concrete, directly-queryable field ...
        SearchableField concrete = features.get("qualities.peakShape");
        assertNotNull(concrete, "the concrete quality-category key present in the index must be searchable");
        assertEquals(SearchableField.FieldType.ENUM, concrete.getFieldType());
        assertNotNull(concrete.getPossibleValues(), "enum key domain (DataQuality values) must be carried over");
        assertFalse(concrete.getPossibleValues().isEmpty());
        // ... and the ".*" template is gone
        assertNull(features.get("qualities.*"), "the '.*' template must be replaced by concrete keys");
    }

    @Test
    public void testObjectsWithoutIndexHaveNoSearchableFields() {
        // Compound has no search index (no document id field)
        List<SearchableField> compoundFields = searchableFields().describe(Compound.class);
        assertNotNull(compoundFields);
        assertTrue(compoundFields.isEmpty(), "objects without index must yield an empty field list");

        // completely unannotated classes as well
        assertTrue(searchableFields().describe(String.class).isEmpty());
    }
}
