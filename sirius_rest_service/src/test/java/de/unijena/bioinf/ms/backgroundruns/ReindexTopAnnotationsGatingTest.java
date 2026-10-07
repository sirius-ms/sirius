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

package de.unijena.bioinf.ms.backgroundruns;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.middleware.model.annotations.FeatureAnnotations;
import de.unijena.bioinf.ms.middleware.model.features.AlignedFeature;
import de.unijena.bioinf.ms.middleware.service.projects.NoSQLProjectImpl;
import de.unijena.bioinf.ms.middleware.service.search.SearchService;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.PerPojoSearchContext;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.SearchServiceImpl;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.sirius.ComputedSubtools;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase;
import de.unijena.bioinf.ms.persistence.storage.nitrite.NitriteSirirusProject;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.projectspace.NoSQLProjectSpaceManager;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code BackgroundRuns} asks {@code NoSQLProjectImpl.updateSearchIndex(instances, reindexTopAnnotations)} to
 * skip refreshing {@code topAnnotationsSummary} when none of the subtools that ran could have changed it -
 * Fingerprint, MsNovelist, Passatutto and SpectraSearch only ever change {@code computedTools}. This pins the
 * merge's own side of that contract: with the flag off, a stale {@code topAnnotations} entry must survive
 * untouched while {@code computedTools} still refreshes; with it on, the stale entry must be gone.
 */
public class ReindexTopAnnotationsGatingTest {

    @AutoClose
    private NitriteSirirusProject ps;
    private NoSQLProjectImpl project;
    private long featureId;

    @BeforeAll
    public static void initApplication() {
        // parsing a command needs the application's properties, which only load if nothing read properties before
        ApplicationCore.VERSION();
    }

    @BeforeEach
    public void createIndexedFeature() throws IOException {
        Path location = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        ps = new NitriteSirirusProject(location);
        SearchService searchService = new SearchServiceImpl(p -> new PerPojoSearchContext(null, Map.of()));
        project = new NoSQLProjectImpl("test", new NoSQLProjectSpaceManager(ps), searchService, (a, b) -> false);

        AlignedFeatures feature = AlignedFeatures.builder().charge((byte) 1).name("af1").build();
        ps.getStorage().insert(feature);
        featureId = feature.getAlignedFeatureId();

        // index the feature once so there is a document to merge onto, not to add
        project.updateSearchIndex(List.of(String.valueOf(featureId)));
    }

    @Test
    public void reindexTopAnnotationsGatesWhetherTheStaleEntrySurvives() throws Exception {
        // stamp a sentinel that only a real refresh could remove, and reset computedTools to a value the
        // next merge has to overwrite regardless of the flag
        FeatureAnnotations sentinel = FeatureAnnotations.builder().confidenceExactMatch(12345.0).build();
        project.getSearchService().updateDocumentsFields(project.getProjectId(), List.of(String.valueOf(featureId)), (AlignedFeature alf) -> {
            alf.setTopAnnotations(sentinel);
            alf.setComputedTools(ComputedSubtools.builder().build());
        }, AlignedFeature.class);

        // the computation that just ran wrote a fresh ComputedSubtools to the database
        ps.getStorage().insert(ComputedSubtools.builder().alignedFeatureId(featureId).formulaSearch(true).build());

        Instance instance = project.getProjectSpaceManager().findInstance(featureId).orElseThrow();

        // gated off (e.g. only Fingerprint ran): computedTools still refreshes, topAnnotations is left alone
        project.updateSearchIndex(List.of(instance), false);
        AlignedFeature gatedOff = indexedFeature();
        assertTrue(gatedOff.getComputedTools().isFormulaSearch(), "computedTools always refreshes");
        assertEquals(12345.0, gatedOff.getTopAnnotations().getConfidenceExactMatch(), 0,
                "gated off: the sentinel must survive untouched");

        // gated on (e.g. Sirius ran): topAnnotations is rebuilt from what the database holds now - nothing,
        // since no FormulaCandidate/CsiStructureSearchResult exists - so the stale sentinel must be gone
        project.updateSearchIndex(List.of(instance), true);
        AlignedFeature gatedOn = indexedFeature();
        assertNotEquals(12345.0, gatedOn.getTopAnnotations() == null ? null : gatedOn.getTopAnnotations().getConfidenceExactMatch(),
                "gated on: the stale sentinel must be gone");
    }

    /** Reads the feature straight from the index - not the database - for exactly the fields the merge touches. */
    private AlignedFeature indexedFeature() {
        return project.findAlignedFeatures("alignedFeatureId:" + featureId, Pageable.ofSize(1), false,
                EnumSet.of(AlignedFeature.OptField.computedTools, AlignedFeature.OptField.topAnnotationsSummary)).getContent().getFirst();
    }
}
