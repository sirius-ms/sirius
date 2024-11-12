/*
 * SIRIUS Nightsky API
 * REST API that provides the full functionality of SIRIUS and its web services as background service. It is intended as entry-point for scripting languages and software integration SDKs.This API is exposed by SIRIUS 6
 *
 * The version of the OpenAPI document: 2.1
 *
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


package io.sirius.ms.sdk.api;

import io.sirius.ms.sdk.model.*;
import org.junit.FixMethodOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runners.MethodSorters;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * API tests for FeaturesApi
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FeaturesApiTest {

    private FeaturesApi instance;
    private ProjectInfo project;
    private String featureIdGet;
    private String featureIdDelete;

    @BeforeEach
    public void setUp() throws IOException {
        TestSetup.getInstance().loginIfNeeded();
        // Assume TestSetup is a utility class that initializes the API client and other test setup.
        instance = TestSetup.getInstance().getSiriusClient().features();
        project = TestSetup.getInstance().createTestProject("featureTest", TestSetup.getInstance().getProjectSourceResults());
        featureIdDelete = "595969845215149616";
        featureIdGet = "595969845215149616";
    }

    @AfterEach
    public void tearDown() {
        TestSetup.getInstance().deleteTestProject(project);
    }

    @Test
    public void testInstance() {
        assertNotNull(instance);
    }

    @Test
    public void testDeleteAlignedFeature() {
        String projectId = project.getProjectId();
        String alignedFeatureId = featureIdDelete;

        // Deleting the feature
        instance.deleteAlignedFeature(projectId, alignedFeatureId);

        // Verifying that fetching the deleted feature throws an exception
        WebClientResponseException exception = assertThrows(WebClientResponseException.class, () ->
                instance.getAlignedFeature(projectId, alignedFeatureId, null));

        // Asserting that the exception is a 404 (not found)
        Assertions.assertEquals(404, exception.getStatusCode().value());
    }

    @Test
    public void testGetAlignedFeatures() {
        String projectId = project.getProjectId();
        List<AlignedFeatureOptField> featureOptFields = List.of(
                AlignedFeatureOptField.TOPANNOTATIONS,
                AlignedFeatureOptField.TOPANNOTATIONSDENOVO,
                AlignedFeatureOptField.MSDATA
        );

        List<AlignedFeature> response = instance.getAlignedFeatures(projectId, featureOptFields);

        assertNotNull(response);
        assertTrue(!response.isEmpty() && response.size() <= 2);
    }

    @Test
    public void testGetAlignedFeature() {
        String projectId = project.getProjectId();
        List<AlignedFeature> features = instance.getAlignedFeatures(projectId, null);
        String alignedFeatureId = features.getFirst().getAlignedFeatureId();

        AlignedFeature response = instance.getAlignedFeature(projectId, alignedFeatureId, null);

        assertNotNull(response);
        Assertions.assertEquals("595969845215149616", response.getAlignedFeatureId());
    }

    @Test
    public void testGetFormulaCandidate() {
        String projectId = project.getProjectId();
        List<AlignedFeature> features = instance.getAlignedFeatures(projectId, null);
        String alignedFeatureId = features.getFirst().getAlignedFeatureId();
        String formulaId = "595969889171455582";

        FormulaCandidate response = instance.getFormulaCandidate(projectId, alignedFeatureId, formulaId, null);

        assertNotNull(response);
        Assertions.assertEquals(formulaId, response.getFormulaId());
    }

    @Test
    public void testGetFormulaCandidates() {
        String projectId = project.getProjectId();
        String alignedFeatureId = featureIdGet;

        List<FormulaCandidate> response = instance.getFormulaCandidates(projectId, alignedFeatureId, null);

        assertNotNull(response);
        assertEquals(1, response.size());
    }

    @Test
    public void testGetStructureCandidates() {
        String projectId = project.getProjectId();
        String alignedFeatureId = featureIdGet;
        List<StructureCandidateOptField> structureCandidateOptField = List.of(
                StructureCandidateOptField.FINGERPRINT,
                StructureCandidateOptField.DBLINKS,
                StructureCandidateOptField.LIBRARYMATCHES
        );

        List<StructureCandidateFormula> response = instance.getStructureCandidates(projectId, alignedFeatureId, structureCandidateOptField);

        assertNotNull(response);
        assertEquals(61, response.size());
    }

    @Test
    public void testGetStructureCandidatesByFormula() {
        String projectId = project.getProjectId();
        String alignedFeatureId = featureIdGet;
        String formulaId = "595969889171455582";

        List<StructureCandidateScored> response = instance.getStructureCandidatesByFormula(projectId, alignedFeatureId, formulaId, null);

        assertNotNull(response);
        assertEquals(61, response.size());
    }

}