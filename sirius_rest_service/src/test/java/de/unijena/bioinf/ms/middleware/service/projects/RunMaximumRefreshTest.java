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

import de.unijena.bioinf.ms.middleware.model.features.AlignedFeature;
import de.unijena.bioinf.ms.middleware.service.search.SearchService;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.NoSqlProjectSearchContextProvider;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.SearchServiceImpl;
import de.unijena.bioinf.ms.persistence.model.core.Compound;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.Feature;
import de.unijena.bioinf.ms.persistence.model.core.run.Chromatography;
import de.unijena.bioinf.ms.persistence.model.core.run.LCMSRun;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase;
import de.unijena.bioinf.ms.persistence.storage.nitrite.NitriteSirirusProject;
import de.unijena.bioinf.projectspace.NoSQLProjectSpaceManager;
import lombok.SneakyThrows;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A feature's relative intensity is a percentage of the most intense feature its run has held, so writing a
 * stronger feature invalidates what the index holds for every other feature of that run. The maxima are stored
 * by the project and rise as features are written; the index rewrites the runs whose maximum rose past what its
 * documents were written against. A deletion lowers no maximum and rewrites nothing.
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

    /** A fresh session on the same project. */
    private NoSQLProjectImpl session() {
        return new NoSQLProjectImpl("test", new NoSQLProjectSpaceManager(ps), searchService, (a, b) -> false);
    }

    /**
     * Imports one aligned feature with one run feature the way every import does, through the project - which is
     * what folds it into the stored maxima. {@code runId} null makes it a peak-list feature.
     */
    @SneakyThrows
    private long importFeature(String name, @Nullable Long runId, double apexIntensity, @Nullable Long compoundId) {
        AlignedFeatures af = AlignedFeatures.builder().charge((byte) 1).name(name)
                .features(List.of(Feature.builder().apexIntensity(apexIntensity).areaUnderCurve(apexIntensity * 20)
                        .runId(runId).build()))
                .build();
        if (compoundId == null)
            ps.importAlignedFeatures(List.of(af));
        else
            ps.importAlignedFeatures(List.of(af), compoundId);
        return af.getAlignedFeatureId();
    }

    private long addFeature(String name, double apexIntensity) {
        return importFeature(name, runId, apexIntensity, null);
    }

    private long addPeakListFeature(String name, double apexIntensity) {
        return importFeature(name, null, apexIntensity, null);
    }

    @SneakyThrows
    private long addCompound(String name) {
        Compound compound = Compound.builder().name(name).build();
        ps.getStorage().insert(compound);
        return compound.getCompoundId();
    }

    private static List<String> ids(long... ids) {
        return java.util.Arrays.stream(ids).mapToObj(String::valueOf).toList();
    }

    private List<String> namesMatching(NoSQLProjectImpl project, String query) {
        return project.findAlignedFeatures(query, Pageable.unpaged(), false,
                        EnumSet.noneOf(AlignedFeature.OptField.class))
                .getContent().stream().map(AlignedFeature::getName).sorted().toList();
    }

    /** The stronger feature arrives in a later session, which knows what the index holds from the project. */
    @Test
    public void testAStrongerFeatureRewritesTheRunsOtherFeatures() {
        NoSQLProjectImpl first = session();
        long weak = addFeature("weak", 1.0e5);
        first.addToSearchIndex(ids(weak), null);
        assertEquals(List.of("weak"), namesMatching(first, "intensityApexRelative:[50 TO *]"),
                "the only feature of the run is its maximum");

        NoSQLProjectImpl second = session();
        long strong = addFeature("strong", 1.0e6);
        second.addToSearchIndex(ids(strong), null);

        assertEquals(List.of("strong"), namesMatching(second, "intensityApexRelative:[50 TO *]"),
                "'weak' is now 10 % of the run maximum and must have been rewritten");
        assertEquals(List.of("strong", "weak"), namesMatching(second, "intensityApexRelative:[5 TO *]"));
    }

    @Test
    public void testASecondWriteInTheSameSessionStillRewritesTheRun() {
        NoSQLProjectImpl session = session();
        long weak = addFeature("weak", 1.0e5);
        session.addToSearchIndex(ids(weak), null);
        assertEquals(List.of("weak"), namesMatching(session, "intensityApexRelative:[50 TO *]"));

        long strong = addFeature("strong", 1.0e6);
        session.addToSearchIndex(ids(strong), null);

        assertEquals(List.of("strong"), namesMatching(session, "intensityApexRelative:[50 TO *]"),
                "'weak' is 10 % of the new maximum and must have been rewritten");
        assertEquals(List.of("strong", "weak"), namesMatching(session, "intensityApexRelative:[5 TO *]"));
    }

    @Test
    public void testAWeakerFeatureDoesNotChangeTheOthers() {
        NoSQLProjectImpl first = session();
        long strong = addFeature("strong", 1.0e6);
        first.addToSearchIndex(ids(strong), null);

        NoSQLProjectImpl second = session();
        long weak = addFeature("weak", 1.0e5);
        second.addToSearchIndex(ids(weak), null);

        assertEquals(List.of("strong"), namesMatching(second, "intensityApexRelative:[50 TO *]"));
        assertEquals(List.of("strong", "weak"), namesMatching(second, "intensityApexRelative:[10 TO *]"));
    }

    /** Peak-list features belong to no run; they are held against the strongest feature of the project. */
    @Test
    public void testFeaturesWithoutARunAreHeldAgainstTheGlobalMaximum() {
        NoSQLProjectImpl session = session();
        long weak = addPeakListFeature("weak", 1.0e5);
        long other = addPeakListFeature("other", 5.0e4);
        session.addToSearchIndex(ids(weak, other), null);
        assertEquals(List.of("weak"), namesMatching(session, "intensityApexRelative:[99 TO *]"));
        assertEquals(List.of("other"), namesMatching(session, "intensityApexRelative:[49 TO 51]"));

        long strong = addPeakListFeature("strong", 1.0e6);
        session.addToSearchIndex(ids(strong), null);

        assertEquals(List.of("strong"), namesMatching(session, "intensityApexRelative:[99 TO *]"));
        assertEquals(List.of("weak"), namesMatching(session, "intensityApexRelative:[9 TO 11]"),
                "'weak' is now 10 % of the global maximum and must have been rewritten");
        assertEquals(List.of("other"), namesMatching(session, "intensityApexRelative:[4 TO 6]"));
    }

    /**
     * A maximum only ever rises: after the strongest feature is deleted the others keep the share of it they
     * had, and nothing is rewritten.
     */
    @Test
    public void testDeletingTheStrongestFeatureLeavesTheOthersAsTheyWere() {
        NoSQLProjectImpl first = session();
        long strong = addFeature("strong", 1.0e6);
        long weak = addFeature("weak", 1.0e5);
        first.addToSearchIndex(ids(strong, weak), null);

        NoSQLProjectImpl second = session();
        second.deleteAlignedFeaturesById(String.valueOf(strong));

        assertEquals(List.of(), namesMatching(second, "intensityApexRelative:[50 TO *]"),
                "the reference is the maximum the run has held, not the one it holds");
        assertEquals(List.of("weak"), namesMatching(second, "intensityApexRelative:[9 TO 11]"));
    }

    /** The other two ways features are deleted reach the same place. */
    @Test
    public void testDeletingSeveralFeaturesOrTheirCompoundLeavesTheOthersAsTheyWere() {
        NoSQLProjectImpl session = session();
        long compound = addCompound("the strong one");
        long strong = importFeature("strong", runId, 1.0e6, compound);
        long alsoStrong = addFeature("alsoStrong", 9.0e5);
        long middle = addFeature("middle", 5.0e5);
        long weak = addFeature("weak", 1.0e5);
        session.addToSearchIndex(ids(strong, alsoStrong, middle, weak), null);

        session.deleteCompoundById(String.valueOf(compound));
        session.deleteAlignedFeaturesByIds(ids(alsoStrong, middle));

        assertEquals(List.of("weak"), namesMatching(session, "intensityApexRelative:[9 TO 11]"));
        assertEquals(List.of("weak"), namesMatching(session, "intensityApexRelative:[0 TO *]"));
    }

    /** Opening a project whose index is complete reads the stored maxima at most - it never derives them. */
    @Test
    @SneakyThrows
    public void testOpeningAProjectWithACompleteIndexDerivesNothing() {
        NoSQLProjectImpl first = session();
        long feature = addFeature("feature", 1.0e5);
        first.addToSearchIndex(ids(feature), null);
        // as if the project had been written before the maxima were stored
        ps.removeProjectProperty(SiriusProjectDocumentDatabase.PROJECT_MAXIMUM_INTENSITIES_KEY);

        NoSQLProjectImpl second = session();
        assertEquals(List.of("feature"), namesMatching(second, "intensityApexRelative:[50 TO *]"),
                "the index is still served");
        assertTrue(ps.findProjectMaximumIntensities().isEmpty(), "opening and reading must not derive the maxima");
    }
}
