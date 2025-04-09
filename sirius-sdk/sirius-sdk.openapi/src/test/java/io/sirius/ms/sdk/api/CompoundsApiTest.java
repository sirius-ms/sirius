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
        String newCompoundName = "testCompound1";

        // Create the two features that make up the compound
        double ionMassProtonated = 285.0787;
        // Creating protonatedMs1 using the builder pattern
        BasicSpectrum protonatedMs1 = new BasicSpectrum()
                .addPeaksItem(new SimplePeak().mz(285.0789).intensity(210252.13))
                .addPeaksItem(new SimplePeak().mz(286.0822).intensity(36264.31))
                .addPeaksItem(new SimplePeak().mz(287.0766).intensity(70364.01))
                .addPeaksItem(new SimplePeak().mz(288.0791).intensity(12274.46))
                .addPeaksItem(new SimplePeak().mz(289.0840).intensity(1037.72));

// Creating protonatedMs2 using the builder pattern with precursorMz
        BasicSpectrum protonatedMs2 = new BasicSpectrum()
                .precursorMz(285.07872)
                .addPeaksItem(new SimplePeak().mz(91.0545).intensity(317.62))
                .addPeaksItem(new SimplePeak().mz(105.0333).intensity(503.78))
                .addPeaksItem(new SimplePeak().mz(154.0415).intensity(3030.97))
                .addPeaksItem(new SimplePeak().mz(167.0116).intensity(240.42))
                .addPeaksItem(new SimplePeak().mz(172.0628).intensity(297.89))
                .addPeaksItem(new SimplePeak().mz(179.0369).intensity(207.02))
                .addPeaksItem(new SimplePeak().mz(180.0199).intensity(349.96))
                .addPeaksItem(new SimplePeak().mz(182.0367).intensity(780.00))
                .addPeaksItem(new SimplePeak().mz(193.0883).intensity(1824.38))
                .addPeaksItem(new SimplePeak().mz(221.1065).intensity(307.91))
                .addPeaksItem(new SimplePeak().mz(222.1147).intensity(2002.34))
                .addPeaksItem(new SimplePeak().mz(228.0573).intensity(1800.88))
                .addPeaksItem(new SimplePeak().mz(241.0527).intensity(301.77))
                .addPeaksItem(new SimplePeak().mz(255.0662).intensity(207.54))
                .addPeaksItem(new SimplePeak().mz(257.0839).intensity(3000.70))
                .addPeaksItem(new SimplePeak().mz(285.0787).intensity(18479.91))
                .addPeaksItem(new SimplePeak().mz(285.2895).intensity(268.90));

        FeatureImport protonatedFeature = new FeatureImport().name("protonated").externalFeatureId("protonatedFeature")
                .ionMass(ionMassProtonated).charge(1).addDetectedAdductsItem("[M+H]+")
                .mergedMs1(protonatedMs1)
                .addMs2SpectraItem(protonatedMs2);



        double ionMassSodiated = 307.0611;
        BasicSpectrum sodiatedMs1 = new BasicSpectrum()
                .addPeaksItem(new SimplePeak().mz(307.0608).intensity(16236.22))
                .addPeaksItem(new SimplePeak().mz(308.0647).intensity(2805.32))
                .addPeaksItem(new SimplePeak().mz(309.0581).intensity(5348.16))
                .addPeaksItem(new SimplePeak().mz(310.0600).intensity(702.98));

        BasicSpectrum sodiatedMs2 = new BasicSpectrum()
                .precursorMz(307.06080)
                .addPeaksItem(new SimplePeak().mz(57.0677).intensity(14.66))
                .addPeaksItem(new SimplePeak().mz(57.1278).intensity(11.91))
                .addPeaksItem(new SimplePeak().mz(65.8891).intensity(11.46))
                .addPeaksItem(new SimplePeak().mz(149.0238).intensity(25.78))
                .addPeaksItem(new SimplePeak().mz(163.0406).intensity(12.37))
                .addPeaksItem(new SimplePeak().mz(165.0898).intensity(10.06))
                .addPeaksItem(new SimplePeak().mz(187.0713).intensity(17.24))
                .addPeaksItem(new SimplePeak().mz(247.1309).intensity(17.40));

        FeatureImport sodiatedFeature = new FeatureImport().name("sodium").externalFeatureId("sodiatedFeature")
                .ionMass(ionMassSodiated).charge(1).addDetectedAdductsItem("[M+Na]+")
                .mergedMs1(sodiatedMs1)
                .addMs2SpectraItem(protonatedMs2);

        CompoundImport compoundImport = new CompoundImport().name(newCompoundName)
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
