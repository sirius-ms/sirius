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

package de.unijena.bioinf.ms.persistence.storage;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.Feature;
import de.unijena.bioinf.ms.persistence.model.properties.ProjectMaximumIntensities;
import de.unijena.bioinf.ms.persistence.storage.nitrite.NitriteSirirusProject;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The storage layer keeps the project's maximum intensities current for every feature it writes, so the
 * reference of a relative intensity never depends on who wrote the data. Values are raw instrument counts in
 * the range LC-MS data reaches: apex intensities around 10^5..10^7, areas one to two orders above.
 */
public class ProjectMaximumIntensitiesTest {

    private static final long RUN_A = 1, RUN_B = 2;

    private Path location;
    private NitriteSirirusProject ps;

    @BeforeEach
    public void openProject() throws IOException {
        location = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        ps = new NitriteSirirusProject(location);
    }

    @AfterEach
    public void closeProject() throws IOException {
        ps.close();
        Files.deleteIfExists(location);
    }

    private static Feature runFeature(@Nullable Long runId, @Nullable Double apex, double auc) {
        return Feature.builder().runId(runId).apexIntensity(apex).areaUnderCurve(auc).averageMass(301.141).build();
    }

    private void importFeature(Feature feature) throws IOException {
        ps.importAlignedFeatures(List.of(AlignedFeatures.singleton(feature)));
    }

    private ProjectMaximumIntensities stored() {
        return ps.findProjectMaximumIntensities().orElseThrow(() -> new AssertionError("no maxima stored"));
    }

    @Test
    public void testANewProjectTracksItsMaximaFromTheFirstImport() throws IOException {
        assertTrue(ps.findProjectMaximumIntensities().isEmpty(), "nothing imported, nothing to track");

        importFeature(runFeature(RUN_A, 1.2e6, 3.4e7));

        assertEquals(1.2e6, stored().run(RUN_A).getApex());
        assertEquals(3.4e7, stored().run(RUN_A).getAuc());
        assertEquals(1.2e6, stored().getGlobal().getApex());
        assertFalse(stored().isFeaturesWithoutRun());
    }

    @Test
    public void testAStrongerFeatureRaisesItsRunAndTheGlobalMaximum() throws IOException {
        importFeature(runFeature(RUN_A, 1.2e6, 3.4e7));
        importFeature(runFeature(RUN_B, 8.0e5, 2.1e7));
        importFeature(runFeature(RUN_A, 4.5e6, 9.0e7));

        assertEquals(4.5e6, stored().run(RUN_A).getApex());
        assertEquals(9.0e7, stored().run(RUN_A).getAuc());
        assertEquals(8.0e5, stored().run(RUN_B).getApex(), "another run's maximum is its own");
        assertEquals(4.5e6, stored().getGlobal().getApex());
    }

    @Test
    public void testAWeakerFeatureChangesNothing() throws IOException {
        importFeature(runFeature(RUN_A, 1.2e6, 3.4e7));
        ProjectMaximumIntensities before = stored();

        importFeature(runFeature(RUN_A, 2.0e5, 5.0e6));

        assertEquals(before, stored());
    }

    @Test
    public void testAFeatureWithoutARunCountsTowardsTheGlobalMaximumOnly() throws IOException {
        importFeature(runFeature(RUN_A, 1.2e6, 3.4e7));
        // a peak-list feature: the precursor peak height, and no area, since there is no trace
        importFeature(runFeature(null, 2.5e6, 0));

        assertEquals(1.2e6, stored().run(RUN_A).getApex(), "it belongs to no run");
        assertEquals(2.5e6, stored().getGlobal().getApex());
        assertEquals(3.4e7, stored().getGlobal().getAuc(), "an area only comes from run features");
        assertTrue(stored().isFeaturesWithoutRun(), "the global maximum now has something to refer to");
    }

    @Test
    public void testFeaturesWithoutAnIntensityAreIgnored() throws IOException {
        importFeature(runFeature(RUN_A, 1.2e6, 3.4e7));
        importFeature(runFeature(RUN_A, null, 0));
        importFeature(runFeature(RUN_A, -5.0, -1.0)); // nonsense from a broken import must not lower anything

        assertEquals(1.2e6, stored().run(RUN_A).getApex());
        assertEquals(3.4e7, stored().run(RUN_A).getAuc());
    }

