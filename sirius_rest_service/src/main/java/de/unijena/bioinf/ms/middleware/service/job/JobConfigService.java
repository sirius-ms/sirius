package de.unijena.bioinf.ms.middleware.service.job;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.chemdb.annotations.FormulaSearchDB;
import de.unijena.bioinf.chemdb.annotations.StructureSearchDB;
import de.unijena.bioinf.chemdb.custom.CustomDataSources;
import de.unijena.bioinf.ms.frontend.core.Workspace;
import de.unijena.bioinf.ms.middleware.model.compute.JobSubmission;
import de.unijena.bioinf.ms.middleware.model.compute.StoredJobSubmission;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Service
public class JobConfigService {

    public final static String DEFAULT_CONFIG_NAME = "Default";
    public final static String DB_SEARCH_CONFIG_NAME = "DB-Search";
    public final static String MS1_CONFIG_NAME = "MS1";

    public final SequencedMap<String, StoredJobSubmission> predefinedConfigs;

    public JobConfigService() {
        predefinedConfigs = new LinkedHashMap<>();
        predefinedConfigs.put(DEFAULT_CONFIG_NAME, wrapSubmission(DEFAULT_CONFIG_NAME, false, getDefaultJobConfig(false)));
        predefinedConfigs.put(DB_SEARCH_CONFIG_NAME, createDBSearchConfig());
        predefinedConfigs.put(MS1_CONFIG_NAME, createMS1Config());
    }

    public JobSubmission getDefaultJobConfig(boolean includeCustomDbsForStructureSearch) {
        return JobSubmission.createDefaultInstance(true, includeCustomDbsForStructureSearch);
    }

    private StoredJobSubmission createMS1Config() {
        JobSubmission js = getDefaultJobConfig(false);
        js.getFormulaIdParams().setPerformDenovoBelowMz(Double.POSITIVE_INFINITY);
        js.getFormulaIdParams().setPerformBottomUpSearch(false);
        js.getFingerprintPredictionParams().setEnabled(false);
        js.getCanopusParams().setEnabled(false);
        js.getStructureDbSearchParams().setEnabled(false);

        return wrapSubmission(MS1_CONFIG_NAME, false, js);
    }

    private StoredJobSubmission createDBSearchConfig() {
        JobSubmission js = getDefaultJobConfig(false);

        js.getFormulaIdParams().setPerformDenovoBelowMz(0d);
        js.getFormulaIdParams().setPerformBottomUpSearch(false);

        // Same DBs as by default in GUI
        Set<CustomDataSources.Source> defaultDBs = new HashSet<>(PropertyManager.DEFAULTS.createInstanceWithDefaults(FormulaSearchDB.class).searchDBs);
        defaultDBs.addAll(PropertyManager.DEFAULTS.createInstanceWithDefaults(StructureSearchDB.class).searchDBs);
        js.getFormulaIdParams().setFormulaSearchDBs(defaultDBs.stream().map(CustomDataSources.Source::name).toList());

        return wrapSubmission(DB_SEARCH_CONFIG_NAME, false, js);
    }

    public List<StoredJobSubmission> getAllConfigs() {
        List<StoredJobSubmission> configs = new ArrayList<>();
        configs.addAll(predefinedConfigs.values());
        configs.addAll(getUserJobConfigs());
        return configs;
    }

    public List<StoredJobSubmission> getUserJobConfigs() {
        try {
            return FileUtils.listAndClose(Workspace.runConfigDir, s -> s.filter(Files::isRegularFile)
                    .map(this::readFromFile).toList());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected Error when crawling job-config files.", e);
        }
    }

    public StoredJobSubmission getConfig(String name) {
        if (predefinedConfigs.containsKey(name)) {
            return predefinedConfigs.get(name);
        }
        return readFromFile(nameToPath(name));
    }

    public boolean configExists(String name) {
        if (predefinedConfigs.containsKey(name)) {
            return true;
        }
        Path path = nameToPath(name);
        return Files.exists(path) && Files.isRegularFile(path);
    }

    public void addUserConfig(String name, JobSubmission config) {
        if (configExists(name)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The job-config '" + name + "' already exists.");
        }
        writeToFile(name, config);
    }

    public void updateConfig(String name, JobSubmission config) {
        if (predefinedConfigs.containsKey(name)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The job-config name '" + name + "' is already used for a predefined config.");
        }
        writeToFile(name, config);
    }

    public void deleteUserConfig(String name) {
        if (predefinedConfigs.containsKey(name)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A predefined config '" + name + "' cannot be deleted.");
        }
        try {
            Files.deleteIfExists(nameToPath(name));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected Error when deleting config file.", e);
        }
    }

    private StoredJobSubmission wrapSubmission(String configName, boolean editable, JobSubmission js) {
        return StoredJobSubmission.builder()
                .name(configName)
                .editable(editable)
                .jobSubmission(js)
                .build();
    }

    private StoredJobSubmission readFromFile(Path path) {
        if (Files.notExists(path) || !Files.isRegularFile(path))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Job-config '" + path + "' does not exist.");

        try (InputStream s = Files.newInputStream(path)) {
            JobSubmission sub = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .readValue(s, JobSubmission.class);
            return wrapSubmission(pathToName(path), true, sub);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error when reading job-config file '" + path + "'.", e);
        }
    }

    private void writeToFile(String name, JobSubmission config) {
        Path path = nameToPath(name);
        try (OutputStream s = Files.newOutputStream(path)) {
            new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(s, config);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected Error when reading default config file.", e);
        }
    }

    private static Path nameToPath(String name) {
        return Workspace.runConfigDir.resolve(name + ".json");
    }

    private static String pathToName(Path path) {
        return path.getFileName().toString().replaceFirst("\\.json$", "");
    }
}
