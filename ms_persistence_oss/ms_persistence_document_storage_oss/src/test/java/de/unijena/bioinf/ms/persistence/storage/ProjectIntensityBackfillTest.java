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
import de.unijena.bioinf.babelms.CloseableIterator;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.Feature;
import de.unijena.bioinf.ms.persistence.model.core.trace.TraceRef;
import de.unijena.bioinf.ms.persistence.model.properties.ProjectMaximumIntensities;
import de.unijena.bioinf.ms.persistence.storage.nitrite.NitriteSirirusProject;
import de.unijena.bioinf.storage.db.nosql.Filter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
 * A project imported before schema version 4 holds no MS1 intensity for features without an LC-MS trace: a
 * peak-list import wrote the run feature without a value, an API import wrote no run feature at all. The
 * conversion to version 4 records what an import records now - the height of the precursor peak in the merged
 * MS1 - and leaves LC-MS features alone, whose run features have carried their intensity all along.
 */
public class ProjectIntensityBackfillTest {

    private Path location;
    private NitriteSirirusProject db;

    @BeforeEach
    public void openProject() throws IOException {
        location = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        db = new NitriteSirirusProject(location);
    }

    @AfterEach
    public void closeProject() throws IOException {
        db.close();
        Files.deleteIfExists(location);
    }

    /** Imports the first spectrum of a bundled peak list the way the peak-list import does now. */
    private AlignedFeatures importPeakList(String resource) throws Exception {
        try (InputStream in = Objects.requireNonNull(getClass().getResourceAsStream(resource))) {
            CloseableIterator<Ms2Experiment> it = new MsExperimentParser().getParser(resource).parseIterator(in, URI.create(resource));
            return db.importMs2ExperimentAsAlignedFeature(it.next());
        }
    }

    private List<Feature> runFeaturesOf(long alignedFeatureId) throws IOException {
        return db.getStorage().findStr(Filter.where("alignedFeatureId").eq(alignedFeatureId), Feature.class).toList();
    }

    /** Turns the project into one written before version 4: no intensity on the run features, version 3. */
    private void asBeforeVersion4(long alignedFeatureId) throws IOException {
        for (Feature runFeature : runFeaturesOf(alignedFeatureId)) {
            runFeature.setApexIntensity(null);
            db.getStorage().upsert(runFeature);
        }
        db.upsertProjectSchemaVersion(new ProjectSchemaVersion(0, 3));
    }

    /** Reopens the project, so nothing the session kept in memory survives into what is checked. */
    private void reopen() throws IOException {
        db.close();
        db = new NitriteSirirusProject(location);
    }

    @Test
    public void testAPeakListFeatureGetsTheIntensityAnImportRecords() throws Exception {
        AlignedFeatures imported = importPeakList("/peaklists/laudanosine.mgf");
        Double recorded = runFeaturesOf(imported.getAlignedFeatureId()).getFirst().getApexIntensity();
        assertNotNull(recorded, "the import records the precursor peak height - the value to come back to");

        asBeforeVersion4(imported.getAlignedFeatureId());
        reopen();
        assertTrue(ProjectSchemaMigrator.migrateIfNeeded(db), "an intensity was recorded, so the index is stale");

        assertEquals(recorded, runFeaturesOf(imported.getAlignedFeatureId()).getFirst().getApexIntensity());
        assertEquals(new ProjectSchemaVersion(0, 4), db.findProjectSchemaVersion().orElseThrow());
        assertFalse(ProjectSchemaMigrator.migrateIfNeeded(db), "converted once, and then never again");
    }

    /**
     * A project with LC-MS runs is an LC-MS project throughout - peak lists are never imported into one - so its
     * features all carry the intensity of their traces and there is nothing to record. The pass is not run at
     * all, which is what keeps it from reading every feature to find that out.
     */
    @Test
    public void testAnLcmsProjectIsNotWalkedForIntensities() throws Exception {
        db.getStorage().insert(de.unijena.bioinf.ms.persistence.model.core.run.LCMSRun.builder().name("run")
                .chromatography(de.unijena.bioinf.ms.persistence.model.core.run.Chromatography.LC).build());
        AlignedFeatures imported = importPeakList("/peaklists/laudanosine.mgf");
        asBeforeVersion4(imported.getAlignedFeatureId());
        reopen();

        assertFalse(ProjectSchemaMigrator.neededPasses(db, db.getStorage())
                .contains(ProjectSchemaMigrator.ConversionJob.Pass.FEATURE_INTENSITIES));
        ProjectSchemaMigrator.migrateIfNeeded(db);
        assertNull(runFeaturesOf(imported.getAlignedFeatureId()).getFirst().getApexIntensity(),
                "not walked, so not recorded");
        assertEquals(new ProjectSchemaVersion(0, 4), db.findProjectSchemaVersion().orElseThrow(), "and still brought up to date");
    }

