package io.sirius.ms.sdk.api;

import io.sirius.ms.sdk.model.AlignedFeature;
import io.sirius.ms.sdk.model.ProjectInfo;
import org.junit.FixMethodOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.runners.MethodSorters;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import static io.sirius.ms.sdk.model.ProjectInfoOptField.SIZEINFORMATION;
import static org.junit.jupiter.api.Assertions.*;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ProjectsApiTest {

    private ProjectsApi instance;
    private FeaturesApi featureApiInstance;
    private ProjectInfo project;

    @BeforeEach
    public void setUp() {
        TestSetup.getInstance().loginIfNeeded();
        instance = TestSetup.getInstance().getSiriusClient().projects();
        featureApiInstance = TestSetup.getInstance().getSiriusClient().features();
        try {
            project = TestSetup.getInstance().createTestProject(null, null);
        } catch (IOException e) {
            fail("Failed to create test project: " + e.getMessage());
        }
    }

    @AfterEach
    public void tearDown() {
        if (project != null) {
            TestSetup.getInstance().deleteTestProject(project);
        }
    }

    @Test
    public void instanceTest() {
        assertNotNull(instance);
    }

    @Test
    public void closeProjectTest() {
        try {
            String projectId = project.getProjectId();
            ProjectInfo pid = instance.getProject(projectId, null);
            assertNotNull(pid);

            instance.closeProject(projectId, false);
            WebClientResponseException ex = assertThrows(WebClientResponseException.class, () -> instance.getProject(projectId, null));
            assertEquals(404, ex.getStatusCode().value());
        } catch (WebClientResponseException e) {
            fail("API exception occurred: " + e.getMessage());
        }
    }

    @Test
    public void createProjectTest() {
        String projectId = UUID.randomUUID().toString();
        String pathToProject = TestSetup.getInstance().getTempDir().resolve(projectId + ".sirius").toString();
        ProjectInfo response = instance.createProject(projectId, pathToProject, null);
        assertNotNull(response);
        assertEquals(projectId, response.getProjectId());

        List<AlignedFeature> cids = featureApiInstance.getAlignedFeatures(projectId, false, null);
        assertEquals(0, cids.size());

        TestSetup.getInstance().deleteTestProject(response);
    }

    @Test
    public void getProjectTest() {
        try {
            String projectId = project.getProjectId();
            ProjectInfo pid = instance.getProject(projectId, null);
            assertNotNull(pid);
            assertEquals(projectId, pid.getProjectId());
        } catch (Exception e) {
            fail("API exception occurred: " + e.getMessage());
        }
    }

    @Test
    public void getProjectsTest() {
        try {
            List<ProjectInfo> response = instance.getProjects();
            assertNotNull(response);
            assertFalse(response.isEmpty());
        } catch (Exception e) {
            fail("API exception occurred: " + e.getMessage());
        }
    }

    @Test
    public void openProjectTest() {
        try {
            String projectId = "open";
            String pathToProject = TestSetup.getInstance().getProjectSourceToOpen().toAbsolutePath().toString();
            ProjectInfo response = instance.openProject(projectId, pathToProject, null);
            assertNotNull(response);
            assertEquals(projectId, response.getProjectId());

            ProjectInfo projectInfo = instance.getProject(projectId, null);
            assertNotNull(projectInfo);
            assertEquals(projectId, projectInfo.getProjectId());

            instance.closeProject(projectId, false);
        } catch (Exception e) {
            fail("API exception occurred: " + e.getMessage());
        }
    }

    @Test
    public void getFingerIdDataTest() {
        String projectId = "fingerid-data";
        try {
            Path location = TestSetup.getInstance().getProjectSourceToOpen();
            instance.openProject(projectId, location.toAbsolutePath().toString(), null);
            String response = instance.getFingerIdData(projectId, 1);
            assertNotNull(response);
            // Expected behavior: Empty string if no fingerprints in PS, non-empty string otherwise.
        } catch (Exception e) {
            fail("API exception occurred: " + e.getMessage());
        }finally {
           instance.closeProject(projectId, false);
        }
    }


    private static Stream<Arguments> importPreprocessedDataTest() {
        return Stream.of(
                Arguments.of("ForTox_TestMix_AMSMS.cef", 15), // null strings should be considered blank
                Arguments.of("221021_After_Gap_Filling.cef", 30),
                Arguments.of("221021_Before_Gap_Filling.cef", 30),
                Arguments.of("Kaempferol.ms", 1),
                Arguments.of("laudanosine.mgf", 1)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void importPreprocessedDataTest(String file, int expected) {
        Path f = TestSetup.getInstance().getDataDir().resolve(file);
        List<File> inputFiles = List.of(f.toFile());
        instance.importPreprocessedData(project.getProjectId(), true, null, inputFiles);

        List<AlignedFeature> alignedFeatures = featureApiInstance.getAlignedFeatures(project.getProjectId(), false, null);
        assertNotNull(alignedFeatures);
        assertEquals(expected, alignedFeatures.size());

    }

    @Test
    public void compactProjectTest() {
        String projectId = project.getProjectId();

        File f = TestSetup.getInstance().getDataDir().resolve("Kaempferol.ms").toFile();
        List<File> files = Collections.nCopies(20, f);
        instance.importPreprocessedData(projectId, true, null, files);

        // delete all features
        List<String> allFeatureIds = featureApiInstance.getAlignedFeatures(projectId, false, null).stream().map(AlignedFeature::getAlignedFeatureId).toList();
        featureApiInstance.deleteAlignedFeatures(projectId, allFeatureIds);

        instance.closeProject(projectId, false);
        ProjectInfo beforeCompacting = instance.openProject(projectId, project.getLocation(), List.of(SIZEINFORMATION));
        instance.closeProject(projectId, true);
        ProjectInfo afterCompacting = instance.openProject(projectId, project.getLocation(), List.of(SIZEINFORMATION));

        assertTrue(Objects.requireNonNull(afterCompacting.getNumOfBytes()) < Objects.requireNonNull(beforeCompacting.getNumOfBytes()));
    }

}
