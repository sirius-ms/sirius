package de.unijena.bioinf.ms.middleware.service.projects;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ms.middleware.model.features.QuantTable;
import de.unijena.bioinf.ms.middleware.service.search.SearchService;
import de.unijena.bioinf.ms.persistence.model.core.statistics.QuantMeasure;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase;
import de.unijena.bioinf.ms.persistence.storage.nitrite.NitriteSirirusProject;
import de.unijena.bioinf.projectspace.NoSQLProjectSpaceManager;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.EnumSet;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

/**
 * The quantification tables select their rows by search query. Features are resolved via the search index, and so is
 * the compound a feature belongs to. Compounds themselves are not indexed yet, so a compound query may only refer to
 * compound ids.
 */
class QuantificationQueryTest {

    @AutoClose
    private NitriteSirirusProject ps;

    private NoSQLProjectImpl projectWithoutIndex;

    @BeforeEach
    void createTestProject() throws IOException {
        Path location = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        ps = new NitriteSirirusProject(location);
        projectWithoutIndex = project(null);
    }

    private NoSQLProjectImpl project(SearchService searchService) {
        return new NoSQLProjectImpl("test", new NoSQLProjectSpaceManager(ps), searchService, (a, b) -> false);
    }

    @Test
    @DisplayName("quantifying everything needs the search index as well")
    void featureQuantificationWithoutQueryStillNeedsTheIndex() {
        //without a query the rows are all aligned features, and which features those are is what the index knows:
        //a feature of an isotopic alignment is stored like any other and would otherwise become a row of its own
        ResponseStatusException e = assertThrows(ResponseStatusException.class, () ->
                projectWithoutIndex.getFeatureQuantification(null, QuantMeasure.APEX_INTENSITY, EnumSet.noneOf(QuantTable.OptField.class)));

        assertEquals(SERVICE_UNAVAILABLE, e.getStatusCode());
    }

    @Test
    @DisplayName("a feature query without search service is reported as unavailable")
    void featureQueryRequiresSearchService() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class, () ->
                projectWithoutIndex.getFeatureQuantification("alignedFeatureId:1", QuantMeasure.APEX_INTENSITY, EnumSet.noneOf(QuantTable.OptField.class)));

        assertEquals(SERVICE_UNAVAILABLE, e.getStatusCode());
    }

    @Test
    @DisplayName("a compound id query without search service is reported as unavailable")
    void compoundIdQueryRequiresSearchService() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class, () ->
                projectWithoutIndex.getCompoundQuantification("compoundId:1", QuantMeasure.APEX_INTENSITY, EnumSet.noneOf(QuantTable.OptField.class)));

        assertEquals(SERVICE_UNAVAILABLE, e.getStatusCode());
    }

    @Test
    @DisplayName("any other compound query is rejected as not supported")
    void compoundSearchQueryIsRejected() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class, () ->
                projectWithoutIndex.getCompoundQuantification("tags.MyTag:sample", QuantMeasure.APEX_INTENSITY, EnumSet.noneOf(QuantTable.OptField.class)));

        assertEquals(METHOD_NOT_ALLOWED, e.getStatusCode());
        assertTrue(e.getReason() != null && e.getReason().contains("compoundId"),
                "the error must name the supported query form, but was: " + e.getReason());
    }

    @Test
    @DisplayName("compounds are related to their features via the index, so it is needed even without a query")
    void compoundQuantificationRequiresSearchService() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class, () ->
                projectWithoutIndex.getCompoundQuantification("  ", QuantMeasure.APEX_INTENSITY, EnumSet.noneOf(QuantTable.OptField.class)));

        assertEquals(SERVICE_UNAVAILABLE, e.getStatusCode());
    }
}
