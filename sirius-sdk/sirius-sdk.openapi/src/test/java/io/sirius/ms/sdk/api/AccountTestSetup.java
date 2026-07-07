package io.sirius.ms.sdk.api;

import io.sirius.ms.sdk.SiriusClient;
import io.sirius.ms.sdk.SiriusSDK;
import io.sirius.ms.sdk.model.AccountCredentials;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.opentest4j.TestAbortedException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Test setup for account-state tests (login, logout, subscription switching) that mutate the server-global
 * account state. Unlike {@link TestSetup}, it never reuses a running SIRIUS instance but always starts a
 * dedicated one with a fresh, isolated workspace (own token store), so the tests can freely change account
 * state without interfering with other test suites or the developer's local SIRIUS installation.
 * <p>
 * Since SIRIUS enforces a single running service per user, the account-state tests are skipped (JUnit
 * assumption) when another SIRIUS instance is already running, e.g. during local development. In CI no
 * instance pre-exists, so the tests always run there.
 */
@Getter
@Slf4j
public class AccountTestSetup {

    private static AccountTestSetup INSTANCE;
    private static String unavailableReason;

    private final Path workspaceDir;
    private final SiriusClient siriusClient;
    private final String SIRIUS_USER_ENV;
    private final String SIRIUS_PW_ENV;
    private final String SIRIUS_ACTIVE_SUB;

    @SneakyThrows
    private AccountTestSetup() {
        SIRIUS_USER_ENV = System.getenv("SIRIUS_USER");
        SIRIUS_PW_ENV = System.getenv("SIRIUS_PW");
        SIRIUS_ACTIVE_SUB = System.getenv("SIRIUS_SUB");

        TestSetup.requireCredentials(SIRIUS_USER_ENV, "SIRIUS_USER");
        TestSetup.requireCredentials(SIRIUS_PW_ENV, "SIRIUS_PW");
        TestSetup.requireCredentials(SIRIUS_ACTIVE_SUB, "SIRIUS_SUB");

        workspaceDir = Files.createTempDirectory("sirius-account-test-workspace-");
        try {
            siriusClient = SiriusSDK.startAndConnectLocallyIsolated(
                    SiriusSDK.ShutdownMode.AUTO, true, true, true, TestSetup.findBootJar(), workspaceDir);
        } catch (Exception e) {
            deleteRecursively(workspaceDir);
            throw e;
        }
    }

    /**
     * @throws TestAbortedException (skips the calling test) when a SIRIUS instance is already running,
     *                              so the isolated account-test instance cannot be started.
     */
    public synchronized static AccountTestSetup getInstance() {
        if (unavailableReason != null)
            throw new TestAbortedException(unavailableReason);
        if (INSTANCE == null) {
            try {
                INSTANCE = new AccountTestSetup();
            } catch (SiriusSDK.InstanceAlreadyRunningException e) {
                unavailableReason = "Skipping account-state tests: " + e.getMessage();
                log.warn(unavailableReason);
                throw new TestAbortedException(unavailableReason, e);
            }
        }
        return INSTANCE;
    }

    /**
     * Logs in with the test account if not logged in already. Unlike the main suite, account-state tests
     * change the login state on purpose, so this is used to establish each test's precondition.
     */
    public synchronized void ensureLoggedIn() {
        try {
            if (!siriusClient.account().isLoggedIn()) {
                siriusClient.account().login(
                        true,
                        new AccountCredentials()
                                .username(SIRIUS_USER_ENV)
                                .password(SIRIUS_PW_ENV),
                        true, null
                );
            }
        } catch (Exception e) {
            throw new IllegalStateException("Login on the isolated account-test instance failed.", e);
        }
    }

    public synchronized void destroy() {
        try {
            siriusClient.close();
        } finally {
            deleteRecursively(workspaceDir);
        }
    }

    public synchronized static void destroyIfCreated() {
        if (INSTANCE != null) {
            INSTANCE.destroy();
            INSTANCE = null;
        }
    }

    private static void deleteRecursively(Path dir) {
        try (var walker = Files.walk(dir)) {
            walker.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    log.warn("Could not delete temporary account-test workspace file: {}", p, e);
                }
            });
        } catch (IOException e) {
            log.warn("Could not clean up temporary account-test workspace: {}", dir, e);
        }
    }
}
