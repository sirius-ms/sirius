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

import static io.sirius.ms.sdk.SiriusSDKUtils.SIRIUS_PID_FILE;
import static io.sirius.ms.sdk.SiriusSDKUtils.SIRIUS_PORT_FILE;
import static io.sirius.ms.sdk.client.ApiClient.*;
import static java.lang.Thread.sleep;

@Slf4j
public final class SiriusSDK extends SiriusClient {
    //process started by the client with full control
    private Process siriusProcess;
    //external process with limited control
    private ProcessHandle siriusProcessHandle;
    private Thread threadStdOut, threadStdErr;
    public final boolean shutDownSirius;


    public SiriusSDK(String basePath, Process siriusProcess, boolean shutDownSirius, boolean redirectSiriusOutput) throws Exception {
        this(basePath, siriusProcess, shutDownSirius, redirectSiriusOutput, null);
    }

    public SiriusSDK(@NotNull String basePath, @Nullable Process siriusProcess, boolean shutDownSirius, boolean redirectSiriusOutput, @Nullable ExecutorService asyncExecutor) {
        super(basePath, asyncExecutor);
        this.shutDownSirius = shutDownSirius;
        this.siriusProcess = siriusProcess;

        if (this.siriusProcess != null) {
            if (!this.siriusProcess.isAlive()) {
                throw new IllegalStateException("Given SIRIUS process has already exited!");
            }

            if (redirectSiriusOutput) {
                threadStdOut = new Thread(() -> SiriusSDKUtils.redirectStdOutputToConsole(this.siriusProcess));
                threadStdOut.start();
                threadStdErr = new Thread(() -> SiriusSDKUtils.redirectErrOutputToConsole(this.siriusProcess));
                threadStdErr.start();
            }
        }
    }

    public SiriusSDK(@NotNull String basePath, ProcessHandle siriusProcessHandle, boolean shutDownSirius) throws Exception {
        this(basePath, siriusProcessHandle, shutDownSirius, null);
    }

    public SiriusSDK(@NotNull String basePath, @Nullable ProcessHandle siriusProcessHandle, boolean shutDownSirius, @Nullable ExecutorService asyncExecutor) {
        super(basePath, asyncExecutor);
        this.shutDownSirius = shutDownSirius;
        this.siriusProcessHandle = siriusProcessHandle;

        if (this.siriusProcess != null) {
            if (!this.siriusProcess.isAlive()) {
                throw new IllegalStateException("Given SIRIUS process has already exited!");
            }
        }
    }


    public void shutdown() {
        if (shutDownSirius) {
            SiriusSDKUtils.restShutdown(apiClient);
        }

        if (threadStdOut != null && threadStdOut.isAlive()) {
            try {
                threadStdOut.join();
            } catch (InterruptedException e) {
                log.error("Error when stopping std out redirect.", e);
            }
        }

        if (threadStdErr != null && threadStdErr.isAlive()) {
            try {
                threadStdErr.join();
            } catch (InterruptedException e) {
                log.error("Error when stopping std err redirect.", e);
            }
        }

        if (shutDownSirius && siriusProcess != null && siriusProcess.isAlive())
            siriusProcess.destroy();

        if (shutDownSirius && siriusProcessHandle != null && siriusProcessHandle.isAlive())
            siriusProcessHandle.destroy();
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
        return startAndConnectLocally(shutDownMode, redirectSiriusOutput, null);
    }

    public synchronized static SiriusSDK startAndConnectLocally(ShutdownMode shutDownMode, boolean redirectSiriusOutput, @Nullable Path executable) throws Exception {

        //try to find and connect to running SIRIUS
        {
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
                    } else {
                        log.warn("Found SIRIUS port file '{}' but cannot connect to corresponding service. Seems to be unresponsive. Ignoring it.", SIRIUS_PID_FILE.getFileName());
                    }
                } catch (NumberFormatException e) {
                    log.warn("Found SIRIUS port file '{}' but could not read port number: '{}'", SIRIUS_PORT_FILE.getFileName(), e.getMessage());
                } catch (IOException e) {
                    log.warn("Could not read SIRIUS port file '{}': '{}'", SIRIUS_PORT_FILE.getFileName(), e.getMessage());
                }
            }

        }

        log.info("Starting SIRIUS process from {}", executable);
        Process process = null;
        for (int tries = 1; tries < 4; tries++) {
            try {
                process = SiriusSDKUtils.startSirius(null, executable);
                if (!Files.exists(SIRIUS_PORT_FILE)) {
                    log.info("Awaiting SIRIUS API to be ready...");
                    long start = System.currentTimeMillis();
                    while (!Files.exists(SIRIUS_PORT_FILE)) {
                        sleep(1000);
                        if (System.currentTimeMillis() - start > 20000) {
                            process.destroy();
                            throw new TimeoutException("SIRIUS startup timed out. Starting SIRIUS failed!");
                        }
                    }
                }
                int port = Integer.parseInt(Files.readAllLines(SIRIUS_PORT_FILE).getFirst());
                return new SiriusSDK("http://localhost:" + port + "/", process, shutDownMode != ShutdownMode.NEVER, redirectSiriusOutput);
            } catch (Exception e) {
                if (tries < 3) {
                    log.error("Error when starting SIRIUS (try '{}'). Cause: {}", tries, e.getMessage());
                    if (process != null) {
                        process.destroy();
                    }
                } else {
                    throw e;
                }
            }
        }
        if (process != null) {
            process.destroy();
        }
        throw new Exception("SIRIUS startup failed!");
    }

    public enum ShutdownMode {
        AUTO, // Shuts down SIRIUS if it was started by this SiriusClient instance and not otherwise
        ALWAYS, // Always shuts down SIRIUS if this SiriusClient instance is shutdown, no matter it was started elsewhere
        NEVER // Never shuts down SIRIUS if this SiriusClient instance is shutdown, no matter it was started elsewhere
    }
}