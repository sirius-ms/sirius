package de.unijena.bioinf.ms.persistence.storage;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.utils.ExFunctions;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.babelms.CloseableIterator;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.DetectedAdduct;
import de.unijena.bioinf.ms.persistence.model.core.feature.DetectedAdducts;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.MSData;
import de.unijena.bioinf.ms.persistence.storage.nitrite.NitriteSirirusProject;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class ProjectSchemaMigratorTest {

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

    private static void withOldProject(ExFunctions.Consumer<NitriteSirirusProject> consumer) {
        Path path = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        try {
            try (InputStream s = ProjectSchemaMigratorTest.class.getResourceAsStream("/sirius-project-features.sirius")) {
                Files.write(path, Objects.requireNonNull(s).readAllBytes());
            }
            try (NitriteSirirusProject ps = new NitriteSirirusProject(path)) {
                consumer.accept(ps);
            } finally {
                Files.deleteIfExists(path);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static AlignedFeatures importFirst(NitriteSirirusProject db, String resource) throws Exception {
        try (InputStream in = Objects.requireNonNull(ProjectSchemaMigratorTest.class.getResourceAsStream(resource))) {
            CloseableIterator<Ms2Experiment> it = new MsExperimentParser().getParser(resource).parseIterator(in, URI.create(resource));
            return db.importMs2ExperimentAsAlignedFeature(it.next());
        }
    }

    // ---- pure compute logic -------------------------------------------------

    @Test
    public void testComputeFlagsFromMsData() {
        assertFalse(ProjectSchemaMigrator.computeHasMs1(null));
        assertFalse(ProjectSchemaMigrator.computeHasMsMs(null));

        MSData empty = MSData.builder().build();
        assertFalse(ProjectSchemaMigrator.computeHasMs1(empty));
        assertFalse(ProjectSchemaMigrator.computeHasMsMs(empty));
    }

    // laudanosine.mgf carries MS/MS -> the imported feature must resolve hasMsMs=true
    @Test
    public void testComputeFlagsTrueForMsMsData() {
        withFreshDb(db -> {
            AlignedFeatures f = importFirst(db, "/peaklists/laudanosine.mgf");
            db.fetchMsData(f);
            MSData d = f.getMSData().orElseThrow();
            assertTrue(ProjectSchemaMigrator.computeHasMsMs(d), "laudanosine should have MS/MS");
            assertTrue(f.isHasMsMs(), "import should already set hasMsMs=true");
        });
    }

    // ---- raw-document field-existence probe ---------------------------------

    @Test
    public void testIsFieldPresentDistinguishesOldVsNew() {
        // old project predates the flags -> field absent on the feature documents
        withOldProject(db ->
                assertFalse(db.getStorage().isFieldPresent("hasMsMs", AlignedFeatures.class),
                        "old project features must not carry the hasMsMs field"));

        // freshly imported feature -> field present
        withFreshDb(db -> {
            importFirst(db, "/peaklists/laudanosine.mgf");
            assertTrue(db.getStorage().isFieldPresent("hasMsMs", AlignedFeatures.class),
                    "freshly imported features must carry the hasMsMs field");
        });
    }

    // ---- end-to-end migration of an old project -----------------------------

    @Test
    public void testMigrateOldProjectBackfillsFlagsAndStampsVersion() {
        withOldProject(db -> {
            assertTrue(db.findProjectSchemaVersion().isEmpty(), "old project must not be versioned");
            assertFalse(db.getStorage().isFieldPresent("hasMsMs", AlignedFeatures.class));

            assertTrue(ProjectSchemaMigrator.migrateIfNeeded(db), "flag backfill must request an index rebuild");

            // flags are now materialized on every feature document ...
            assertTrue(db.getStorage().isFieldPresent("hasMsMs", AlignedFeatures.class));
            assertTrue(db.getStorage().isFieldPresent("hasMs1", AlignedFeatures.class));

            // ... with values consistent with the actual MS data
            for (AlignedFeatures f : db.getStorage().findAllStr(AlignedFeatures.class).toList()) {
                db.fetchMsData(f);
                MSData d = f.getMSData().orElse(null);
                assertEquals(ProjectSchemaMigrator.computeHasMsMs(d), f.isHasMsMs());
                assertEquals(ProjectSchemaMigrator.computeHasMs1(d), f.isHasMs1());
            }

            // version stamped
            assertEquals(ProjectSchemaMigrator.CURRENT_SCHEMA_VERSION, db.findProjectSchemaVersion().orElseThrow());

            // this fixture has no detected adducts on any feature -> nothing to backfill, project property stays absent
            assertTrue(db.findDetectedAdducts().isEmpty(), "no real adducts to backfill for this fixture");
        });
    }

    @Test
    public void testMigrateIsSkippedWhenVersionCurrent() {
        withOldProject(db -> {
            ProjectSchemaMigrator.migrateIfNeeded(db); // -> stamps current version

            // corrupt a flag: an iso-only feature gets hasMsMs=true (wrong). A re-run must NOT touch it,
            // proving the version gate short-circuits before any recompute.
            AlignedFeatures f = db.getStorage().findAllStr(AlignedFeatures.class).findFirst().orElseThrow();
            f.setHasMsMs(true);
            db.getStorage().upsert(f);

            ProjectSchemaMigrator.migrateIfNeeded(db);

            AlignedFeatures reloaded = db.getStorage().getByPrimaryKey(f.getAlignedFeatureId(), AlignedFeatures.class).orElseThrow();
            assertTrue(reloaded.isHasMsMs(), "current-version project must not be re-migrated");
        });
    }

    @Test
    public void testMigratePreservesFlagsWhenAlreadyPresent() {
        withFreshDb(db -> {
            AlignedFeatures imported = importFirst(db, "/peaklists/laudanosine.mgf");
            assertTrue(db.findProjectSchemaVersion().isEmpty());

            assertFalse(ProjectSchemaMigrator.migrateIfNeeded(db), "no flag backfill -> no forced index rebuild");

            AlignedFeatures reloaded = db.getStorage().getByPrimaryKey(imported.getAlignedFeatureId(), AlignedFeatures.class).orElseThrow();
            assertTrue(reloaded.isHasMsMs(), "existing correct flag must be preserved");
            assertEquals(ProjectSchemaMigrator.CURRENT_SCHEMA_VERSION, db.findProjectSchemaVersion().orElseThrow());
        });
    }

    // ---- detected-adducts backfill ------------------------------------------

    @Test
    public void testMigrateBackfillsProjectDetectedAdducts() {
        withFreshDb(db -> {
            AlignedFeatures f = importFirst(db, "/peaklists/laudanosine.mgf");
            DetectedAdducts adducts = new DetectedAdducts().addAll(
                    DetectedAdduct.builder().adduct(PrecursorIonType.fromString("[M+H]+")).score(.9)
                            .source(de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts.Source.LCMS_ALIGN).build(),
                    DetectedAdduct.builder().adduct(PrecursorIonType.fromString("[M+Na]+")).score(.1)
                            .source(de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts.Source.LCMS_ALIGN).build());
            f.setDetectedAdducts(adducts);
            db.getStorage().upsert(f);
            assertTrue(db.findDetectedAdducts().isEmpty(), "project property must be absent before migration");

            ProjectSchemaMigrator.migrateIfNeeded(db);

            Set<PrecursorIonType> expected = f.getDetectedAdducts().getAllAdducts().stream().collect(Collectors.toSet());
            Set<PrecursorIonType> actual = db.findDetectedAdducts().orElseThrow().getDetectedAdductsAsIonTypes();
            assertEquals(expected, actual, "project detected adducts must equal the union of feature adducts");
        });
    }
}
