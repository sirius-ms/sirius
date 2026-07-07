package de.unijena.bioinf.ms.middleware.service.projects;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ms.middleware.model.features.AlignedFeature;
import de.unijena.bioinf.ms.middleware.model.features.FeatureImport;
import de.unijena.bioinf.ms.middleware.model.spectra.BasicSpectrum;
import de.unijena.bioinf.ms.middleware.service.search.SearchService;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase;
import de.unijena.bioinf.ms.persistence.storage.nitrite.NitriteSirirusProject;
import de.unijena.bioinf.projectspace.NoSQLProjectSpaceManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Phase-1 null-safety: {@link NoSQLProjectImpl} must not crash when the {@link SearchService} is absent
 * (search disabled) or when index initialization fails.
 * <ul>
 *   <li>B2 — {@code close(boolean)} must not NPE and must still close the project-space manager.</li>
 *   <li>H1 — write/delete paths must guard {@code searchService} instead of dereferencing it blindly.</li>
 *   <li>H12 — a failure while initializing the index must not abort project construction.</li>
 * </ul>
 */
public class NullSafetyTest {

    private NitriteSirirusProject ps;

    @BeforeEach
    public void createProjectSpace() throws IOException {
        Path location = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        ps = new NitriteSirirusProject(location);
    }

    @AfterEach
    public void closeProjectSpace() {
        try {
            ps.close();
        } catch (Exception ignored) {
            // may already be closed by a test (e.g. B2); ignore.
        }
    }

    private NoSQLProjectImpl projectWithSearchService(SearchService searchService) {
        return new NoSQLProjectImpl("test", new NoSQLProjectSpaceManager(ps), searchService, (a, b) -> false);
    }

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
    public void closeWithoutSearchServiceDoesNotThrow() {
        NoSQLProjectImpl project = projectWithSearchService(null);
        assertDoesNotThrow(() -> project.close(),
                "close() must not NPE when search is disabled and must still close the project-space manager (B2)");
    }

    @Test
    public void addAlignedFeaturesWithoutSearchServiceDoesNotThrow() {
        NoSQLProjectImpl project = projectWithSearchService(null);
        assertDoesNotThrow(() -> project.addAlignedFeatures(
                        List.of(feature("foo", "FID-A")), null,
                        EnumSet.noneOf(AlignedFeature.OptField.class), "src"),
                "addAlignedFeatures must not NPE when search is disabled (H1)");
    }

    @Test
    public void removeTagsFromObjectWithoutSearchServiceDoesNotThrow() {
        NoSQLProjectImpl project = projectWithSearchService(null);
        assertDoesNotThrow(() -> project.removeTagsFromObject(AlignedFeature.class, "1", List.of("someTag")),
                "removeTagsFromObject must not NPE when search is disabled (H1)");
    }

    @Test
    public void constructionSurvivesSearchIndexInitFailure() throws IOException {
        SearchService throwing = Mockito.mock(SearchService.class);
        Mockito.doThrow(new RuntimeException("simulated index init failure"))
                .when(throwing).openOrCreateProjectIndex(Mockito.any());

        assertDoesNotThrow(() -> projectWithSearchService(throwing),
                "a RuntimeException during index initialization must not abort project construction (H12)");
    }
}
