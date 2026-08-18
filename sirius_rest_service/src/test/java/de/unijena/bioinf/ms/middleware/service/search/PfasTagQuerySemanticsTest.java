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
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.middleware.service.search;

import de.unijena.bioinf.ms.middleware.model.tags.Tag;
import de.unijena.bioinf.ms.middleware.service.projects.Project;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.PerPojoSearchContext;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.SearchServiceImpl;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.Taggable;
import de.unijena.bioinf.ms.middleware.service.search.mappers.IndexFieldWithMapper;
import de.unijena.bioinf.ms.middleware.service.search.mappers.TagMapper;
import de.unijena.bioinf.ms.persistence.model.core.tags.TagDefinitions;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueType;
import de.unijena.bioinf.projectspace.IndexField;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageRequest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins the query shapes the GUI's PFAS filter emits ({@code PanelQueryNodeFactory}) against the real
 * search stack:
 * <ul>
 *     <li>"has a pfas tag" is the open range {@code [* TO *]} - a bare {@code *} is rejected by the
 *     parser (leading wildcards are not allowed),</li>
 *     <li>"no pfas tag" needs the {@code *:*} anchor, since a query of only negations matches nothing.</li>
 * </ul>
 * The corpus deliberately uses all three tag values, so presence must not depend on which one it is.
 */
public class PfasTagQuerySemanticsTest {

    private static final String PFAS = "tags." + TagDefinitions.PFAS_TYPE.getTagName();

    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaggedFeature implements Taggable {
        @IndexField(name = "id", documentId = true)
        public String id;

        @IndexField(name = "name", fullTextSearch = true, defaultSearchField = true)
        public String name;

        @IndexFieldWithMapper(mapper = TagMapper.class)
        public Map<String, Tag> tags;

        @Override
        public Map<String, Tag> getTags() {
            return tags;
        }

        @Override
        public void setTags(Map<String, Tag> tags) {
            this.tags = tags;
        }
    }

    private SearchService searchService;
    private Project<?> mockProject;
    private final String projectId = "pfas-test-project";

    @BeforeEach
    public void setup() throws IOException {
        mockProject = Mockito.mock(Project.class);
        Mockito.when(mockProject.getProjectId()).thenReturn(projectId);
        Mockito.when(mockProject.getSystemUID()).thenReturn("test-system-uid");

        searchService = new SearchServiceImpl(project -> {
            Map<String, ValueType> tagDefinitions = new HashMap<>();
            tagDefinitions.put(TagDefinitions.PFAS_TYPE.getTagName(), ValueType.TEXT);
            return new PerPojoSearchContext(null, tagDefinitions);
        });
        searchService.openOrCreateProjectIndex(mockProject);

        // 2 untagged features, 3 potential PFAS, 2 with a PFAS formula, 1 with a PFAS structure
        add("untagged-1", null);
        add("untagged-2", null);
        add("potential-1", TagDefinitions.PFAS_TYPE_0);
        add("potential-2", TagDefinitions.PFAS_TYPE_0);
        add("potential-3", TagDefinitions.PFAS_TYPE_0);
        add("formula-1", TagDefinitions.PFAS_TYPE_1);
        add("formula-2", TagDefinitions.PFAS_TYPE_1);
        add("structure-1", TagDefinitions.PFAS_TYPE_2);
    }

    @AfterEach
    public void cleanup() throws IOException {
        searchService.closeProjectIndex(mockProject, true);
    }

    private void add(String id, String pfasValue) {
        Map<String, Tag> tags = pfasValue == null ? null : Map.of(TagDefinitions.PFAS_TYPE.getTagName(),
                Tag.builder().tagName(TagDefinitions.PFAS_TYPE.getTagName()).value(pfasValue).build());
        searchService.addDocument(projectId, new TaggedFeature(id, "caffeine", tags));
    }

    private long count(String query) {
        return searchService.search(projectId, query, PageRequest.of(0, 20), TaggedFeature.class).getTotalElements();
    }


    @Test
    public void testTagPresenceIsAnOpenRange() {
        assertEquals(6, count(PFAS + ":[* TO *]"), "every feature that carries a pfas tag");
    }

    @Test
    public void testTagAbsenceNeedsTheMatchAllAnchor() {
        assertEquals(2, count("*:* AND NOT " + PFAS + ":[* TO *]"), "the two untagged features");
        assertEquals(0, count("NOT " + PFAS + ":[* TO *]"),
                "a query of only negations matches nothing - this is why the executed query is anchored");
    }

    @Test
    public void testAnchoredAbsenceStillNarrowsWithAFreeTextSegment() {
        assertEquals(2, count("(*:* AND NOT " + PFAS + ":[* TO *]) AND (name:caffeine)"));
        assertEquals(0, count("(*:* AND NOT " + PFAS + ":[* TO *]) AND (name:aspirin)"));
    }

    @Test
    public void testInvertingTheAnchoredAbsenceIsThePresence() {
        String absent = "*:* AND NOT " + PFAS + ":[* TO *]";
        assertEquals(2, count(absent));
        assertEquals(6, count("*:* AND NOT (" + absent + ")"), "the features the filter hides");
    }
}
