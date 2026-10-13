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

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.utils.ExFunctions;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.babelms.CloseableIterator;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.ms.persistence.model.core.Compound;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.storage.nitrite.NitriteSirirusProject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers {@link ProjectSchemaMigrator}'s {@code COMPOUND_LINKING} pass: the repair for the LC/MS-align bug
 * that left {@code AlignedFeatures.compoundId} null and the {@code Compound} its own {@code Compound.singleton}
 * call inserted an unreferenced zombie. Unlike the other passes, this one is probe-gated rather than
 * version-gated (see {@code ProjectSchemaMigrator#neededPasses}), so none of these cases touch
 * {@code upsertProjectSchemaVersion} directly - the "before" state only needs to violate one of the two probe
 * conditions: a feature with a null {@code compoundId}, or more compounds than features.
 */
public class ProjectCompoundLinkingBackfillTest {

    private static void withFreshDb(ExFunctions.Consumer<NitriteSirirusProject> consumer) {
        try {
            Path location = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
            try (NitriteSirirusProject ps = new NitriteSirirusProject(location)) {
                consumer.accept(ps);
            } finally {
                Files.deleteIfExists(location);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static AlignedFeatures importFirst(NitriteSirirusProject db, String resource) throws Exception {
        try (InputStream in = Objects.requireNonNull(ProjectCompoundLinkingBackfillTest.class.getResourceAsStream(resource))) {
            CloseableIterator<Ms2Experiment> it = new MsExperimentParser().getParser(resource).parseIterator(in, URI.create(resource));
            return db.importMs2ExperimentAsAlignedFeature(it.next());
        }
    }

    /**
     * Turns one correctly-linked feature into what the LC/MS-align bug would have left: {@code compoundId}
     * null, with the original, now-unreferenced compound row still in storage as the zombie.
     */
    private static void simulateHistoricalBug(NitriteSirirusProject db, AlignedFeatures feature) throws IOException {
        feature.setCompoundId(null);
        db.getStorage().upsert(feature);
    }

    @Test
    public void testAnOrphanedFeatureGetsLinkedAndItsZombieCompoundIsRemoved() {
        withFreshDb(db -> {
            AlignedFeatures feature = importFirst(db, "/peaklists/laudanosine.mgf");
            ProjectSchemaMigrator.migrateIfNeeded(db); // bring the project up to date before the bug is simulated
            long originalCompoundId = feature.getCompoundId();
            simulateHistoricalBug(db, feature);

            assertTrue(ProjectSchemaMigrator.migrateIfNeeded(db), "an orphaned feature's compound is index-relevant");

            AlignedFeatures relinked = db.getStorage().getByPrimaryKey(feature.getAlignedFeatureId(), AlignedFeatures.class).orElseThrow();
            assertNotNull(relinked.getCompoundId());
            assertNotEquals(originalCompoundId, relinked.getCompoundId(), "a fresh compound was created, not the original zombie");
            assertTrue(db.getStorage().getByPrimaryKey(originalCompoundId, Compound.class).isEmpty(), "the zombie is gone");
            assertEquals(1, db.getStorage().countAll(Compound.class));

            assertFalse(ProjectSchemaMigrator.migrateIfNeeded(db), "converted once, and then never again");
        });
    }

    @Test
    public void testAMultiFeatureCompoundIsLeftAlone() {
        withFreshDb(db -> {
            AlignedFeatures f1 = importFirst(db, "/peaklists/laudanosine.mgf");
            AlignedFeatures f2 = importFirst(db, "/peaklists/laudanosine.mgf");
            db.getStorage().removeByPrimaryKey(f1.getCompoundId(), Compound.class);
            db.getStorage().removeByPrimaryKey(f2.getCompoundId(), Compound.class);
            Compound shared = Compound.builder().adductFeatures(List.of(f1, f2)).build();
            db.getStorage().insert(shared);
            f1.setCompoundId(shared.getCompoundId());
            f2.setCompoundId(shared.getCompoundId());
            db.getStorage().upsert(f1);
            db.getStorage().upsert(f2);
            ProjectSchemaMigrator.migrateIfNeeded(db); // bring the project up to date before the bug is simulated

            // an unrelated orphaned feature elsewhere in the same project trips the probe and runs the pass
            AlignedFeatures orphan = importFirst(db, "/peaklists/laudanosine.mgf");
            simulateHistoricalBug(db, orphan);

            assertTrue(ProjectSchemaMigrator.migrateIfNeeded(db));

            AlignedFeatures f1After = db.getStorage().getByPrimaryKey(f1.getAlignedFeatureId(), AlignedFeatures.class).orElseThrow();
            AlignedFeatures f2After = db.getStorage().getByPrimaryKey(f2.getAlignedFeatureId(), AlignedFeatures.class).orElseThrow();
            assertEquals(shared.getCompoundId(), f1After.getCompoundId(), "an already-linked feature keeps its compound");
            assertEquals(shared.getCompoundId(), f2After.getCompoundId(), "an already-linked feature keeps its compound");
            assertTrue(db.getStorage().getByPrimaryKey(shared.getCompoundId(), Compound.class).isPresent(),
                    "the shared compound must survive untouched");
        });
    }

    @Test
    public void testMultipleOrphanedFeaturesEachGetTheirOwnCompoundAndNoOthersAreDeleted() {
        withFreshDb(db -> {
            AlignedFeatures linked = importFirst(db, "/peaklists/laudanosine.mgf");
            AlignedFeatures orphan1 = importFirst(db, "/peaklists/laudanosine.mgf");
            AlignedFeatures orphan2 = importFirst(db, "/peaklists/laudanosine.mgf");
            ProjectSchemaMigrator.migrateIfNeeded(db); // bring the project up to date before the bug is simulated

            long linkedCompoundId = linked.getCompoundId();
            long zombie1 = orphan1.getCompoundId();
            long zombie2 = orphan2.getCompoundId();
            simulateHistoricalBug(db, orphan1);
            simulateHistoricalBug(db, orphan2);

            assertTrue(ProjectSchemaMigrator.migrateIfNeeded(db));

            AlignedFeatures orphan1After = db.getStorage().getByPrimaryKey(orphan1.getAlignedFeatureId(), AlignedFeatures.class).orElseThrow();
            AlignedFeatures orphan2After = db.getStorage().getByPrimaryKey(orphan2.getAlignedFeatureId(), AlignedFeatures.class).orElseThrow();
            assertNotNull(orphan1After.getCompoundId());
            assertNotNull(orphan2After.getCompoundId());
            assertNotEquals(orphan1After.getCompoundId(), orphan2After.getCompoundId(), "each orphan gets its own compound");
            assertTrue(db.getStorage().getByPrimaryKey(zombie1, Compound.class).isEmpty());
            assertTrue(db.getStorage().getByPrimaryKey(zombie2, Compound.class).isEmpty());

            AlignedFeatures linkedAfter = db.getStorage().getByPrimaryKey(linked.getAlignedFeatureId(), AlignedFeatures.class).orElseThrow();
            assertEquals(linkedCompoundId, linkedAfter.getCompoundId(), "the already-linked feature/compound is untouched");
            assertTrue(db.getStorage().getByPrimaryKey(linkedCompoundId, Compound.class).isPresent());
        });
    }

    @Test
    public void testAZombieCompoundWithNoOrphanedFeatureStillTripsTheProbe() {
        withFreshDb(db -> {
            AlignedFeatures feature = importFirst(db, "/peaklists/laudanosine.mgf");
            ProjectSchemaMigrator.migrateIfNeeded(db); // bring the project up to date before the zombie is added

            // the feature is correctly linked, but a stray extra compound sits unreferenced - e.g. a
            // pre-existing orphan from before the cascadeDeleteAlignedFeatures fix
            Compound stray = Compound.builder().build();
            db.getStorage().insert(stray);

            assertTrue(ProjectSchemaMigrator.migrateIfNeeded(db),
                    "more compounds than features alone must trip the probe, with zero features to link");

            assertTrue(db.getStorage().getByPrimaryKey(stray.getCompoundId(), Compound.class).isEmpty());
            AlignedFeatures featureAfter = db.getStorage().getByPrimaryKey(feature.getAlignedFeatureId(), AlignedFeatures.class).orElseThrow();
            assertEquals(feature.getCompoundId(), featureAfter.getCompoundId(), "the correctly-linked feature is untouched");
            assertEquals(1, db.getStorage().countAll(Compound.class));
        });
    }

    @Test
    public void testAnAlreadyLinkedProjectIsANoOp() {
        withFreshDb(db -> {
            importFirst(db, "/peaklists/laudanosine.mgf");
            ProjectSchemaMigrator.migrateIfNeeded(db); // bring the project up to date

            assertFalse(ProjectSchemaMigrator.migrateIfNeeded(db), "nothing is orphaned, so there is nothing to correct");
        });
    }
}
