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
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.middleware.service.search;

import de.unijena.bioinf.ms.middleware.model.features.AlignedFeature;
import de.unijena.bioinf.ms.middleware.model.features.QuantRowType;
import de.unijena.bioinf.ms.middleware.model.statistics.FoldChange;
import de.unijena.bioinf.ms.middleware.model.statistics.Statistics;
import de.unijena.bioinf.ms.middleware.service.projects.Project;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.PerPojoSearchContext;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.SearchServiceImpl;
import de.unijena.bioinf.ms.middleware.service.search.mappers.FoldChangeMapper;
import de.unijena.bioinf.ms.persistence.model.core.statistics.AggregationType;
import de.unijena.bioinf.ms.persistence.model.core.statistics.QuantMeasure;
import de.unijena.bioinf.ms.persistence.model.core.tags.Groups;
import org.apache.lucene.index.IndexableField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageRequest;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The other half of the blank filter contract. The gui side is pinned in
 * {@code BlankRemovalFoldChangeOrientationTest}, which has to spell the mapper's naming rule as a literal
 * because the mapper lives here and is not on its classpath. This test holds that literal against the real
 * mapper, so the two cannot drift apart.
 */
public class BlankRemovalIndexFieldNameTest {

    /**
     * Copied from {@code FeatureFilterModel.BLANK_REMOVAL_SEARCH_FIELD_NAME} in sirius_gui, which is not a
     * dependency of this module. This literal is the contract between them.
     */
    private static final String BLANK_REMOVAL_FIELD = "stats.foldChange.Sample.Blank.APEX_INTENSITY.AVG";

    private SearchService searchService;
    private Project<?> mockProject;
    private final String projectId = "blank-removal-test";

    @BeforeEach
    public void setup() throws IOException {
        mockProject = Mockito.mock(Project.class);
        Mockito.when(mockProject.getProjectId()).thenReturn(projectId);
        Mockito.when(mockProject.getSystemUID()).thenReturn("blank-removal-uid");
        searchService = new SearchServiceImpl(project -> new PerPojoSearchContext(null, new HashMap<>()));
        searchService.openOrCreateProjectIndex(mockProject);
    }

    @AfterEach
    public void cleanup() throws IOException {
        searchService.closeProjectIndex(mockProject, true);
    }

    /** A blank-removal fold change as the job submitted by the run type dialog produces it. */
    private static Statistics sampleOverBlank(double sampleAbundance, double blankAbundance) {
        return FoldChange.builder()
                .quantType(QuantRowType.FEATURES)
                .objectId("1")
                .leftGroup(Groups.SAMPLE_RUNS.getGroupName())
                .rightGroup(Groups.BLANK_RUNS.getGroupName())
                .quantification(QuantMeasure.APEX_INTENSITY)
                .aggregation(AggregationType.AVG)
                .leftAbundance(sampleAbundance)
                .rightAbundance(blankAbundance)
                .foldChange(sampleAbundance / blankAbundance)
                .build();
    }

    private static AlignedFeature featureWith(Statistics... stats) {
        AlignedFeature feature = new AlignedFeature();
        feature.setAlignedFeatureId("1");
        feature.setStats(List.of(stats));
        return feature;
    }

    @Test
    public void testTheMapperNamesTheFieldTheBlankFilterQueries() {
        Set<String> names = new HashSet<>();
        for (IndexableField field : new FoldChangeMapper.AlignedFeatureFoldChange()
                .toIndexableFields("stats", List.of(sampleOverBlank(100_000d, 1_000d))))
            names.add(field.name());

        assertEquals(Set.of(BLANK_REMOVAL_FIELD), names,
                "the gui queries " + BLANK_REMOVAL_FIELD + " and nothing else is written for this job");
    }

    /**
     * End to end over the real index: a feature a hundred times more abundant in the sample than in the blank
     * passes a minimum of two, and the same query still discriminates - the threshold is read, not ignored.
     */
    @Test
    public void testAFeatureEnrichedInTheSampleIsFoundByTheBlankFilter() {
        searchService.addDocument(projectId, featureWith(sampleOverBlank(100_000d, 1_000d)));

        assertEquals(1, search(BLANK_REMOVAL_FIELD + ":[2.0 TO *]"), "fold change is 100, the minimum is 2");
        assertEquals(0, search(BLANK_REMOVAL_FIELD + ":[1000.0 TO *]"), "fold change is 100, the minimum is 1000");
    }

    /**
     * The failure this guards against is silent: a feature that is only in the blank is not excluded by an
     * error but by matching nothing, which is exactly what a wrong field name looks like. Pinning a feature
     * that must be found next to one that must not keeps the two apart.
     */
    @Test
    public void testAFeatureNoHigherInTheSampleThanInTheBlankIsFilteredOut() {
        searchService.addDocument(projectId, featureWith(sampleOverBlank(1_000d, 1_000d)));

        assertEquals(0, search(BLANK_REMOVAL_FIELD + ":[2.0 TO *]"), "fold change is 1");
        assertTrue(search(BLANK_REMOVAL_FIELD + ":[* TO *]") > 0,
                "and it is absent because of its value, not because the field is missing");
    }

    private long search(String query) {
        return searchService.search(projectId, query, PageRequest.of(0, 10), AlignedFeature.class)
                .getTotalElements();
    }
}