    @Test
    public void testDeletingTheStrongestFeatureLeavesTheMaximum() throws IOException {
        importFeature(runFeature(RUN_A, 1.2e6, 3.4e7));
        AlignedFeatures strongest = AlignedFeatures.singleton(runFeature(RUN_A, 4.5e6, 9.0e7));
        ps.importAlignedFeatures(List.of(strongest));

        ps.cascadeDeleteAlignedFeatures(strongest.getAlignedFeatureId());

        assertEquals(4.5e6, stored().run(RUN_A).getApex(), "a maximum only ever rises");
    }

    @Test
    public void testTheMaximaSurviveReopeningTheProject() throws IOException {
        importFeature(runFeature(RUN_A, 1.2e6, 3.4e7));
        ProjectMaximumIntensities before = stored();
        ps.close();

        ps = new NitriteSirirusProject(location);

        assertEquals(before, stored());
        assertEquals(before, ps.getMaximumIntensities(), "read, not derived again");
    }

    @Test
    public void testAProjectWithoutTheMaximaDerivesThemOnceFromEveryFeature() throws IOException {
        // a project written before the maxima were stored: features, and no property
        importFeature(runFeature(RUN_A, 1.2e6, 3.4e7));
        ps.removeProjectProperty(SiriusProjectDocumentDatabase.PROJECT_MAXIMUM_INTENSITIES_KEY);
        ps.close();
        ps = new NitriteSirirusProject(location);

        // a write before anything asked for the maxima must not leave a partial value behind
        importFeature(runFeature(RUN_B, 8.0e5, 2.1e7));
        assertTrue(ps.findProjectMaximumIntensities().isEmpty(), "only the features written since would be in it");

        ProjectMaximumIntensities derived = ps.getMaximumIntensities();
        assertEquals(1.2e6, derived.run(RUN_A).getApex());
        assertEquals(8.0e5, derived.run(RUN_B).getApex());
        assertEquals(derived, stored(), "derived once, and kept");

        importFeature(runFeature(RUN_B, 9.9e5, 2.2e7));
        assertEquals(9.9e5, stored().run(RUN_B).getApex(), "and folded into from then on");
    }

    /** Enough features that the derivation is split across workers; what it finds must not depend on that. */
    @Test
    public void testDerivingTheMaximaOfManyFeaturesFindsWhatFoldingThemOneByOneDoes() throws IOException {
        java.util.Random random = new java.util.Random(42);
        ProjectMaximumIntensities expected = new ProjectMaximumIntensities();
        List<AlignedFeatures> batch = new java.util.ArrayList<>();
        for (int i = 0; i < 5000; i++) {
            Long runId = switch (i % 4) {
                case 0 -> RUN_A;
                case 1 -> RUN_B;
                case 2 -> 3L;
                default -> null; // a peak-list feature
            };
            Double apex = i % 50 == 0 ? null : Math.exp(random.nextDouble(8, 16));
            Feature feature = runFeature(runId, apex, Math.exp(random.nextDouble(10, 18)));
            expected.fold(List.of(feature));
            batch.add(AlignedFeatures.singleton(feature));
        }
        ps.importAlignedFeatures(batch);
        ps.removeProjectProperty(SiriusProjectDocumentDatabase.PROJECT_MAXIMUM_INTENSITIES_KEY);
        ps.close();
        ps = new NitriteSirirusProject(location);

        assertEquals(expected, ps.getMaximumIntensities());
        assertEquals(expected, stored(), "derived once, and kept");
    }

    @Test
    public void testConcurrentImportsAllLandInTheMaxima() throws Exception {
        importFeature(runFeature(RUN_A, 1.0e5, 1.0e6));
        int writers = 8;
        ExecutorService pool = Executors.newFixedThreadPool(writers);
        try {
            List<Future<?>> done = new java.util.ArrayList<>();
            for (int i = 1; i <= writers; i++) {
                long runId = 100 + i;
                double apex = 1.0e6 * i;
                done.add(pool.submit(() -> {
                    importFeature(runFeature(runId, apex, apex * 20));
                    return null;
                }));
            }
            for (Future<?> f : done)
                f.get();
        } finally {
            pool.shutdown();
        }

        for (int i = 1; i <= writers; i++)
            assertEquals(1.0e6 * i, stored().run(100 + i).getApex(), "run " + (100 + i) + " lost its maximum");
        assertEquals(1.0e6 * writers, stored().getGlobal().getApex());
    }
}
