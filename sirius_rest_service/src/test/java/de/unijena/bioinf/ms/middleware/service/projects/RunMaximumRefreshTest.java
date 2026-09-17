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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.middleware.service.projects;

import de.unijena.bioinf.ms.persistence.model.core.run.Chromatography;
import de.unijena.bioinf.ms.middleware.model.features.AlignedFeature;
import de.unijena.bioinf.ms.middleware.service.search.SearchService;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.NoSqlProjectSearchContextProvider;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.SearchServiceImpl;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.Feature;
import de.unijena.bioinf.ms.persistence.model.core.run.LCMSRun;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase;
import de.unijena.bioinf.ms.persistence.storage.nitrite.NitriteSirirusProject;
import de.unijena.bioinf.projectspace.NoSQLProjectSpaceManager;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A feature's relative intensity is a percentage of the most intense feature of its run, so writing a stronger
 * feature invalidates what the index holds for every other feature of that run. The run maxima that decide this
 * are derived from the run features when they are first needed - a session that only reads never derives them -
 * so a write may find none cached and still has to know which maxima it moved.
 */
public class RunMaximumRefreshTest {

    private Path location;
    private NitriteSirirusProject ps;
    private SearchService searchService;
    private long runId;

    @BeforeEach
    @SneakyThrows
    public void createProject() {
        location = Files.createTempDirectory("run-maximum-refresh-")
                .resolve("test" + SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        ps = new NitriteSirirusProject(location);
        searchService = new SearchServiceImpl(new NoSqlProjectSearchContextProvider(true, null, true));

        LCMSRun run = LCMSRun.builder().name("run1").chromatography(Chromatography.LC).build();
        ps.getStorage().insert(run);
        runId = run.getRunId();
    }

    @AfterEach
    @SneakyThrows
    public void closeProject() {
        searchService.close();
        ps.close();
    }

    /** A fresh session on the same project: its run maxima are not derived yet. */
    private NoSQLProjectImpl session() {
        return new NoSQLProjectImpl("test", new NoSQLProjectSpaceManager(ps), searchService, (a, b) -> false);
    }

    @SneakyThrows
    private long addFeature(String name, double apexIntensity) {
        AlignedFeatures af = AlignedFeatures.builder().charge((byte) 1).name(name).build();
        ps.getStorage().insert(af);
        ps.getStorage().insert(Feature.builder().alignedFeatureId(af.getAlignedFeatureId())
                .apexIntensity(apexIntensity).areaUnderCurve(apexIntensity).runId(runId).build());
        return af.getAlignedFeatureId();
    }

    private List<String> namesMatching(NoSQLProjectImpl project, String query) {
        return project.findAlignedFeatures(query, Pageable.unpaged(), false,
                        EnumSet.noneOf(AlignedFeature.OptField.class))
                .getContent().stream().map(AlignedFeature::getName).sorted().toList();
    }

    /**
     * The write that raises a run's maximum happens in a session that has not derived the maxima yet - the
     * state the index was written against is then the one without the features this write added, which is what
     * makes the comparison possible at all.
     */
    @Test
    @SneakyThrows
    public void testAStrongerFeatureRewritesTheRunsOtherFeatures() {
        NoSQLProjectImpl first = session();
        long weak = addFeature("weak", 100d);
        first.addToSearchIndex(List.of(String.valueOf(weak)), null);
        assertEquals(List.of("weak"), namesMatching(first, "intensityApex:[50% TO *]"),
                "the only feature of the run is its maximum");

        // a new session reads nothing, so it derives no maxima before the write
        NoSQLProjectImpl second = session();
        long strong = addFeature("strong", 1000d);
        second.addToSearchIndex(List.of(String.valueOf(strong)), null);

        assertEquals(List.of("strong"), namesMatching(second, "intensityApex:[50% TO *]"),
                "'weak' is now 10 % of the run maximum and must have been rewritten");
        assertEquals(List.of("strong", "weak"), namesMatching(second, "intensityApex:[5% TO *]"));
    }

    /**
     * Deleting the feature that set a run's maximum lowers it, so every other feature of that run is a larger
     * share of it than the index says. Unlike a write, a deletion cannot be compared against a state derived
     * afterwards - what it removed is gone - so the comparison has to be prepared before it happens.
     */
    @Test
    @SneakyThrows
    public void testDeletingTheStrongestFeatureRewritesTheRunsOtherFeatures() {
        NoSQLProjectImpl first = session();
        long strong = addFeature("strong", 1000d);
        long weak = addFeature("weak", 100d);
        first.addToSearchIndex(List.of(String.valueOf(strong), String.valueOf(weak)), null);
        assertEquals(List.of("strong"), namesMatching(first, "intensityApex:[50% TO *]"),
                "'weak' is 10 % of the run maximum while 'strong' is still there");

        NoSQLProjectImpl second = session();
        second.deleteAlignedFeaturesById(String.valueOf(strong));

        assertEquals(List.of("weak"), namesMatching(second, "intensityApex:[50% TO *]"),
                "'weak' is now the run maximum and must have been rewritten");
    }

    /** A deletion that does not touch a maximum leaves every other feature's percentage as it was. */
    @Test
    @SneakyThrows
    public void testDeletingAWeakerFeatureLeavesTheOthersAlone() {
        NoSQLProjectImpl first = session();
        long strong = addFeature("strong", 1000d);
        long middle = addFeature("middle", 500d);
        long weak = addFeature("weak", 100d);
        first.addToSearchIndex(List.of(String.valueOf(strong), String.valueOf(middle), String.valueOf(weak)), null);

        NoSQLProjectImpl second = session();
        second.deleteAlignedFeaturesById(String.valueOf(weak));

        assertEquals(List.of("middle", "strong"), namesMatching(second, "intensityApex:[50% TO *]"),
                "the run maximum is untouched, so the remaining percentages are too");
        assertEquals(List.of("middle"), namesMatching(second, "intensityApex:{20% TO 99%}"));
    }

    /** Nothing moved: a weaker feature leaves the maximum, and the other features' percentages, alone. */
    @Test
    @SneakyThrows
    public void testAWeakerFeatureDoesNotChangeTheOthers() {
        NoSQLProjectImpl first = session();
        long strong = addFeature("strong", 1000d);
        first.addToSearchIndex(List.of(String.valueOf(strong)), null);

        NoSQLProjectImpl second = session();
        long weak = addFeature("weak", 100d);
        second.addToSearchIndex(List.of(String.valueOf(weak)), null);

        assertEquals(List.of("strong"), namesMatching(second, "intensityApex:[50% TO *]"));
        assertEquals(List.of("strong", "weak"), namesMatching(second, "intensityApex:[10% TO *]"));
    }
}
