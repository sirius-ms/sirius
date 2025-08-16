package io.sirius.ms.sdk;

import io.sirius.ms.sdk.client.ApiClient;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;

import static io.sirius.ms.sdk.SiriusSDKUtils.*;
import static io.sirius.ms.sdk.client.ApiClient.*;
import static java.lang.Thread.sleep;

@Slf4j
public final class SiriusSDK extends SiriusClient {

    private static final int PORT_IN_USE_EXIT_CODE = 10; // Should match the code in SiriusMiddlewareApplication

    private final ProcessHandle siriusProcessHandle;
    private final boolean shutDownSirius;

    public SiriusSDK(@NotNull String basePath, ProcessHandle siriusProcessHandle, boolean shutDownSirius) {
        this(basePath, siriusProcessHandle, shutDownSirius, null);
    }

    public SiriusSDK(@NotNull String basePath, @Nullable ProcessHandle siriusProcessHandle, boolean shutDownSirius, @Nullable ExecutorService asyncExecutor) {
        super(basePath, asyncExecutor);
        this.shutDownSirius = shutDownSirius;
        this.siriusProcessHandle = siriusProcessHandle;

        if (siriusProcessHandle != null && !siriusProcessHandle.isAlive()) {
            throw new IllegalStateException("Given SIRIUS process has already exited!");
        }
    }

    public long getPID() {
        if (siriusProcessHandle != null) {
           return siriusProcessHandle.pid();
        }
        return -1;
    }


    public void shutdown() {
        if (shutDownSirius) {
            SiriusSDKUtils.restShutdown(apiClient);

            if (siriusProcessHandle != null) {
                siriusProcessHandle.destroy();
            }
        }
    }

    @Override
    public synchronized void close() {
        super.close();
        shutdown();
    }


    public synchronized static SiriusSDK startAndConnectLocally() throws Exception {
        return startAndConnectLocally(true);
    }

    public synchronized static SiriusSDK startAndConnectLocally(boolean redirectSiriusOutput) throws Exception {
        return startAndConnectLocally(ShutdownMode.AUTO, redirectSiriusOutput);
    }

    public synchronized static SiriusSDK startAndConnectLocally(ShutdownMode shutDownMode, boolean redirectSiriusOutput) throws Exception {
        return startAndConnectLocally(shutDownMode, redirectSiriusOutput, false, null);
    }

    public synchronized static SiriusSDK startAndConnectLocally(ShutdownMode shutDownMode, boolean redirectSiriusOutput, boolean headless, @Nullable Path executable) throws Exception {
        @Nullable SiriusSDK sdk = findAndConnectLocally(shutDownMode, false);
        if (sdk != null)
            return sdk;

        log.info("Starting SIRIUS process from {}", executable);
        Process process = null;
        int retryAttempts = 3;
        for (int attempt = 1; attempt <= retryAttempts; attempt++) {
            try {
                process = SiriusSDKUtils.startSirius(null, executable, redirectSiriusOutput, headless);
                log.info("Awaiting SIRIUS API to be ready...");
                long start = System.currentTimeMillis();
                while (!Files.exists(SIRIUS_PORT_FILE)) {
                    sleep(1000);

                    if (!process.isAlive()) {
                        int exitCode = process.exitValue();
                        if (exitCode == PORT_IN_USE_EXIT_CODE) {
                            throw new Exception("Sirius could not be started due to port conflict!");
                        }
                        throw new Exception("SIRIUS process already exited with code " + exitCode);
                    }
                    if (System.currentTimeMillis() - start > 20000) {
                        process.destroy();
                        throw new TimeoutException("SIRIUS startup timed out. Starting SIRIUS failed!");
                    }
                }
                int port = Integer.parseInt(Files.readAllLines(SIRIUS_PORT_FILE).getFirst());
                return new SiriusSDK("http://localhost:" + port + "/", process.toHandle(), shutDownMode != ShutdownMode.NEVER);
            } catch (Exception e) {
                if (process != null) {
                    process.destroy();
                }
                log.error("Error when starting SIRIUS (try {}). Cause: {}", attempt, e.getMessage());
                if (attempt == retryAttempts) {
                    throw e;
                }
            }
        }
        throw new Exception("unreachable");
    }

