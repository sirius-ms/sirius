package de.unijena.bioinf.ms.middleware.service.projects;

import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ms.middleware.model.features.Feature;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.run.LCMSRun;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase;
import de.unijena.bioinf.ms.persistence.storage.nitrite.NitriteSirirusProject;
import de.unijena.bioinf.projectspace.NoSQLProjectSpaceManager;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * The features an aligned feature was aligned from, one per run it was detected in. They carry what is specific to a
 * run: where the feature sits on that run's retention time axis and how much of it was measured there.
 */
class FeaturesOfAlignedFeatureTest {

    @AutoClose
    private NitriteSirirusProject ps;
    private NoSQLProjectImpl project;

    private long alignedFeatureId;
    private long runlessFeatureId;

    @BeforeEach
    void createTestProject() throws IOException {
        Path location = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        ps = new NitriteSirirusProject(location);
        project = new NoSQLProjectImpl("test", new NoSQLProjectSpaceManager(ps), null, (a, b) -> false);

        alignedFeatureId = insertAlignedFeature();
        for (int i = 0; i < 5; i++)
            insertFeature(alignedFeatureId, insertRun("run-" + i), 100d + i);

        //a project imported from preprocessed data has features that belong to no run
        runlessFeatureId = insertAlignedFeature();
        insertFeature(runlessFeatureId, null, 42d);
    }

    @Test
    @DisplayName("the features of an aligned feature are returned page by page")
    void featuresArePaged() {
        Page<Feature> first = project.findFeaturesByAlignedFeatureId(Long.toString(alignedFeatureId), PageRequest.of(0, 2));

        assertEquals(2, first.getContent().size());
        assertEquals(5, first.getTotalElements(), "the total is the number of features of that aligned feature");
        assertTrue(first.getContent().stream().allMatch(f -> Long.toString(alignedFeatureId).equals(f.getAlignedFeatureId())));
    }

    @Test
    @DisplayName("a feature carries where it sits in its run and how much was measured")
    void featuresCarryTheirRunSpecificProperties() {
        Feature feature = project.findFeaturesByAlignedFeatureId(Long.toString(alignedFeatureId), Pageable.unpaged())
                .getContent().getFirst();

        assertNotNull(feature.getFeatureId());
        assertNotNull(feature.getRunId());
        assertNotNull(feature.getApexIntensity());
        assertNotNull(feature.getAreaUnderCurve());
        assertNotNull(feature.getApexMz(), "the apex m/z is what a caller needs to relate a feature to a mass");
        assertEquals(60d, feature.getRtStartSeconds());
        assertEquals(80d, feature.getRtEndSeconds());
        assertEquals(70d, feature.getRtApexSeconds());
    }

    @Test
    @DisplayName("a feature that belongs to no run has no run, it is not an error")
    void featureWithoutRunIsReturned() {
        Page<Feature> features = project.findFeaturesByAlignedFeatureId(Long.toString(runlessFeatureId), Pageable.unpaged());

        assertEquals(1, features.getContent().size());
        assertNull(features.getContent().getFirst().getRunId(), "there is no run to refer to");
    }

    @Test
    @DisplayName("an aligned feature that does not exist has no features")
    void unknownAlignedFeatureHasNoFeatures() {
        Page<Feature> features = project.findFeaturesByAlignedFeatureId("123456789", Pageable.unpaged());

        assertTrue(features.getContent().isEmpty());
        assertEquals(0, features.getTotalElements());
    }

    @Test
    @DisplayName("an id that is no id is a bad request, not an internal error")
    void malformedIdIsRejected() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> project.findFeaturesByAlignedFeatureId("not-an-id", Pageable.unpaged()));

        assertEquals(BAD_REQUEST, e.getStatusCode());
    }

    @SneakyThrows
    private long insertRun(String name) {
        LCMSRun run = LCMSRun.builder().name(name).build();
        ps.getStorage().insert(run);
        return run.getRunId();
    }

    @SneakyThrows
    private long insertAlignedFeature() {
        AlignedFeatures feature = AlignedFeatures.builder().name("aligned").build();
        ps.getStorage().insert(feature);
        return feature.getAlignedFeatureId();
    }

    @SneakyThrows
    private void insertFeature(long alignedFeatureId, Long runId, double apexIntensity) {
        ps.getStorage().insert(de.unijena.bioinf.ms.persistence.model.core.feature.Feature.builder()
                .alignedFeatureId(alignedFeatureId)
                .runId(runId)
                .apexIntensity(apexIntensity)
                .areaUnderCurve(apexIntensity * 10)
                .apexMass(195.0876)
                .averageMass(195.0871)
                .retentionTime(new RetentionTime(60d, 80d, 70d))
                .build());
    }
}
