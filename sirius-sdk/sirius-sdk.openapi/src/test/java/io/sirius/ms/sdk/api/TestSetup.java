package io.sirius.ms.sdk.api;

import io.sirius.ms.sdk.model.*;
import io.sirius.ms.sdk.SiriusClient;
import io.sirius.ms.sdk.SiriusSDK;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Getter
@Slf4j
public class TestSetup {

    private static TestSetup INSTANCE ;

    private final Path projectSourceToOpen;
    private final Path projectSourceCompounds;
    private final Path projectSourceResults;
    private final Path searchableCustomDB;
    private final Path dbImportStructures;
    private final Path dbImportSpectrum;
    private final Path tempDir;
    private final Path dataDir;
    private final String SIRIUS_USER_ENV;
    private final String SIRIUS_PW_ENV;
    private final String SIRIUS_ACTIVE_SUB;
    private final SiriusClient siriusClient;

    @SneakyThrows
    private TestSetup() {
        projectSourceToOpen = getResourceFilePath("/data/test-project-to-open/test-project-open.sirius");
        projectSourceCompounds = getResourceFilePath("/data/test-project_compounds/test-project-compounds.sirius");
        projectSourceResults = getResourceFilePath("/data/test-project-results/test-project-results.sirius");
        searchableCustomDB = getResourceFilePath("/data/test-searchable-database/customDbID.siriusdb");
        dbImportStructures = getResourceFilePath("/data/custom-db-sirius-demo-structures.tsv");
        dbImportSpectrum = getResourceFilePath("/data/SM801101.txt");
        dataDir = getResourceFilePath("/data/");
        tempDir = Path.of(System.getProperty("java.io.tmpdir")).resolve("nightsky-test-" + UUID.randomUUID());

        SIRIUS_USER_ENV = System.getenv("SIRIUS_USER");
        SIRIUS_PW_ENV = System.getenv("SIRIUS_PW");
        SIRIUS_ACTIVE_SUB = System.getenv("SIRIUS_SUB");

        if (!Files.exists(tempDir)) {
            try {
                Files.createDirectories(tempDir);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create temp directory", e);
            }
        }

        // use content dir of current module to navigate relatively to the bootjar location. Since the working directory is not always the same.
        Path bootJar;
        try (Stream<Path> walker = Files.walk(dataDir.getParent().getParent().getParent().getParent().getParent().getParent()
                .resolve("sirius_rest_service/build/libs"))){
            bootJar = walker.filter(p -> p.getFileName().toString().matches("sirius_rest_service-.*-boot.jar")).max(Comparator.comparing(p -> {
                        try {
                          return Files.getLastModifiedTime(p);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }))
                    .orElseThrow(() -> new IOException("Could not finger boot jar for testing."));
        }

        siriusClient = SiriusSDK.startAndConnectLocally(SiriusSDK.ShutdownMode.AUTO, true, bootJar);
    }

    public synchronized void destroy(){
        siriusClient.close();
    }

    private synchronized Path getResourceFilePath(String resourcePath) {
        try {
            return Paths.get(getClass().getResource(resourcePath).toURI());
        } catch (Exception e) {
            throw new RuntimeException("Failed to load resource: " + resourcePath, e);
        }
    }

    public synchronized static TestSetup getInstance() {
        if (INSTANCE == null)
            INSTANCE = new TestSetup();
        return INSTANCE;
    }

    public synchronized ProjectInfo createTestProject(String projectSuffix, Path sourceProject) throws IOException {
        return createTestProject(projectSuffix, sourceProject, null);
    }

    public synchronized ProjectInfo createTestProject(String projectSuffix, Path sourceProject, List<ProjectInfoOptField> optFields) throws IOException {
        String uid = UUID.randomUUID().toString();
        String name = "test-project-" + uid + (projectSuffix != null ? "-" + projectSuffix : "") + ".sirius";

        Path path = tempDir.resolve(name);

        if (sourceProject != null) {
            copySiriusProject(sourceProject, path);
            return siriusClient.projects().openProject(uid, path.toAbsolutePath().toString(), optFields);
        } else {
            return siriusClient.projects().createProject(uid, path.toAbsolutePath().toString(), optFields);
        }
    }

    public synchronized void deleteTestProject(ProjectInfo projectSpace) {
        try {
            siriusClient.projects().closeProject(projectSpace.getProjectId(), false);
            Files.deleteIfExists(Paths.get(projectSpace.getLocation()));
        } catch (WebClientResponseException e) {
            log.warn("Could not delete test project", e);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public synchronized void deleteTestSearchableDatabase(String databaseId, String location) {
        siriusClient.databases().removeDatabase(databaseId, true);
    }

    public synchronized void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        dir.delete();
    }

    public synchronized Path copyToTempDirectory(Path sourceDir, Path destinationDir, boolean recursive) throws IOException {
        Path tempDirectory = tempDir.resolve(UUID.randomUUID().toString());
        copyDirectory(sourceDir, tempDirectory, true);
        return tempDirectory;
    }

    public synchronized void copySiriusProject(Path sourcePath, Path destinationPath) throws IOException {
        Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
    }

    public synchronized void copyDirectory(Path src, Path dest, boolean recursive) throws IOException {
               if (!Files.exists(src)) {
            throw new IOException("Source directory not found: " + src);
        }

        Files.createDirectories(dest);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(src)) {
            for (Path entry : stream) {
                Path targetPath = dest.resolve(src.relativize(entry));
                if (Files.isDirectory(entry)) {
                    if (recursive) {
                        copyDirectory(entry, targetPath, true);
                    }
                } else {
                    Files.copy(entry, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    public synchronized void loginIfNeeded(){
        try {
            if (!siriusClient.account().isLoggedIn()) {
                siriusClient.account().login(
                        true,
                        new AccountCredentials()
                                .username(TestSetup.getInstance().getSIRIUS_USER_ENV())
                                .password(TestSetup.getInstance().getSIRIUS_PW_ENV()),
                        true, null
                );
            }
        } catch (Exception e) {
            log.error("Error while logging in. Some tests might fail as subsequent error.", e);
        }
    }

    public static FeatureImport makeProtonatedValium() {
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

        return new FeatureImport().name("Valium protonated").externalFeatureId("protonatedFeature")
                .ionMass(ionMassProtonated).charge(1).addDetectedAdductsItem("[M+H]+")
                .mergedMs1(protonatedMs1)
                .addMs2SpectraItem(protonatedMs2);
    }

    public static FeatureImport makeSodiatedValium() {
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

        return new FeatureImport().name("Valium sodiated").externalFeatureId("sodiatedFeature")
                .ionMass(ionMassSodiated).charge(1).addDetectedAdductsItem("[M+Na]+")
                .mergedMs1(sodiatedMs1)
                .addMs2SpectraItem(sodiatedMs2);
    }
}
