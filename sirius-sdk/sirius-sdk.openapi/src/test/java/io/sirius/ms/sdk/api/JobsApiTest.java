package io.sirius.ms.sdk.api;

import io.sirius.ms.sdk.model.*;
import org.junit.FixMethodOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.runners.MethodSorters;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class JobsApiTest {

    private JobsApi instance;
    private ProjectInfo project;
    private FeaturesApi featuresApi;
    private TestSetup testSetup = TestSetup.getInstance();

    @BeforeEach
    public void setUp() throws IOException {
        TestSetup.getInstance().loginIfNeeded();
        instance = testSetup.getSiriusClient().jobs();
        featuresApi = testSetup.getSiriusClient().features();
        project = testSetup.createTestProject("jobsTest", testSetup.getProjectSourceCompounds());
    }

    @AfterEach
    public void tearDown() {
        testSetup.deleteTestProject(project);
    }

    @Test
    public void testInstance() {
        assertNotNull(instance);
    }

    @ParameterizedTest
    @CsvSource({"true, true",
            "false, true"
    })
    public void testDeleteJob(boolean cancelIfRunning, boolean awaitDeletion) {
        JobSubmission defJs = instance.getDefaultJobConfig(true);
        defJs.setFormulaIdParams(new Sirius().enabled(false));
        defJs.setZodiacParams(new Zodiac().enabled(false));
        defJs.setFingerprintPredictionParams(new FingerprintPrediction().enabled(false));
        defJs.setStructureDbSearchParams(new StructureDbSearch().enabled(false));
        defJs.setSpectraSearchParams(new SpectralLibrarySearch().enabled(false));
        defJs.setCanopusParams(new Canopus().enabled(true));

        Job job = instance.startJob(project.getProjectId(), defJs, null);
        assertNotNull(job);

        instance.deleteJob(project.getProjectId(), job.getId(), cancelIfRunning, awaitDeletion);

        WebClientResponseException ex = assertThrows(WebClientResponseException.class, () -> {
            if (!awaitDeletion)
                testSetup.getSiriusClient().awaitJob(project.getProjectId(), job.getId());
            instance.getJob(project.getProjectId(), job.getId(), null);
        });
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    public void testDeleteJobConfig() {
        JobSubmission defJs = instance.getDefaultJobConfig(true);
        String name = null;
        try {
            name = instance.saveJobConfig(UUID.randomUUID().toString(), defJs, false);
            assertNotNull(name);
            instance.deleteJobConfig(name);
        } finally {
            if (name != null) instance.deleteJobConfig(name);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testGetDefaultJobConfig(boolean includeConfigMap) {
        JobSubmission defJs = instance.getDefaultJobConfig(includeConfigMap);
        assertNotNull(defJs);
        assertEquals(defJs.getConfigMap() != null && !defJs.getConfigMap().isEmpty(), includeConfigMap);
    }

    @Test
    public void testGetJob() throws InterruptedException {
        JobSubmission defJs = instance.getDefaultJobConfig(true);
        defJs.setFormulaIdParams(new Sirius().enabled(true));
        defJs.setZodiacParams(new Zodiac().enabled(false));
        defJs.setFingerprintPredictionParams(new FingerprintPrediction().enabled(true));
        defJs.setStructureDbSearchParams(new StructureDbSearch().enabled(false));
        defJs.setSpectraSearchParams(new SpectralLibrarySearch().enabled(false));
        defJs.setCanopusParams(new Canopus().enabled(true));

        Job job = instance.startJob(project.getProjectId(), defJs, null);
        assertNotNull(job);

        List<JobOptField> jobOptFields = makeOptFields(true, true, true);
        job = instance.getJob(project.getProjectId(), job.getId(), jobOptFields);
        assertNotNull(job);
        assertNotNull(job.getProgress());
        assertNotNull(job.getCommand());

        testSetup.getSiriusClient().awaitJob(project.getProjectId(), job.getId());
        assertNotNull(job.getAffectedCompoundIds());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testGetJobConfig(boolean includeConfigMap) {
        String name = null;
        try {
            JobSubmission defJs = instance.getDefaultJobConfig(true);
            defJs.setFormulaIdParams(new Sirius().enabled(false));
            defJs.setZodiacParams(new Zodiac().enabled(false));
            defJs.setFingerprintPredictionParams(new FingerprintPrediction().enabled(false));
            defJs.setStructureDbSearchParams(new StructureDbSearch().enabled(false));

            name = instance.saveJobConfig(UUID.randomUUID().toString(), defJs, false);
            assertNotNull(name);

            JobSubmission jc = instance.getJobConfig(name, includeConfigMap);
            assertNotNull(jc);
            assertEquals(jc.getConfigMap() != null && !jc.getConfigMap().isEmpty(), includeConfigMap);

            assertTrue(jc.getCanopusParams().isEnabled());
            assertFalse(jc.getFormulaIdParams().isEnabled());
            assertFalse(jc.getZodiacParams().isEnabled());
            assertFalse(jc.getFingerprintPredictionParams().isEnabled());
            assertFalse(jc.getStructureDbSearchParams().isEnabled());
        } finally {
            if (name != null) instance.deleteJobConfig(name);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testGetJobConfigs(boolean includeConfigMap) {
        String name = null;
        try {
            JobSubmission defJs = instance.getDefaultJobConfig(true);
            name = instance.saveJobConfig(UUID.randomUUID().toString(), defJs, false);
            assertNotNull(name);

            List<JobSubmission> response = instance.getJobConfigs(includeConfigMap);
            assertNotNull(response);
            assertFalse(response.isEmpty());
        } finally {
            if (name != null) instance.deleteJobConfig(name);
        }
    }

    @ParameterizedTest
    @CsvSource({
            "false, false, false, false",
            "true, true, true, true"
    })
    public void testStartJob(boolean includeProgress, boolean includeCommand, boolean includeAffectedCompounds, boolean configMap) throws InterruptedException {
        JobSubmission submission = instance.getDefaultJobConfig(configMap);
        submission.setZodiacParams(new Zodiac().enabled(false));
        submission.setRecompute(true);

        List<JobOptField> opts = makeOptFields(includeProgress, includeCommand, includeAffectedCompounds);

        Job job = instance.startJob(project.getProjectId(), submission, opts);
        assertNotNull(job);

        assertEquals(job.getProgress() != null, includeProgress);
        assertEquals(job.getCommand() != null, includeCommand);

        job = testSetup.getSiriusClient().awaitJob(project.getProjectId(), job.getId());
        assertEquals(JobProgress.StateEnum.DONE, job.getProgress().getState());
    }

    @ParameterizedTest
    @CsvSource({
            "false, false, false, true",
            "true, true, true, false"
    })
    public void testStartJobFromConfig(boolean includeProgress, boolean includeCommand, boolean includeAffectedCompounds, boolean configMap) throws InterruptedException {
        // Creating job submission with parameters
        JobSubmission submission = instance.getDefaultJobConfig(configMap);
        submission.getFormulaIdParams().setEnabled(true);
        submission.getZodiacParams().setEnabled(false);
        submission.getStructureDbSearchParams().setEnabled(false);
        submission.getFingerprintPredictionParams().setEnabled(false);
        submission.getCanopusParams().setEnabled(false);
        submission.setRecompute(false); // store false but start job with true

        // Saving job configuration
        String configName = instance.saveJobConfig(UUID.randomUUID().toString(), submission, true);

        // Check if storing worked
        JobSubmission config = instance.getJobConfig(configName, null);
        assertNotNull(config);

        // Retrieving aligned features for the project
        List<String> features = featuresApi.getAlignedFeatures(project.getProjectId(), null)
                .stream()
                .map(AlignedFeature::getAlignedFeatureId)
                .toList();

        // Start job from config
        List<JobOptField> optFields = makeOptFields(includeProgress, includeCommand, includeAffectedCompounds);
        Job job = instance.startJobFromConfig(project.getProjectId(), configName, features, true, optFields);
        assertNotNull(job);

        // Check if progress and command fields are included as expected
        assertEquals(includeProgress, job.getProgress() != null);
        assertEquals(includeCommand, job.getCommand() != null);

        // Wait for the job to complete
        job = testSetup.getSiriusClient().awaitJob(project.getProjectId(), job.getId());
        assertEquals(JobProgress.StateEnum.DONE, job.getProgress().getState());
    }

    // Helper function to generate optional fields list
    private List<JobOptField> makeOptFields(boolean includeProgress, boolean includeCommand, boolean includeAffectedCompounds) {
        List<JobOptField> optFields = new ArrayList<>();
        if (includeProgress) optFields.add(JobOptField.PROGRESS);
        if (includeCommand) optFields.add(JobOptField.COMMAND);
        if (includeAffectedCompounds) optFields.add(JobOptField.AFFECTEDIDS);
        if (optFields.isEmpty())
            optFields.add(JobOptField.NONE);
        return optFields;
    }
}
