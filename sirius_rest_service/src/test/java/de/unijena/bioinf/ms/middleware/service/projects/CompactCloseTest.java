package de.unijena.bioinf.ms.middleware.service.projects;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ms.middleware.model.features.AlignedFeature;
import de.unijena.bioinf.ms.middleware.model.features.FeatureImport;
import de.unijena.bioinf.ms.middleware.model.spectra.BasicSpectrum;
import de.unijena.bioinf.ms.middleware.service.search.SearchService;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.NoSqlProjectSearchContextProvider;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.SearchServiceImpl;
import de.unijena.bioinf.ms.persistence.model.core.PersistentSearchIndex;
import de.unijena.bioinf.ms.persistence.storage.MsProjectDocumentDatabase;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase;
import de.unijena.bioinf.ms.persistence.storage.nitrite.NitriteSirirusProject;
import de.unijena.bioinf.projectspace.NoSQLProjectSpaceManager;
import de.unijena.bioinf.storage.db.nosql.Metadata;
import de.unijena.bioinf.storage.db.nosql.nitrite.NitriteDatabase;
import org.h2.mvstore.MVStore;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * What closing a project with compaction has to do, across both modes it offers.
 * <p>
 * Phase-5 (M2): {@code close(compact=true)} must reanchor the persisted index versions without ever holding
 * two Nitrite handles on the same project file at once, and must leave the compacted project (DB and index)
 * intact and restorable.
 * <p>
 * And (#971) a compaction now rewrites an outdated page layout rather than copying it, so these also cover
 * which of the two close flags makes that happen and that the index is reanchored when it does - a rewrite
 * restarts the store's commit version, so an index left anchored to the old one is silently stale.
 * <p>
 * Uses a real DB-backed, on-disk search index so reanchor actually updates persisted
 * {@link PersistentSearchIndex} records.
 */
public class CompactCloseTest {

    private static FeatureImport feature(String name, String externalFeatureId) {
        BasicSpectrum ms1 = new BasicSpectrum(new double[]{1, 2, 42}, new double[]{1, 2, 3});
        BasicSpectrum ms2 = new BasicSpectrum(new double[]{1, 2, 42}, new double[]{1, 2, 3});
        ms2.setCollisionEnergy(CollisionEnergy.fromString("20eV"));
        ms2.setMsLevel(2);
        ms2.setPrecursorMz(42d);
        ms2.setScanNumber(5);
        return FeatureImport.builder()
                .externalFeatureId(externalFeatureId)
                .name(name)
                .ionMass(42d)
                .charge((byte) 1)
                .detectedAdducts(java.util.Set.of("M+H+"))
                .rtStartSeconds(6d)
                .rtApexSeconds(10d)
                .rtEndSeconds(12d)
                .mergedMs1(ms1)
                .ms1Spectra(List.of(ms1))
                .ms2Spectra(List.of(ms2, ms2))
                .build();
    }

    @Test
    public void closeWithCompactReanchorsWithoutHandleConflict() throws Exception {
        Path location = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        Path indexHome1 = Files.createTempDirectory("compact-index-1");
        Path indexHome2 = Files.createTempDirectory("compact-index-2");
        try {
            // Session 1: real project with a DB-backed, on-disk search index; index one feature.
            NitriteSirirusProject ps = new NitriteSirirusProject(location);
            SearchService searchService = new SearchServiceImpl(new NoSqlProjectSearchContextProvider(false, indexHome1, true));
            NoSQLProjectImpl project = new NoSQLProjectImpl("test", new NoSQLProjectSpaceManager(ps), searchService, (a, b) -> false);
            project.addAlignedFeatures(List.of(feature("alpha", "FID-A")), null, EnumSet.noneOf(AlignedFeature.OptField.class), "src");

            assertEquals(1, searchService.searchIds(project.getProjectId(), null, Pageable.unpaged(), AlignedFeature.class).getTotalElements(),
                    "feature should be indexed before compacting close");

            // Must not throw: reanchor now runs after the manager is closed, using a single fresh handle.
            assertDoesNotThrow(() -> project.close(true), "close(compact=true) must not fail on a handle conflict (M2)");

            // Reanchor must have written the persisted index record and anchored it to the compacted commit.
            Metadata metadata = MsProjectDocumentDatabase.buildMetadata();
            try (NitriteDatabase check = new NitriteDatabase(location, metadata)) {
                Optional<PersistentSearchIndex> saved = check.getByPrimaryKey("AlignedFeature", PersistentSearchIndex.class);
                assertTrue(saved.isPresent(), "AlignedFeature index must be persisted after compacting close (M2)");
                assertEquals(check.getStorageCommitId(), saved.get().getStorageCommitId(),
                        "reanchor must anchor the persisted index version to the compacted DB commit (M2)");
            }

            // Session 2: reopen the compacted project; DB data and the search index must be intact.
            NitriteSirirusProject ps2 = new NitriteSirirusProject(location);
            SearchService searchService2 = new SearchServiceImpl(new NoSqlProjectSearchContextProvider(false, indexHome2, true));
            NoSQLProjectImpl reopened = new NoSQLProjectImpl("test-2", new NoSQLProjectSpaceManager(ps2), searchService2, (a, b) -> false);
            try {
                assertEquals(1, reopened.findAlignedFeatures(Pageable.unpaged(), false,
                                EnumSet.noneOf(AlignedFeature.OptField.class)).getTotalElements(),
                        "compacted project must retain its DB data (M2)");
                assertEquals(1, searchService2.searchIds(reopened.getProjectId(), null, Pageable.unpaged(), AlignedFeature.class).getTotalElements(),
                        "compacted project must retain a working search index (M2)");
            } finally {
                reopened.close();
            }
        } finally {
            FileUtils.deleteRecursively(indexHome1);
            FileUtils.deleteRecursively(indexHome2);
            FileUtils.deleteRecursively(location);
        }
    }

    /**
     * Opens {@code location} as a middleware project with a DB-backed, on-disk index, hands it to
     * {@code body}, and leaves closing it to the body - which is what each of these tests varies.
     */
    private static void withProject(Path location, Path indexHome, String id,
                                    ThrowingBiConsumer<NoSQLProjectImpl, NoSQLProjectSpaceManager> body) throws Exception {
        NitriteSirirusProject ps = new NitriteSirirusProject(location);
        NoSQLProjectSpaceManager manager = new NoSQLProjectSpaceManager(ps);
        SearchService searchService = new SearchServiceImpl(new NoSqlProjectSearchContextProvider(false, indexHome, true));
        body.accept(new NoSQLProjectImpl(id, manager, searchService, (a, b) -> false), manager);
    }

    private interface ThrowingBiConsumer<A, B> {
        void accept(A a, B b) throws Exception;
    }

    /**
     * Leaves the store recording no page budget, which is how every release before this one left it -
     * MVStore does not persist {@code pageSplitSize}, so a project that predates the marker reads 0.
     * That is what makes a project look like it needs its layout rewritten.
     */
    private static void stampAsWrittenBeforeTheBudgetWasRecorded(Path location) {
        try (MVStore store = new MVStore.Builder().fileName(location.toString()).open()) {
            store.setStoreVersion(0);
            store.commit();
        }
    }

    /** The page budget the store records, which a rewrite sets to the one it split its pages against. */
    private static int recordedPageBudget(Path location) {
        try (MVStore store = new MVStore.Builder().fileName(location.toString()).readOnly().open()) {
            return store.getStoreVersion();
        }
    }

    /**
     * The GUI's "Full Compact" closes with {@code compact=false, rewriteLayout=true}, so the rewrite has to
     * happen on the strength of the second flag alone - and the index has to be reanchored even though the
     * first one is false, because a rewrite restarts the store's commit version.
     */
    @Test
    public void closeRewritingTheLayoutRepairsItAndReanchorsTheIndex() throws Exception {
        Path location = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        Path indexHome1 = Files.createTempDirectory("rewrite-index-1");
        Path indexHome2 = Files.createTempDirectory("rewrite-index-2");
        try {
            withProject(location, indexHome1, "rewrite", (project, manager) -> {
                project.addAlignedFeatures(List.of(feature("alpha", "FID-A")), null,
                        EnumSet.noneOf(AlignedFeature.OptField.class), "src");
                project.close(false);
            });
            stampAsWrittenBeforeTheBudgetWasRecorded(location);

            withProject(location, indexHome1, "rewrite-2", (project, manager) -> {
                assertTrue(manager.isCompactionRecommended(),
                        "a project recording no page budget has an outdated layout and has to say so");
                // compact=false on purpose: this is exactly what the GUI's "Full Compact" sends
                assertDoesNotThrow(() -> project.close(false, true),
                        "close(compact=false, rewriteLayout=true) must rewrite rather than do nothing");
            });

            assertEquals(NitriteDatabase.DEFAULT_PAGE_SPLIT_SIZE_BYTE, recordedPageBudget(location),
                    "the rewrite has to record the budget it split the pages against");

            Metadata metadata = MsProjectDocumentDatabase.buildMetadata();
            try (NitriteDatabase check = new NitriteDatabase(location, metadata)) {
                Optional<PersistentSearchIndex> saved = check.getByPrimaryKey("AlignedFeature", PersistentSearchIndex.class);
                assertTrue(saved.isPresent(), "the index must still be persisted after a rewriting close");
                assertEquals(check.getStorageCommitId(), saved.get().getStorageCommitId(),
                        "a rewrite restarts the commit version, so the index has to be reanchored to the new one");
            }

            withProject(location, indexHome2, "rewrite-3", (project, manager) -> {
                try {
                    assertFalse(manager.isCompactionRecommended(),
                            "a rewritten project must not keep asking to be compacted");
                    assertEquals(1, project.findAlignedFeatures(Pageable.unpaged(), false,
                                    EnumSet.noneOf(AlignedFeature.OptField.class)).getTotalElements(),
                            "a rewritten project must retain its DB data");
                } finally {
                    project.close();
                }
            });
        } finally {
            FileUtils.deleteRecursively(indexHome1);
            FileUtils.deleteRecursively(indexHome2);
            FileUtils.deleteRecursively(location);
        }
    }

    /**
     * The changelog's promise to users: compacting an affected project repairs it. An ordinary
     * {@code close(compact=true)} has to rewrite the layout when the storage reports it as outdated,
     * without the caller asking for a rewrite.
     */
    @Test
    public void closeWithCompactRepairsAnOutdatedLayout() throws Exception {
        Path location = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        Path indexHome = Files.createTempDirectory("repair-index");
        try {
            withProject(location, indexHome, "repair", (project, manager) -> {
                project.addAlignedFeatures(List.of(feature("alpha", "FID-A")), null,
                        EnumSet.noneOf(AlignedFeature.OptField.class), "src");
                project.close(false);
            });
            stampAsWrittenBeforeTheBudgetWasRecorded(location);

            withProject(location, indexHome, "repair-2", (project, manager) -> {
                assertTrue(manager.isCompactionRecommended(), "precondition: the layout has to look outdated");
                project.close(true);
            });

            assertEquals(NitriteDatabase.DEFAULT_PAGE_SPLIT_SIZE_BYTE, recordedPageBudget(location),
                    "an ordinary compaction of an outdated project has to rewrite its layout");
            withProject(location, indexHome, "repair-3", (project, manager) -> {
                try {
                    assertFalse(manager.isCompactionRecommended(), "the repaired project must report itself healthy");
                    assertEquals(1, project.findAlignedFeatures(Pageable.unpaged(), false,
                                    EnumSet.noneOf(AlignedFeature.OptField.class)).getTotalElements(),
                            "the repaired project must retain its DB data");
                } finally {
                    project.close();
                }
            });
        } finally {
            FileUtils.deleteRecursively(indexHome);
            FileUtils.deleteRecursively(location);
        }
    }

    /**
     * The other half of the same rule: a close that asks for neither must leave the store alone. Without
     * this, both tests above would still pass if a rewrite ran on every close.
     */
    @Test
    public void closeWithoutCompactLeavesAnOutdatedLayoutAlone() throws Exception {
        Path location = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        Path indexHome = Files.createTempDirectory("untouched-index");
        try {
            withProject(location, indexHome, "untouched", (project, manager) -> {
                project.addAlignedFeatures(List.of(feature("alpha", "FID-A")), null,
                        EnumSet.noneOf(AlignedFeature.OptField.class), "src");
                project.close(false);
            });
            stampAsWrittenBeforeTheBudgetWasRecorded(location);

            withProject(location, indexHome, "untouched-2", (project, manager) -> {
                assertTrue(manager.isCompactionRecommended(), "precondition: the layout has to look outdated");
                project.close(false, false);
            });

            assertEquals(0, recordedPageBudget(location),
                    "a close that asks for neither a compaction nor a rewrite must not rewrite the layout");
        } finally {
            FileUtils.deleteRecursively(indexHome);
            FileUtils.deleteRecursively(location);
        }
    }
}
