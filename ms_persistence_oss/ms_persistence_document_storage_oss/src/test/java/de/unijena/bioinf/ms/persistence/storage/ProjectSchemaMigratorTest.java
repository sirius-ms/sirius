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
import de.unijena.bioinf.ms.persistence.model.sirius.CsiStructureMatch;
import de.unijena.bioinf.ms.persistence.model.sirius.CsiStructureSearchResult;
import de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate;
import de.unijena.bioinf.ms.persistence.storage.nitrite.NitriteSirirusProject;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class ProjectSchemaMigratorTest {

    /** Runs the feature pass and reports how many structure-search results it filled in. */
    private static int structureLinksOf(NitriteSirirusProject db) throws Exception {
        return structureLinksOf(db, ProjectSchemaMigrator.DEFAULT_CANDIDATE_CACHE);
    }

    private static int structureLinksOf(NitriteSirirusProject db,
                                        ProjectSchemaMigrator.CandidateCache cache) throws Exception {
        return ProjectSchemaMigrator.convert(db, EnumSet.of(ProjectSchemaMigrator.ConversionJob.Pass.FEATURES),
                cache, ProjectSchemaMigrator.DEFAULT_CANDIDATE_CACHE_LIMIT).getStructureResultsFilled().get();
    }

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
            assertEquals(ProjectSchemaMigrator.MIGRATES_TO_SCHEMA_VERSION, db.findProjectSchemaVersion().orElseThrow());

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

            assertTrue(ProjectSchemaMigrator.migrateIfNeeded(db),
                    "a project without a version is converted, and converting means rebuilding the index");

            AlignedFeatures reloaded = db.getStorage().getByPrimaryKey(imported.getAlignedFeatureId(), AlignedFeatures.class).orElseThrow();
            assertTrue(reloaded.isHasMsMs(), "existing correct flag must be preserved");
            assertEquals(ProjectSchemaMigrator.MIGRATES_TO_SCHEMA_VERSION, db.findProjectSchemaVersion().orElseThrow());
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

    // ---- the two backfills that complete version 1 -----------------------------------------------------

    /**
     * What both backfills are gated on, and the reason they can be: a field a record simply does not have is
     * told apart from one it has and left empty. If a null were stored by leaving the key out, the lipid probe
     * could not tell a project that predates the field from one where the first formula is not a lipid - which
     * is the common case - and every open would re-read every stored tree.
     */
    @Test
    public void testAnEmptyValueIsStoredAsAKeyRatherThanAsNothing() {
        withFreshDb(db -> {
            db.getStorage().insert(FormulaCandidate.builder().alignedFeatureId(1)
                    .molecularFormula(de.unijena.bioinf.ChemistryBase.chem.MolecularFormula.parseOrThrow("C6H12O6"))
                    .adduct(PrecursorIonType.fromString("[M + H]+"))
                    .build()); // not a lipid

            assertTrue(db.getStorage().isFieldPresent("lipidSpecies", FormulaCandidate.class),
                    "a formula that is not a lipid still carries the field, or the probe cannot gate on it");
        });
    }

    /**
     * A repository with nothing in it has nothing missing - otherwise a project that never ran a structure
     * search would look like one that needs repairing, on every open.
     */
    @Test
    public void testAnEmptyRepositoryCountsAsComplete() {
        withFreshDb(db -> {
            assertTrue(db.getStorage().isFieldPresent("matchedDatabases", CsiStructureSearchResult.class));
            assertTrue(db.getStorage().isFieldPresent("lipidSpecies", FormulaCandidate.class));

            // an empty project still records that it is current, so the next open is the fast path
            ProjectSchemaMigrator.migrateIfNeeded(db);
            assertEquals(ProjectSchemaMigrator.MIGRATES_TO_SCHEMA_VERSION, db.findProjectSchemaVersion().orElseThrow());
        });
    }

    /** A structure match of the given rank, on a candidate linked to the given databases. */
    private static void insertMatch(NitriteSirirusProject db, long featureId, String inchiKey, int rank,
                                    String... databases) throws Exception {
        de.unijena.bioinf.chemdb.FingerprintCandidate candidate = new de.unijena.bioinf.chemdb.FingerprintCandidate(
                de.unijena.bioinf.ChemistryBase.chem.InChIs.newInChI(inchiKey, "InChI=1S/CH4/h1H4"), null);
        candidate.setLinks(java.util.Arrays.stream(databases)
                .map(name -> new de.unijena.bioinf.chemdb.DBLink(name, "id")).toList());
        db.getStorage().insert(candidate);
        db.getStorage().insert(CsiStructureMatch.builder().alignedFeatureId(featureId)
                .candidateInChiKey(candidate.getInchiKey2D()).structureRank(rank).build());
    }

    /**
     * The feature a structure-search result belongs to. The conversion walks the feature keys and finds the
     * result under the same key, so a result whose feature is missing is not reachable - which in a real project
     * cannot happen, since the id is the feature's.
     */
    private static void insertFeature(NitriteSirirusProject db, long alignedFeatureId) throws Exception {
        db.getStorage().insert(AlignedFeatures.builder().alignedFeatureId(alignedFeatureId).build());
    }

    /**
     * The substance of the structure-database repair: the map is the best (lowest) rank each database was hit
     * at, taken from the links of the candidate behind every structure match - which is how the search itself
     * builds it, and all of it is still stored.
     */
    @Test
    public void testTheDatabaseLinksAreRecomputedFromTheStoredMatches() {
        withFreshDb(db -> {
            // the better-ranked match is in PubChem only, the worse one in PubChem and GNPS - so the two
            // databases must end up with different ranks, and PubChem with the better of its two
            insertFeature(db, 7L);
            insertMatch(db, 7L, "AAAAAAAAAAAAAA-UHFFFAOYSA-N", 1, "PubChem");
            insertMatch(db, 7L, "BBBBBBBBBBBBBB-UHFFFAOYSA-N", 3, "PubChem", "GNPS");
            db.getStorage().insert(CsiStructureSearchResult.builder().alignedFeatureId(7L).build());

            assertEquals(1, structureLinksOf(db));

            java.util.Map<String, Integer> matched = db.getStorage()
                    .getByPrimaryKey(7L, CsiStructureSearchResult.class).orElseThrow().getMatchedDatabases();
            assertEquals(Set.of("PubChem", "GNPS"), matched.keySet(), "every database the matches link to");
            assertEquals(1, matched.get("PubChem"), "the best rank it was hit at, not the last one seen");
            assertEquals(3, matched.get("GNPS"), "the only rank it was hit at");
        });
    }

    /**
     * The schema and the thing that migrates to it have to agree.
     * <p>
     * The data schema says what a project written now looks like; the migrator says what it can bring an older
     * project up to. Raising the first without teaching the second is the mistake this catches - it would leave
     * converted projects a version behind newly created ones, silently missing whatever the new version added,
     * and nothing at runtime would look wrong.
     */
    @Test
    public void testTheMigratorMigratesToTheSchemaTheProjectIsWrittenWith() {
        assertEquals(SiriusProjectDocumentDatabase.CURRENT_PROJECT_SCHEMA_VERSION,
                ProjectSchemaMigrator.MIGRATES_TO_SCHEMA_VERSION,
                "the data schema was raised without adapting the conversion to it");
    }

    /**
     * A project from a newer SIRIUS is left alone rather than half-converted by a migrator that does not know
     * what it would be converting to.
     */
    @Test
    public void testAProjectNewerThanTheMigratorIsLeftAlone() {
        withFreshDb(db -> {
            db.upsertProjectSchemaVersion(ProjectSchemaMigrator.MIGRATES_TO_SCHEMA_VERSION + 1);

            assertFalse(ProjectSchemaMigrator.migrateIfNeeded(db), "nothing this migrator can do");
            assertEquals(ProjectSchemaMigrator.MIGRATES_TO_SCHEMA_VERSION + 1,
                    db.findProjectSchemaVersion().orElseThrow(),
                    "and it must not be stamped back down to what this build knows");
        });
    }

    /**
     * All three ways of finding out which databases a candidate is in must answer the same thing.
     * <p>
     * Which one is fastest is a property of the project rather than of the code, so all three exist and any can
     * be switched on - which is only safe while they cannot disagree.
     */
    @Test
    public void testAllThreeWaysOfReadingTheDatabasesAgree() {
        for (ProjectSchemaMigrator.CandidateCache cache
                : ProjectSchemaMigrator.CandidateCache.values())
            withFreshDb(db -> {
                {
                    insertFeature(db, 11L);
                    insertMatch(db, 11L, "AAAAAAAAAAAAAA-UHFFFAOYSA-N", 1, "PubChem");
                    insertMatch(db, 11L, "BBBBBBBBBBBBBB-UHFFFAOYSA-N", 3, "PubChem", "GNPS");
                    // a match whose candidate the project no longer holds, which neither way may trip over
                    db.getStorage().insert(CsiStructureMatch.builder().alignedFeatureId(11L)
                            .candidateInChiKey("CCCCCCCCCCCCCC-UHFFFAOYSA-N").structureRank(2).build());
                    db.getStorage().insert(CsiStructureSearchResult.builder().alignedFeatureId(11L).build());

                    assertEquals(1, structureLinksOf(db, cache), "cache=" + cache);

                    java.util.Map<String, Integer> matched = db.getStorage()
                            .getByPrimaryKey(11L, CsiStructureSearchResult.class).orElseThrow()
                            .getMatchedDatabases();
                    assertEquals(Set.of("PubChem", "GNPS"), matched.keySet(), "cache=" + cache);
                    assertEquals(1, matched.get("PubChem"), "cache=" + cache);
                    assertEquals(3, matched.get("GNPS"), "cache=" + cache);
                }
            });
    }

    /**
     * A structure-search result whose matches carry no links is filled with an empty map rather than left
     * alone: null is what "never asked" looks like, so leaving it would ask again on every open.
     */
    @Test
    public void testAResultWithoutLinksIsStillMarkedAsDone() {
        withFreshDb(db -> {
            insertFeature(db, 9L);
            db.getStorage().insert(CsiStructureSearchResult.builder().alignedFeatureId(9L).build());

            assertEquals(1, structureLinksOf(db));

            assertNotNull(db.getStorage().getByPrimaryKey(9L, CsiStructureSearchResult.class)
                    .orElseThrow().getMatchedDatabases());
        });
    }

    /**
     * The fast path: a project that records the current version and passes the cheap probe does nothing at all,
     * however much is in it - which is what keeps opening a healthy project from costing anything.
     */
    @Test
    public void testACurrentProjectIsLeftAlone() {
        withFreshDb(db -> {
            db.getStorage().insert(CsiStructureSearchResult.builder().alignedFeatureId(3L)
                    .matchedDatabases(java.util.Map.of("PubChem", 1)).build());
            db.upsertProjectSchemaVersion(ProjectSchemaMigrator.MIGRATES_TO_SCHEMA_VERSION);

            assertFalse(ProjectSchemaMigrator.migrateIfNeeded(db), "nothing to do, so no index rebuild");

            assertEquals(java.util.Map.of("PubChem", 1), db.getStorage()
                    .getByPrimaryKey(3L, CsiStructureSearchResult.class).orElseThrow().getMatchedDatabases(),
                    "and nothing rewritten");
        });
    }

    /**
     * An old project is recognised by its version, whatever the probes say - the version is what the expensive
     * steps are allowed to run on.
     */
    @Test
    public void testAProjectWithoutAVersionIsMigratedAndStamped() {
        withOldProject(db -> {
            assertTrue(db.findProjectSchemaVersion().orElse(0) < ProjectSchemaMigrator.MIGRATES_TO_SCHEMA_VERSION,
                    "the fixture is an old project");

            ProjectSchemaMigrator.migrateIfNeeded(db);

            assertEquals(ProjectSchemaMigrator.MIGRATES_TO_SCHEMA_VERSION,
                    db.findProjectSchemaVersion().orElseThrow(), "stamped once everything returned");
        });
    }
}