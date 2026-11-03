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
import de.unijena.bioinf.ms.middleware.model.features.AlignedFeature;
import de.unijena.bioinf.ms.middleware.service.projects.NoSQLProjectImpl;
import de.unijena.bioinf.ms.middleware.service.search.SearchService;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.PerPojoSearchContext;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.SearchServiceImpl;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase;
import de.unijena.bioinf.ms.persistence.storage.nitrite.NitriteSirirusProject;
import de.unijena.bioinf.projectspace.NoSQLProjectSpaceManager;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A command that names no features is submitted with the whole project as its instances. Its run locks them while
 * it runs, and a tool chain re-indexes them when it finishes, since it computed new results for them. A standalone
 * tool changes no feature: it reads them (writing summaries, which is why they stay locked) or does not touch them
 * at all (a custom database import) - so re-indexing after it only costs time, on large projects a lot of it.
 */
public class BackgroundRunsCommandTest {

    @AutoClose
    private NitriteSirirusProject ps;
    private NoSQLProjectImpl project;

    @BeforeAll
    public static void initApplication() {
        // parsing a command needs the application's properties, which only load if nothing read properties before
        ApplicationCore.VERSION();
    }

    @BeforeEach
    public void createTestProjectWithFeatures() throws IOException {
        Path location = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        ps = new NitriteSirirusProject(location);
        SearchService searchService = new SearchServiceImpl(p -> new PerPojoSearchContext(null, Map.of()));
        project = new NoSQLProjectImpl("test", new NoSQLProjectSpaceManager(ps), searchService, (a, b) -> false);
        ps.getStorage().insertAll(List.of(
                AlignedFeatures.builder().charge((byte) 1).name("af1").build(),
                AlignedFeatures.builder().charge((byte) 1).name("af2").build()));
    }

    @Test
    public void standaloneToolCommandLocksButDoesNotReindexTheProjectsFeatures() throws Exception {
        // the features were stored after the project opened, so its index does not hold them yet
        assertEquals(2, project.getProjectSpaceManager().countAllFeatures());
        assertEquals(0, numberOfIndexedFeatures());

        // submitted as ComputeServiceImpl submits a command without feature ids: with the whole project
        BackgroundRuns.BackgroundRunJob run = new BackgroundRuns(project, null)
                .runCommand(List.of("decomp", "--mass", "180.0634"), project.getProjectSpaceManager(), null);
        run.awaitResult();

        // still claimed, so they are locked while the tool runs ...
        assertEquals(2, run.getAffectedFeatureIds().size());
        // ... but not re-indexed: a re-index would have written them to the index
        assertEquals(0, numberOfIndexedFeatures());

        // the index does count what a re-index writes, so the zero above is not an index that counts nothing
        project.updateSearchIndex(run.getAffectedFeatureIds());
        assertEquals(2, numberOfIndexedFeatures());
    }

    private int numberOfIndexedFeatures() {
        return project.getSearchService().getNumberOfDocuments(project.getProjectId(), AlignedFeature.class);
    }
}
