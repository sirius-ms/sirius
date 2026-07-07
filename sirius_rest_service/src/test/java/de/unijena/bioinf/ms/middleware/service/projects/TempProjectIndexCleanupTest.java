package de.unijena.bioinf.ms.middleware.service.projects;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ms.middleware.model.features.AlignedFeature;
import de.unijena.bioinf.ms.middleware.model.features.FeatureImport;
import de.unijena.bioinf.ms.middleware.model.spectra.BasicSpectrum;
import de.unijena.bioinf.ms.middleware.service.search.SearchService;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.NoSqlProjectSearchContextProvider;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.SearchServiceImpl;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase;
import de.unijena.bioinf.ms.persistence.storage.nitrite.NitriteSirirusProject;
import de.unijena.bioinf.projectspace.NoSQLProjectSpaceManager;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase-6 (Low B3): a temp project's on-disk search index must be deleted when the project is closed, so its
 * index directory does not linger under the indexing home until application shutdown.
 */
public class TempProjectIndexCleanupTest {

    private static FeatureImport feature(String name, String externalFeatureId) {
        BasicSpectrum ms1 = new BasicSpectrum(new double[]{1, 2, 42}, new double[]{1, 2, 3});
        BasicSpectrum ms2 = new BasicSpectrum(new double[]{1, 2, 42}, new double[]{1, 2, 3});
        ms2.setCollisionEnergy(CollisionEnergy.fromString("20eV"));
        ms2.setMsLevel(2);
        ms2.setPrecursorMz(42d);
        ms2.setScanNumber(5);
        return FeatureImport.builder()
                .externalFeatureId(externalFeatureId).name(name)
                .ionMass(42d).charge((byte) 1).detectedAdducts(java.util.Set.of("M+H+"))
                .rtStartSeconds(6d).rtApexSeconds(10d).rtEndSeconds(12d)
                .mergedMs1(ms1).ms1Spectra(List.of(ms1)).ms2Spectra(List.of(ms2, ms2))
                .build();
    }

    @Test
    public void tempProjectIndexDirIsDeletedOnClose() throws Exception {
        Path indexingHome = Files.createTempDirectory("b3-index-home");
        Path location = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        try {
            NitriteSirirusProject ps = new NitriteSirirusProject(location);
            NoSQLProjectSpaceManager psm = new NoSQLProjectSpaceManager(ps);
            psm.setTempProject(true);

            // On-disk, DB-backed index via the real provider.
            SearchService searchService = new SearchServiceImpl(new NoSqlProjectSearchContextProvider(false, indexingHome, true));
            NoSQLProjectImpl project = new NoSQLProjectImpl("test", psm, searchService, (a, b) -> false);

            project.addAlignedFeatures(List.of(feature("alpha", "FID-A")), null,
                    EnumSet.noneOf(AlignedFeature.OptField.class), "src");

            Path projectIndexDir = indexingHome.resolve(project.getSystemUID());
            assertTrue(Files.isDirectory(projectIndexDir), "on-disk index dir should exist while the temp project is open");

            project.close();

            assertFalse(Files.exists(projectIndexDir),
                    "a temp project's on-disk index dir must be deleted on close (B3)");
        } finally {
            FileUtils.deleteRecursively(indexingHome);
            FileUtils.deleteRecursively(location);
        }
    }
}