    /**
     * Every pass over the features at once, as an old project needs them: they share one read of each feature
     * and one write, so what each of them fills in must survive the others - the flags, the compound and the
     * intensity all on the same feature.
     */
    @Test
    public void testTheFeaturePassesTogetherKeepWhatEachOfThemFilledIn() throws Exception {
        AlignedFeatures imported = importPeakList("/peaklists/laudanosine.mgf");
        long id = imported.getAlignedFeatureId();
        Double recorded = runFeaturesOf(id).getFirst().getApexIntensity();
        long zombie = imported.getCompoundId();
        asBeforeVersion4(id);
        AlignedFeatures stale = db.getStorage().getByPrimaryKey(id, AlignedFeatures.class).orElseThrow();
        stale.setHasMs1(false);
        stale.setHasMsMs(false);
        stale.setCompoundId(null); // what the LC/MS-align bug left
        db.getStorage().upsert(stale);
        reopen();

        ProjectSchemaMigrator.convert(db, java.util.EnumSet.of(ProjectSchemaMigrator.ConversionJob.Pass.FEATURES,
                ProjectSchemaMigrator.ConversionJob.Pass.FEATURE_INTENSITIES,
                ProjectSchemaMigrator.ConversionJob.Pass.COMPOUND_LINKING));
        reopen();

        AlignedFeatures converted = db.getStorage().getByPrimaryKey(id, AlignedFeatures.class).orElseThrow();
        assertTrue(converted.isHasMs1(), "the flags");
        assertTrue(converted.isHasMsMs(), "the flags");
        assertNotNull(converted.getCompoundId(), "the compound");
        assertNotEquals(zombie, converted.getCompoundId(), "a fresh one");
        assertTrue(db.getStorage().getByPrimaryKey(zombie, de.unijena.bioinf.ms.persistence.model.core.Compound.class).isEmpty(),
                "the zombie is gone");
        assertEquals(1, db.getStorage().countAll(de.unijena.bioinf.ms.persistence.model.core.Compound.class));
        assertEquals(recorded, runFeaturesOf(id).getFirst().getApexIntensity(), "the intensity");
    }

    @Test
    public void testAnApiImportWithoutARunFeatureGetsOne() throws Exception {
        AlignedFeatures imported = importPeakList("/peaklists/laudanosine.mgf");
        Double recorded = runFeaturesOf(imported.getAlignedFeatureId()).getFirst().getApexIntensity();
        // an API import before version 4 wrote no run feature at all
        db.getStorage().removeAll(Filter.where("alignedFeatureId").eq(imported.getAlignedFeatureId()), Feature.class);
        db.upsertProjectSchemaVersion(new ProjectSchemaVersion(0, 3));
        reopen();

        ProjectSchemaMigrator.migrateIfNeeded(db);

        List<Feature> runFeatures = runFeaturesOf(imported.getAlignedFeatureId());
        assertEquals(1, runFeatures.size(), "one run feature, as an import writes now");
        assertEquals(recorded, runFeatures.getFirst().getApexIntensity());
        assertNull(runFeatures.getFirst().getRunId(), "it belongs to no run");
        assertEquals(imported.getAverageMass(), runFeatures.getFirst().getAverageMass());
    }

    @Test
    public void testAnLcmsFeatureIsLeftAlone() throws Exception {
        // an LC-MS feature: a merged trace, and a run feature that has no intensity here only because this
        // test removed it - the conversion must not read a spectrum to replace it
        AlignedFeatures imported = importPeakList("/peaklists/laudanosine.mgf");
        imported.setTraceRef(new TraceRef(42L, 0, 1, 2, 3));
        db.getStorage().upsert(imported);
        asBeforeVersion4(imported.getAlignedFeatureId());
        reopen();

        ProjectSchemaMigrator.migrateIfNeeded(db);

        assertNull(runFeaturesOf(imported.getAlignedFeatureId()).getFirst().getApexIntensity());
        assertEquals(new ProjectSchemaVersion(0, 4), db.findProjectSchemaVersion().orElseThrow(), "and the project is current all the same");
    }

    @Test
    public void testAFeatureWithoutAnMs1StaysWithoutAnIntensity() throws Exception {
        AlignedFeatures imported = importPeakList("/peaklists/laudanosine.mgf");
        asBeforeVersion4(imported.getAlignedFeatureId());
        db.fetchMsData(imported);
        var msData = imported.getMSData().orElseThrow();
        msData.setMergedMs1Spectrum(null);
        db.getStorage().upsert(msData);
        reopen();

        assertFalse(ProjectSchemaMigrator.migrateIfNeeded(db), "nothing was recorded, so the index still holds");

        assertNull(runFeaturesOf(imported.getAlignedFeatureId()).getFirst().getApexIntensity());
    }

    @Test
    public void testTheRecordedIntensityReachesTheStoredMaxima() throws Exception {
        AlignedFeatures imported = importPeakList("/peaklists/laudanosine.mgf");
        Double recorded = runFeaturesOf(imported.getAlignedFeatureId()).getFirst().getApexIntensity();
        asBeforeVersion4(imported.getAlignedFeatureId());
        // maxima stored before the conversion, without the value it records
        db.upsertProjectMaximumIntensities(new ProjectMaximumIntensities());
        reopen();

        ProjectSchemaMigrator.migrateIfNeeded(db);

        assertEquals(recorded, db.findProjectMaximumIntensities().orElseThrow().getGlobal().getApex());
    }
}
