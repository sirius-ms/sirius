/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2026 Bright Giant GmbH
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.middleware.service.projects;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.ms.middleware.model.features.AlignedFeature;
import de.unijena.bioinf.ms.middleware.service.search.SearchService;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.NoSqlProjectSearchContextProvider;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.SearchServiceImpl;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.Feature;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase;
import de.unijena.bioinf.ms.persistence.storage.nitrite.NitriteSirirusProject;
import de.unijena.bioinf.projectspace.NoSQLProjectSpaceManager;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.data.domain.Pageable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Building the search index and listing features are what opening a project and browsing it wait for, and a
 * computation can have every CPU thread busy meanwhile. Neither may wait for those threads: the calling thread
 * does the work, and CPU threads only help when they are free.
 */
@Timeout(120)
public class SearchIndexUnderLoadTest {

    /** More than one chunk of the index feed, so it hands out work the CPU threads could take. */
    private static final int FEATURES = 600;

    private NitriteSirirusProject ps;
    private SearchService searchService;
    private NoSQLProjectImpl project;
    private final CountDownLatch release = new CountDownLatch(1);

    @BeforeEach
    @SneakyThrows
    public void createProject() {
        Path location = Files.createTempDirectory("index-under-load-")
                .resolve("test" + SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        ps = new NitriteSirirusProject(location);
        List<AlignedFeatures> features = new ArrayList<>();
        for (int i = 0; i < FEATURES; i++)
            features.add(AlignedFeatures.builder().charge((byte) 1).name("feature" + i)
                    .features(List.of(Feature.builder().apexIntensity(1000d + i).areaUnderCurve(1d).build())).build());
        ps.importAlignedFeatures(features);
        searchService = new SearchServiceImpl(new NoSqlProjectSearchContextProvider(true, null, true));
        project = new NoSQLProjectImpl("test", new NoSQLProjectSpaceManager(ps), searchService, (a, b) -> false);
    }

    @AfterEach
    @SneakyThrows
    public void closeProject() {
        release.countDown();
        searchService.close();
        ps.close();
    }

    private void occupyEveryCpuThread() throws InterruptedException {
        int threads = SiriusJobs.getGlobalJobManager().getCPUThreads();
        CountDownLatch busy = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++)
            SiriusJobs.getGlobalJobManager().submitJob(new BasicJJob<Boolean>(JJob.JobType.CPU) {
                @Override
                protected Boolean compute() throws InterruptedException {
                    busy.countDown();
                    release.await();
                    return true;
                }
            });
        assertTrue(busy.await(30, TimeUnit.SECONDS), "every CPU thread is busy");
    }

    @Test
    public void theIndexIsBuiltAndFeaturesAreListedWhileEveryCpuThreadIsBusy() throws Exception {
        occupyEveryCpuThread();

        project.createSearchIndex(true);
        assertEquals(FEATURES, project.findAlignedFeatures("feature*", Pageable.unpaged(), false,
                EnumSet.noneOf(AlignedFeature.OptField.class)).getContent().size(), "every feature was indexed");

        List<AlignedFeature> listed = project.findAlignedFeatures(Pageable.unpaged(), false,
                EnumSet.of(AlignedFeature.OptField.intensities)).getContent();
        assertEquals(FEATURES, listed.size());
        assertTrue(listed.stream().allMatch(f -> f.getIntensity() != null && f.getIntensity().getApex() != null),
                "converted with what was asked for");
    }
}
