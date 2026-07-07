package io.sirius.ms.sdk.api;

import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

/**
 * Deterministically shuts down test-started SIRIUS instances at the end of each test JVM (i.e. each Gradle
 * test task), so a subsequent test task never finds a leftover server. Instances that were merely reused
 * (a developer's locally running SIRIUS) are left alive by {@code ShutdownMode.AUTO} semantics.
 * <p>
 * Registered via {@code META-INF/services/org.junit.platform.launcher.LauncherSessionListener}.
 */
public class SiriusServerShutdownListener implements LauncherSessionListener {

    @Override
    public void launcherSessionClosed(LauncherSession session) {
        TestSetup.destroyIfCreated();
        AccountTestSetup.destroyIfCreated();
    }
}
