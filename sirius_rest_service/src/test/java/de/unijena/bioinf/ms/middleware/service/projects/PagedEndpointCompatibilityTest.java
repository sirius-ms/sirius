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
import de.unijena.bioinf.ms.middleware.model.compounds.Compound;
import de.unijena.bioinf.ms.middleware.model.features.AlignedFeature;
import de.unijena.bioinf.ms.middleware.model.features.FeatureImport;
import de.unijena.bioinf.ms.middleware.model.spectra.BasicSpectrum;
import de.unijena.bioinf.ms.middleware.model.tags.TagDefinition;
import de.unijena.bioinf.ms.middleware.service.search.SearchService;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.PerPojoSearchContext;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.SearchServiceImpl;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueType;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase;
import de.unijena.bioinf.ms.persistence.storage.nitrite.NitriteSirirusProject;
import de.unijena.bioinf.projectspace.NoSQLProjectSpaceManager;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pins the behaviour of the paged read endpoints as it must stay for the intermediate, backward-compatible release.
 * <p>
 * {@code /aligned-features/page} deliberately serves from the Lucene index even when no search query is given — the
 * index is the single source of truth. That is only safe as long as an empty query yields exactly what the document
 * store would have returned, which is what these tests assert. {@code /compounds/page} must keep reading the database
 * directly because compound-level indexing is not implemented yet.
 */
public class PagedEndpointCompatibilityTest {

    private static final EnumSet<AlignedFeature.OptField> NO_OPT = EnumSet.noneOf(AlignedFeature.OptField.class);

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

    /**
     * Realistic-ish features with distinct retention times and masses so ordering is unambiguous.
     * A monoisotopic peak plus a ~1.1% 13C isotope peak keeps the mock MS1 physically plausible.
     */
    private List<AlignedFeature> importFeatures(int count) {
        List<FeatureImport> imports = IntStream.range(0, count).mapToObj(i -> {
            double precursorMz = 200d + i;
            BasicSpectrum ms1 = new BasicSpectrum(
                    new double[]{precursorMz, precursorMz + 1.00336},
                    new double[]{1.0, 0.011});
            return FeatureImport.builder()
                    .name("feature-" + i)
                    .externalFeatureId("ext-" + i)
                    .ionMass(precursorMz)
                    .charge((byte) 1)
                    .rtStartSeconds(10d * i)
                    .rtApexSeconds(10d * i + 5d)
                    .rtEndSeconds(10d * i + 10d)
                    .mergedMs1(ms1)
                    .ms1Spectra(List.of(ms1))
                    .build();
        }).toList();
        return project.addAlignedFeatures(imports, null, NO_OPT);
    }

    private static List<String> idsOf(Page<AlignedFeature> page) {
        return page.getContent().stream().map(AlignedFeature::getAlignedFeatureId).toList();
    }

    @Test
    @DisplayName("features: blank query serves the same set from the index as from the document store")
    void blankQueryMatchesDocumentStore() {
        importFeatures(12);

        List<String> fromIndex = idsOf(project.findAlignedFeatures(null, Pageable.unpaged(), false, NO_OPT));
        List<String> fromDb = idsOf(project.findAlignedFeatures(Pageable.unpaged(), false, NO_OPT));

        assertEquals(12, fromDb.size(), "sanity: all imported features must be in the document store");
        assertEquals(fromDb.size(), fromIndex.size(),
                "index-backed paging must not lose or duplicate features when no query is given");
        assertEquals(Set.copyOf(fromDb), Set.copyOf(fromIndex),
                "index and document store must agree on the feature set for an empty query");
    }

    @Test
    @DisplayName("features: an empty-string query behaves like no query at all")
    void emptyStringQueryIsTreatedAsMatchAll() {
        importFeatures(5);

        assertEquals(5, project.findAlignedFeatures("", Pageable.unpaged(), false, NO_OPT).getTotalElements());
        assertEquals(5, project.findAlignedFeatures("   ", Pageable.unpaged(), false, NO_OPT).getTotalElements());
    }