    @Nullable
    public synchronized static SiriusSDK findAndConnectLocally(ShutdownMode shutDownMode, boolean killAndCleanIfUnresponsive) {
        //try to find and connect to running SIRIUS
        ProcessHandle processHandle = null;
        if (Files.exists(SIRIUS_PID_FILE)) {
            try {
                int pid = Integer.parseInt(Files.readAllLines(SIRIUS_PID_FILE).getFirst());
                processHandle = ProcessHandle.of(pid).orElse(null);
                if (processHandle != null && !processHandle.isAlive()) {
                    processHandle = null;
                    log.warn("Found SIRIUS process file but process seems to be dead. Ignoring it.");
                }
            } catch (NumberFormatException e) {
                log.warn("Found SIRIUS process file '{}' but could not read process number: '{}'", SIRIUS_PID_FILE.getFileName(), e.getMessage());
            } catch (IOException e) {
                log.warn("Could not read SIRIUS process file '{}': '{}'", SIRIUS_PID_FILE.getFileName(), e.getMessage());
            }
        }

        if (Files.exists(SIRIUS_PORT_FILE)) {
            try {
                int port = Integer.parseInt(Files.readAllLines(SIRIUS_PORT_FILE).getFirst());
                ApiClient apiClient = new ApiClient(buildWebClientBuilder(createDefaultObjectMapper(createDefaultDateFormat()))
                        .codecs(codecs -> codecs
                                .defaultCodecs()
                                .maxInMemorySize(100 * 1024 * 1024))
                        .build());
                apiClient.setBasePath("http://localhost:" + port + "/");

                if (SiriusSDKUtils.restHealthCheck(apiClient)) {
                    return new SiriusSDK(apiClient.getBasePath(), processHandle, shutDownMode == ShutdownMode.ALWAYS);
                } else if (killAndCleanIfUnresponsive) {
                    log.warn("Found SIRIUS port file '{}' but cannot connect to corresponding service. Seems to be unresponsive. Killing it.", SIRIUS_PID_FILE.getFileName());
                    restShutdown(apiClient);
                } else {
                    log.warn("Found SIRIUS port file '{}' but cannot connect to corresponding service. Seems to be unresponsive. Ignoring it.", SIRIUS_PID_FILE.getFileName());
                }
            } catch (NumberFormatException e) {
                log.warn("Found SIRIUS port file '{}' but could not read port number: '{}'", SIRIUS_PORT_FILE.getFileName(), e.getMessage());
            } catch (IOException e) {
                log.warn("Could not read SIRIUS port file '{}': '{}'", SIRIUS_PORT_FILE.getFileName(), e.getMessage());
            }
        }

        if (killAndCleanIfUnresponsive) {
            if (processHandle != null && processHandle.isAlive()) {
                log.warn("Process is still alive. Killing it forcefully!");
                processHandle.destroyForcibly();
            }

            log.debug("Deleting SIRIUS port and pid files from dead SIRIUS process '{},{}'", SIRIUS_PORT_FILE.getFileName(), SIRIUS_PID_FILE);
            try {
                Files.deleteIfExists(SIRIUS_PORT_FILE);
            } catch (IOException e) {
                log.error("Could not delete SIRIUS PID file: {}", SIRIUS_PORT_FILE.getFileName(), e);
            }
            try {
                Files.deleteIfExists(SIRIUS_PID_FILE);
            } catch (IOException e) {
                log.error("Could not delete SIRIUS pid file: {}", SIRIUS_PID_FILE.getFileName(), e);
            }
        }
        return null;
    }



    public enum ShutdownMode {
        AUTO, // Shuts down SIRIUS if it was started by this SiriusClient instance and not otherwise
        ALWAYS, // Always shuts down SIRIUS if this SiriusClient instance is shutdown, no matter it was started elsewhere
        NEVER // Never shuts down SIRIUS if this SiriusClient instance is shutdown, no matter it was started elsewhere
    }
}