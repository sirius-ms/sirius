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
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

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
    public void closeProjectSpaceTest() {
        try {
            String projectId = project.getProjectId();
            ProjectInfo pid = instance.getProjectSpace(projectId, null);
            assertNotNull(pid);

            instance.closeProjectSpace(projectId);
            WebClientResponseException ex = assertThrows(WebClientResponseException.class, () -> instance.getProjectSpace(projectId, null));
            assertEquals(404, ex.getStatusCode().value());
        } catch (WebClientResponseException e) {
            fail("API exception occurred: " + e.getMessage());
        }
    }

    @Test
    public void createProjectSpaceTest() {
        String projectId = UUID.randomUUID().toString();
        String pathToProject = TestSetup.getInstance().getTempDir().resolve(projectId + ".sirius").toString();
        ProjectInfo response = instance.createProjectSpace(projectId, pathToProject, null);
        assertNotNull(response);
        assertEquals(projectId, response.getProjectId());

        List<AlignedFeature> cids = featureApiInstance.getAlignedFeatures(projectId, null);
        assertEquals(0, cids.size());

        TestSetup.getInstance().deleteTestProject(response);
    }

    @Test
    public void getProjectSpaceTest() {
        try {
            String projectId = project.getProjectId();
            ProjectInfo pid = instance.getProjectSpace(projectId, null);
            assertNotNull(pid);
            assertEquals(projectId, pid.getProjectId());
        } catch (Exception e) {
            fail("API exception occurred: " + e.getMessage());
        }
    }

    @Test
    public void getProjectSpacesTest() {
        try {
            List<ProjectInfo> response = instance.getProjectSpaces();
            assertNotNull(response);
            assertFalse(response.isEmpty());
        } catch (Exception e) {
            fail("API exception occurred: " + e.getMessage());
        }
    }

    @Test
    public void openProjectSpaceTest() {
        try {
            String projectId = "open";
            String pathToProject = TestSetup.getInstance().getProjectSourceToOpen().toAbsolutePath().toString();
            ProjectInfo response = instance.openProjectSpace(projectId, pathToProject, null);
            assertNotNull(response);
            assertEquals(projectId, response.getProjectId());

            ProjectInfo projectInfo = instance.getProjectSpace(projectId, null);
            assertNotNull(projectInfo);
            assertEquals(projectId, projectInfo.getProjectId());

            instance.closeProjectSpace(projectId);
        } catch (Exception e) {
            fail("API exception occurred: " + e.getMessage());
        }
    }

    @Test
    public void getFingerIdDataTest() {
        String projectId = "fingerid-data";
        try {
            Path location = TestSetup.getInstance().getProjectSourceToOpen();
            instance.openProjectSpace(projectId, location.toAbsolutePath().toString(), null);
            String response = instance.getFingerIdData(projectId, 1);
            assertNotNull(response);
            // Expected behavior: Empty string if no fingerprints in PS, non-empty string otherwise.
        } catch (Exception e) {
            fail("API exception occurred: " + e.getMessage());
        }finally {
           instance.closeProjectSpace(projectId);
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

        List<AlignedFeature> alignedFeatures = featureApiInstance.getAlignedFeatures(project.getProjectId(), null);
        assertNotNull(alignedFeatures);
        assertEquals(expected, alignedFeatures.size());

    }
}
