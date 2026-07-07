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
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase-5 (M2): {@code close(compact=true)} must reanchor the persisted index versions without ever holding
 * two Nitrite handles on the same project file at once, and must leave the compacted project (DB and index)
 * intact and restorable. Uses a real DB-backed, on-disk search index so reanchor actually updates persisted
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
}