    @Test
    @DisplayName("features: paging over the index covers every element exactly once")
    void pagingCoversAllElementsWithoutOverlap() {
        importFeatures(10);

        List<String> paged = IntStream.range(0, 4)
                .mapToObj(p -> project.findAlignedFeatures(null, PageRequest.of(p, 3), false, NO_OPT))
                .flatMap(page -> idsOf(page).stream())
                .toList();

        assertEquals(10, paged.size(), "paging must yield each feature exactly once");
        assertEquals(10, Set.copyOf(paged).size(), "pages must not overlap");
    }

    @Test
    @DisplayName("features: sorting by retention time is honoured and reversible")
    void sortOrderIsHonoured() {
        importFeatures(6);

        Page<AlignedFeature> ascPage = project.findAlignedFeatures(
                null, PageRequest.of(0, 6, Sort.by(Sort.Direction.ASC, "rtApexSeconds")), false, NO_OPT);
        Page<AlignedFeature> descPage = project.findAlignedFeatures(
                null, PageRequest.of(0, 6, Sort.by(Sort.Direction.DESC, "rtApexSeconds")), false, NO_OPT);

        List<Double> ascRt = ascPage.getContent().stream().map(AlignedFeature::getRtApexSeconds).toList();
        assertEquals(6, ascRt.size());
        assertFalse(ascRt.contains(null), "rtApexSeconds must be populated for the sort to be meaningful");
        for (int i = 1; i < ascRt.size(); i++)
            assertTrue(ascRt.get(i - 1) <= ascRt.get(i),
                    () -> "ascending sort must yield non-decreasing retention times, got " + ascRt);

        assertEquals(idsOf(ascPage), idsOf(descPage).reversed(),
                "descending order must be the exact reverse of ascending order");
    }

    @Test
    @DisplayName("features: an unavailable search service is reported as 503, not as an empty result")
    void missingSearchServiceFailsLoudly() throws IOException {
        Path location = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        try (NitriteSirirusProject bare = new NitriteSirirusProject(location)) {
            NoSQLProjectImpl unindexed =
                    new NoSQLProjectImpl("no-index", new NoSQLProjectSpaceManager(bare), null, (a, b) -> false);

            ResponseStatusException e = assertThrows(ResponseStatusException.class,
                    () -> unindexed.findAlignedFeatures(null, Pageable.unpaged(), false, NO_OPT));
            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, e.getStatusCode(),
                    "silently returning an empty page would look like an empty project");
        }
    }

    @Test
    @DisplayName("compounds: blank query reads the database directly")
    void compoundsBlankQueryUsesDatabase() {
        importFeatures(4);

        Page<Compound> compounds = project.findCompounds(
                null, Pageable.unpaged(), false,
                EnumSet.noneOf(Compound.OptField.class), NO_OPT);

        assertEquals(4, compounds.getTotalElements(),
                "each imported feature becomes its own compound when no grouping is given");
    }

    @Test
    @DisplayName("compounds: a real search query is rejected because compound indexing is not implemented")
    void compoundsSearchQueryIsRejected() {
        importFeatures(2);

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> project.findCompounds("tags.anything:1", Pageable.unpaged(), false,
                        EnumSet.noneOf(Compound.OptField.class), NO_OPT));
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, e.getStatusCode(),
                "documented contract: searchQuery on /compounds/page responds 405 until compounds are indexed");
    }

    @Test
    @DisplayName("features: empty project pages cleanly instead of failing")
    void emptyProjectYieldsEmptyPage() {
        Page<AlignedFeature> page = project.findAlignedFeatures(null, Pageable.unpaged(), false, NO_OPT);
        assertTrue(page.isEmpty());
        assertEquals(0, page.getTotalElements());
    }
}
