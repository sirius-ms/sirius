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

package de.unijena.bioinf.ms.middleware.service.projects;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ms.middleware.model.features.AlignedFeature;
import de.unijena.bioinf.ms.middleware.model.features.FeatureImport;
import de.unijena.bioinf.ms.middleware.model.statistics.Statistics;
import de.unijena.bioinf.ms.middleware.model.tags.TagDefinition;
import de.unijena.bioinf.ms.middleware.service.search.SearchService;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.PerPojoSearchContext;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.SearchServiceImpl;
import de.unijena.bioinf.ms.persistence.model.core.statistics.AggregationType;
import de.unijena.bioinf.ms.persistence.model.core.statistics.FoldChange;
import de.unijena.bioinf.ms.persistence.model.core.statistics.QuantMeasure;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueType;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase;
import de.unijena.bioinf.ms.persistence.storage.nitrite.NitriteSirirusProject;
import de.unijena.bioinf.projectspace.NoSQLProjectSpaceManager;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The fold changes of a feature are only reported when they are asked for.
 * <p>
 * They are hidden from the api and absent from the generated clients, so nothing can read them - but they are
 * restored on every read of a feature (the index mapper rebuilds them because the fold-change update path needs
 * something to merge into) and are by far the largest part of a feature: one entry per group pair, quantification
 * and aggregation. Sending them to a client that discards them is the cost this pins away.
 * <p>
 * The other half of what is pinned here is that nothing was broken to achieve it: the fold changes must survive in
 * the INDEX, because the update path reads them back to merge into. A read that hides them must not be a read that
 * drops them.
 */
public class OptionalFeatureStatisticsTest {

    private static final EnumSet<AlignedFeature.OptField> NO_OPT = EnumSet.noneOf(AlignedFeature.OptField.class);
    private static final EnumSet<AlignedFeature.OptField> WITH_STATS = EnumSet.of(AlignedFeature.OptField.statistics);

    @AutoClose
    private NitriteSirirusProject ps;
    private NoSQLProjectImpl project;

    @SneakyThrows
    private static SearchService makeSearchService() {
        return new SearchServiceImpl(project -> {
            Map<String, ValueType> tagDefinitions = new HashMap<>();
            for (Object item : project.findTags()) {
                TagDefinition td = (TagDefinition) item;
                tagDefinitions.put(td.getTagName(), td.getValueType());
            }
            return new PerPojoSearchContext(null, tagDefinitions);
        });
    }

    @BeforeEach
    public void createTestProject() throws IOException {
        Path location = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        ps = new NitriteSirirusProject(location);
        project = new NoSQLProjectImpl("test", new NoSQLProjectSpaceManager(ps), makeSearchService(), (a, b) -> false);
    }

    private String importFeature() {
        return project.addAlignedFeatures(List.of(FeatureImport.builder()
                .name("caffeine")
                .externalFeatureId("ext-1")
                .ionMass(195.0876d)
                .charge((byte) 1)
                .rtStartSeconds(10d)
                .rtApexSeconds(15d)
                .rtEndSeconds(20d)
                .build()), null, NO_OPT).getFirst().getAlignedFeatureId();
    }

    /** Writes one fold change into the index the way the fold-change workflow does. */
    private void indexFoldChange(String featureId) {
        project.getSearchService().updateDocumentsFields(project.getProjectId(), List.of(featureId),
                (AlignedFeature indexed) -> indexed.setStats(List.of(
                        de.unijena.bioinf.ms.middleware.model.statistics.FoldChange.builder()
                                .objectId(featureId)
                                .quantType(de.unijena.bioinf.ms.middleware.model.features.QuantRowType.FEATURES)
                                .leftGroup("samples")
                                .rightGroup("blanks")
                                .quantification(QuantMeasure.APEX_INTENSITY)
                                .aggregation(AggregationType.AVG)
                                .leftAbundance(100d)
                                .rightAbundance(10d)
                                .foldChange(10d)
                                .build())),
                AlignedFeature.class);
    }

