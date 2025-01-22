package io.sirius.ms.sdk.api;

import io.sirius.ms.sdk.model.AccountCredentials;
import io.sirius.ms.sdk.model.ProjectInfo;
import io.sirius.ms.sdk.SiriusClient;
import io.sirius.ms.sdk.SiriusSDK;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
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
            bootJar = walker.filter(p -> p.getFileName().toString().matches("sirius_rest_service-.*-boot.jar")).findAny()
                    .orElseThrow(() -> new IOException("Could not finger boot jar for testing."));
        }

        siriusClient = SiriusSDK.startAndConnectLocally(SiriusSDK.ShutdownMode.AUTO, true, bootJar);
    }

    public void destroy(){
        siriusClient.close();
    }

    private Path getResourceFilePath(String resourcePath) {
        try {
            return Paths.get(getClass().getResource(resourcePath).toURI());
        } catch (Exception e) {
            throw new RuntimeException("Failed to load resource: " + resourcePath, e);
        }
    }

    public static TestSetup getInstance() {
        if (INSTANCE == null)
            INSTANCE = new TestSetup();
        return INSTANCE;
    }

    public ProjectInfo createTestProject(String projectSuffix, Path sourceProject) throws IOException {
        String uid = UUID.randomUUID().toString();
        String name = "test-project-" + uid;

        if (projectSuffix != null) {
            name += "-" + projectSuffix + ".sirius";
        }

        Path path = tempDir.resolve(name);

        if (sourceProject != null) {
            copySiriusProject(sourceProject, path);
            return siriusClient.projects().openProject(uid, path.toAbsolutePath().toString(), null);
        } else {
            return siriusClient.projects().createProject(uid, path.toAbsolutePath().toString(), null);
        }
    }

    public void deleteTestProject(ProjectInfo projectSpace) {
        try {
            siriusClient.projects().closeProject(projectSpace.getProjectId());
            Files.deleteIfExists(Paths.get(projectSpace.getLocation()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void deleteTestSearchableDatabase(String databaseId, String location) {
        siriusClient.databases().removeDatabase(databaseId, true);
    }

    public void deleteDirectory(File dir) {
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

    public Path copyToTempDirectory(Path sourceDir, Path destinationDir, boolean recursive) throws IOException {
        Path tempDirectory = tempDir.resolve(UUID.randomUUID().toString());
        copyDirectory(sourceDir, tempDirectory, true);
        return tempDirectory;
    }

    public void copySiriusProject(Path sourcePath, Path destinationPath) throws IOException {
        Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
    }

    public void copyDirectory(Path src, Path dest, boolean recursive) throws IOException {
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

    public void loginIfNeeded(){
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
}
