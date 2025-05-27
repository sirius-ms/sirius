package io.sirius.ms.sdk.api;

import io.sirius.ms.sdk.model.*;
import org.junit.FixMethodOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runners.MethodSorters;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CompoundsApiTest {

    private CompoundsApi instance;
    private io.sirius.ms.sdk.model.ProjectInfo project;
    private String compoundIdDelete;
    private String compoundIdGet;

    @BeforeEach
    public void setUp() throws IOException {
        TestSetup.getInstance().loginIfNeeded();
        instance = TestSetup.getInstance().getSiriusClient().compounds();
        compoundIdDelete = "595984619130949856";
        compoundIdGet = "595984619000926424";
        project = TestSetup.getInstance().createTestProject("compoundsProject", TestSetup.getInstance().getProjectSourceCompounds());
    }

    @AfterEach
    public void tearDown() {
        TestSetup.getInstance().deleteTestProject(project);
    }

    @Test
    public void instanceTest() {
        assertNotNull(instance);
    }

    @Test
    public void deleteCompoundTest() {
        String projectId = project.getProjectId();
        List<Compound> compounds = instance.getCompounds(projectId, false, null, null);
        String compoundId = compoundIdDelete;
        instance.deleteCompound(projectId, compoundId);
        assertThrows(WebClientResponseException.class, () -> instance.getCompound(projectId, compoundId, false, null, null));
    }

    @Test
    public void getCompoundTest() {
        String projectId = project.getProjectId();
        String compoundId = compoundIdGet;
        Compound response = instance.getCompound(projectId, compoundId, false, null, null);
        assertEquals(compoundId, response.getCompoundId());
    }

    @Test
    public void getCompoundsTest() {
        String projectId = project.getProjectId();
        List<CompoundOptField> compoundOptFields = List.of(
                CompoundOptField.CONSENSUSANNOTATIONS,
                CompoundOptField.CUSTOMANNOTATIONS,
                CompoundOptField.CONSENSUSANNOTATIONSDENOVO
        );
        List<Compound> response = instance.getCompounds(projectId, false, compoundOptFields, null);
        assertNotNull(response);
        assertTrue(response.size() >= 12);
    }

    @Test
    public void addCompoundsTest() {
        String newCompoundName = "testCompound1";
        double ionMass = 194.11757;

        FeatureImport feature = new FeatureImport().name("testFeature1").externalFeatureId("myFeatureId1").ionMass(ionMass).charge(1);

        CompoundImport compoundImport = new CompoundImport()
                .name(newCompoundName).features(List.of(feature));

        List<Compound> newCompounds = instance.addCompounds(project.getProjectId(), List.of(compoundImport),
                null, null, List.of(AlignedFeatureOptField.MSDATA));

        assertNotNull(newCompounds);
        Compound newCompound = newCompounds.getFirst();
        assertEquals(newCompoundName, newCompound.getName());
        assertEquals(ionMass, newCompound.getFeatures().getFirst().getIonMass());

        instance.deleteCompound(project.getProjectId(), newCompound.getCompoundId());
    }

    @Test
    public void addCompoundAndComputeWorkflowTest() throws InterruptedException {
        JobsApi jobsApi = TestSetup.getInstance().getSiriusClient().jobs();
        FeatureImport protonatedFeature = TestSetup.makeProtonatedValium();
        FeatureImport sodiatedFeature = TestSetup.makeSodiatedValium();

        CompoundImport compoundImport = new CompoundImport().name("Valium")
                .features(List.of(protonatedFeature, sodiatedFeature));

        // Add the compound to the project
        List<Compound> newCompounds = instance.addCompounds(
                project.getProjectId(),
                List.of(compoundImport),
                null,
                null,
                List.of(AlignedFeatureOptField.MSDATA)
        );

        Compound newCompound = newCompounds.getFirst();
        assertNotNull(newCompound);
        assertEquals(284.0715, newCompound.getNeutralMass(), 0.001);

        // Get Default Job Submission
        JobSubmission submission = jobsApi.getDefaultJobConfig(true, false, false);
        submission.setZodiacParams(new Zodiac().enabled(false));
        submission.setRecompute(true);
        submission.setCompoundIds(List.of(newCompound.getCompoundId()));

        Job job = jobsApi.startJob(project.getProjectId(), submission, null);

        // Wait until computations are finished
        TestSetup.getInstance().getSiriusClient().awaitJob(project.getProjectId(), job.getId());

        // Get the consensus result for the whole compound
        Compound compound = instance.getCompound(
                project.getProjectId(),
                newCompound.getCompoundId(),
                false,
                List.of(CompoundOptField.CONSENSUSANNOTATIONS),
                List.of(AlignedFeatureOptField.TOPANNOTATIONS)
        );

        assertTrue(Set.of("diazepam", "valium").contains(compound.getConsensusAnnotations().getCsiFingerIdStructure().getStructureName().toLowerCase()));

        // Check if features were annotated as expected
        assertEquals(
                compound.getConsensusAnnotations().getCsiFingerIdStructure().getInchiKey(),
                compound.getFeatures().stream()
                        .max(Comparator.comparingDouble(f -> f.getTopAnnotations().getConfidenceApproxMatch()))
                        .get().getTopAnnotations().getStructureAnnotation().getInchiKey()
        );
    }
}
