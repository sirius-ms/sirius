package io.sirius.ms.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sirius.ms.sdk.api.ActuatorApi;
import io.sirius.ms.sdk.client.ApiClient;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.ResponseEntity;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class SiriusSDKUtils {

    private static final Path GLOBAL_SIRIUS_CONFIG_DIR = Paths.get(System.getProperty("user.home"), ".sirius");
    private static final int MAJOR_SIRIUS_VERSION = 6;
    public static final Path SIRIUS_PID_FILE = GLOBAL_SIRIUS_CONFIG_DIR.resolve(String.format("sirius-%s.pid", MAJOR_SIRIUS_VERSION));
    public static final Path SIRIUS_PORT_FILE = GLOBAL_SIRIUS_CONFIG_DIR.resolve(String.format("sirius-%s.port", MAJOR_SIRIUS_VERSION));

    public static Process startSirius() throws Exception {
        return startSirius(null);
    }

    public static Process startSirius(@Nullable String configDir) throws Exception {
        return startSirius(configDir, null);
    }

    public static Process startSirius(@Nullable String configDir, @Nullable Path executable) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(executable != null ? executable.toAbsolutePath().toString() : findSiriusExecutable());
        if (configDir != null) {
            command.add("--workspace");
            command.add(configDir);
        }
        command.add("--noCite");
        command.add("rest");
        command.add("-s");
        ProcessBuilder processBuilder = new ProcessBuilder(command.toArray(String[]::new));

        Process process = processBuilder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("Web server failed to start. Port")) {
                    process.destroy();
                    throw new Exception("Sirius could not be started due to port conflict!");
                }
                if (line.contains("SIRIUS Service started successfully!") || line.contains("Workflow DONE")) {
                    return process;
                }
            }
        }

        System.out.println("WARNING: Could not verify whether Startup of SIRIUS was successful. Please verify via PID file and API health check.");
        if (!process.isAlive()) {
            throw new Exception("SIRIUS process already exited!");
        }

        return process;
    }

    public static String findSiriusExecutable() {
        // OS independent: search for EXE in environment variable
        Map<String, String> env = System.getenv();
        String siriusExe = env.get("SIRIUS_EXE");

        if ((siriusExe == null || siriusExe.trim().isEmpty()) && isWindows()) {
            try {
                siriusExe = getSiriusFromWindowsRegistry(); // Placeholder for Windows registry check
            } catch (Exception e) {
                log.error("Error when accessing windows registry. Try using alternative SIRIUS locations.", e);
                siriusExe = null;
            }
        }

        if (siriusExe == null || siriusExe.trim().isEmpty()) {
            log.debug("Could not find SIRIUS executable. Assuming it is in the global path as `sirius`.");
            return "sirius"; // As fallback assume sirius executable is on the path
        }

        return siriusExe;
    }

    // Placeholder for registry access (you'd need to use JNA or other libraries for this)
    private static String getSiriusFromWindowsRegistry() {
        if (!isWindows())
            return null;

        // Implementation for retrieving SiriusExe from Windows Registry would go here
        log.debug("Searching for sirius executable in windows registry currently not supported by the java SDK.");
        return null; // Placeholder for now
    }

    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    public static void redirectStdOutputToConsole(Process process) {
        try {
            process.getInputStream().transferTo(System.out);
        } catch (IOException e) {
           log.error("Error when reading process '{}' stdout. Future output might be lost.", process.pid(), e);
        }
    }

    public static void redirectErrOutputToConsole(Process process) {
        try {
            process.getInputStream().transferTo(System.err);
        } catch (IOException e) {
            log.error("Error when reading process '{}' error stream. Future output might be lost.", process.pid(), e);
        }
    }

    public static boolean restShutdown(ApiClient client) {
        return restShutdown(new ActuatorApi(client));
    }

    public static boolean restShutdown(ActuatorApi api) {
        try {
            api.shutdown();
            return true;
        } catch (Exception e) {
            log.error("Error when shutting down SIRIUS via REST call on '{}'. Cause: {}", api.getApiClient().getBasePath(), e.getMessage());
            return false;
        }
    }

    public static boolean restHealthCheck(ApiClient client) {
        return restHealthCheck(new ActuatorApi(client));
    }

    public static boolean restHealthCheck(ActuatorApi api) {
        try {
            ResponseEntity<String> resp = api.healthWithHttpInfo();
            if (!resp.getStatusCode().is2xxSuccessful())
                return false;
            // Calling the health API and checking response status
            JsonNode response = new ObjectMapper().readTree(resp.getBody());
            return "UP".equalsIgnoreCase(response.get("status").asText());
        } catch (Exception e) {
            log.error("Cannot connect to SIRIUS background service on '{}'. Cause: {}", api.getApiClient().getBasePath(), e.getMessage());
            return false;
        }
    }
}