    /** ... and into the document store, which is what a feature read by id is built from. */
    @SneakyThrows
    private void storeFoldChange(String featureId) {
        ps.getStorage().insert(FoldChange.AlignedFeaturesFoldChange.builder()
                .alignedFeatureId(Long.parseLong(featureId))
                .leftGroup("samples")
                .rightGroup("blanks")
                .quantification(QuantMeasure.APEX_INTENSITY)
                .aggregation(AggregationType.AVG)
                .leftAbundance(100d)
                .rightAbundance(10d)
                .build());
    }

    @Test
    @DisplayName("a page does not carry the fold changes unless they are asked for")
    void pageOmitsStatisticsByDefault() {
        String id = importFeature();
        indexFoldChange(id);

        AlignedFeature quiet = project.findAlignedFeatures(null, Pageable.unpaged(), false, NO_OPT)
                .getContent().getFirst();
        assertNull(quiet.getStats(), "nothing can read them, so nothing should pay for them");

        AlignedFeature asked = project.findAlignedFeatures(null, Pageable.unpaged(), false, WITH_STATS)
                .getContent().getFirst();
        assertNotNull(asked.getStats());
        assertEquals(1, asked.getStats().size());
        Statistics stat = asked.getStats().getFirst();
        assertEquals("samples", stat.getLeftGroup());
        assertEquals("blanks", stat.getRightGroup());
    }

    /**
     * Asking for everything else the index holds is not asking for these.
     * <p>
     * Spelled out rather than taken from {@code INDEXED_OPT_FIELDS}, which contains the fold changes: a document
     * carries them, so every read hands them back and something has to drop them again. This is the request
     * where that is easiest to get wrong, because every field in it comes off the document and nothing has to be
     * read for it.
     */
    @Test
    @DisplayName("asking for the rest of the indexed fields does not ask for these")
    void everyOtherIndexedFieldStillOmitsStatistics() {
        String id = importFeature();
        indexFoldChange(id);

        EnumSet<AlignedFeature.OptField> everythingElseInTheIndex = EnumSet.of(
                AlignedFeature.OptField.tags, AlignedFeature.OptField.computedTools,
                AlignedFeature.OptField.topAnnotationsSummary, AlignedFeature.OptField.qualities);

        AlignedFeature feature = project
                .findAlignedFeatures(null, Pageable.unpaged(), false, everythingElseInTheIndex)
                .getContent().getFirst();
        assertNull(feature.getStats());
    }

    @Test
    @DisplayName("a single feature never carries them - it is read from its document, not the index")
    void singleFeatureCarriesNoStatistics() {
        String id = importFeature();
        storeFoldChange(id);
        indexFoldChange(id);

        // findAlignedFeaturesById builds the feature from its document, and fold changes live in the index. They
        // are therefore absent whether or not they are asked for - as they were before the opt field existed.
        // Serving them here would mean a query per feature, which is what the page path avoids by reading the
        // index; if this endpoint ever needs them it should read the index too.
        assertNull(project.findAlignedFeaturesById(id, false, NO_OPT).getStats());
        assertNull(project.findAlignedFeaturesById(id, false, WITH_STATS).getStats());

        // ... while the page, which is served from the index, has them
        assertNotNull(project.findAlignedFeatures(null, Pageable.unpaged(), false, WITH_STATS)
                .getContent().getFirst().getStats());
    }

    @Test
    @DisplayName("hiding them from a read does not drop them from the index")
    void readingWithoutStatisticsKeepsThemInTheIndex() {
        String id = importFeature();
        indexFoldChange(id);

        // The update path reads a feature back, merges into its fold changes and re-indexes the whole bean. If a
        // read that hides them ever reached it, the re-index would erase them - this is the guard for that.
        assertNull(project.findAlignedFeatures(null, Pageable.unpaged(), false, NO_OPT).getContent().getFirst().getStats());

        project.getSearchService().updateDocumentsFields(project.getProjectId(), List.of(id),
                (AlignedFeature indexed) -> assertNotNull(indexed.getStats(),
                        "the update path must still see the fold changes it merges into"),
                AlignedFeature.class);

        assertEquals(1, project.findAlignedFeatures(null, Pageable.unpaged(), false, WITH_STATS)
                .getContent().getFirst().getStats().size(), "the fold change must have survived both reads");
    }
}
