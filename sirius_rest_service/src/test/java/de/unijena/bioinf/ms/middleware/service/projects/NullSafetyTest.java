package de.unijena.bioinf.ms.middleware.service.projects;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ms.middleware.service.search.SearchService;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase;
import de.unijena.bioinf.ms.persistence.storage.nitrite.NitriteSirirusProject;
import de.unijena.bioinf.projectspace.NoSQLProjectSpaceManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Phase-1 null-safety: {@link NoSQLProjectImpl} must not crash when the {@link SearchService} is absent
 * (search disabled) or when index initialization fails.
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

    @Test
    public void closeWithoutSearchServiceDoesNotThrow_B2() {
        NoSQLProjectImpl project = projectWithSearchService(null);
        assertDoesNotThrow(() -> project.close(),
                "close() must not NPE when search is disabled and must still close the project-space manager (B2)");
    }
}
