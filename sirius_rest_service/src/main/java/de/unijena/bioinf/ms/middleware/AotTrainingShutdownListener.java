/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.middleware;

import de.unijena.bioinf.ms.middleware.service.gui.GuiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;

import java.awt.GraphicsEnvironment;

/**
 * Drives the {@code aot-train} run mode. Once the embedded web server is up it waits (off the Spring
 * event-dispatch thread) until the application is fully initialized - in GUI mode until a GUI
 * instance has been registered, in headless mode just for a short settle period - and then shuts the
 * application down cleanly.
 * <p>
 * The clean shutdown mirrors the sequence used by the GUI window-close handler
 * ({@code guiService.shutdown()} -> {@code context.close()} -> {@code System.exit(0)}). A normal JVM
 * exit is required so that a JVM launched with {@code -XX:AOTCacheOutput=...} assembles and writes
 * the Project Leyden AOT cache. A hard timeout guarantees the process can never hang an installer.
 */
@Slf4j
public class AotTrainingShutdownListener implements ApplicationListener<WebServerInitializedEvent> {

    /** Hard cap on how long we wait for the GUI to appear before shutting down anyway. */
    private static final long READINESS_TIMEOUT_MS = 120_000L;
    /** Poll interval while waiting for the GUI instance to register. */
    private static final long GUI_POLL_INTERVAL_MS = 500L;
    /** Extra time after readiness to let lazy initialization / EDT painting exercise more classes. */
    private static final long SETTLE_MS = 5_000L;

    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        final ApplicationContext ctx = event.getApplicationContext();
        // Run off the event-dispatch thread so we don't block Spring startup while waiting.
        Thread t = new Thread(() -> waitForReadinessAndShutdown(ctx), "aot-training-shutdown");
        t.setDaemon(true);
        t.start();
    }

    private void waitForReadinessAndShutdown(ApplicationContext ctx) {
        GuiService guiService = null;
        try {
            final long deadline = System.currentTimeMillis() + READINESS_TIMEOUT_MS;
            // Gate on the actual headless flag, NOT on guiService bean presence: the AOT-processed
            // context registers the guiService bean even in headless mode, so a bean-presence check
            // would make a headless run wait pointlessly for a GUI that never appears (until the hard
            // timeout). The headless flag is published into the environment by the launcher.
            boolean headless = ctx.getEnvironment()
                    .getProperty("de.unijena.bioinf.sirius.headless", Boolean.class, GraphicsEnvironment.isHeadless());
            if (!headless && ctx.containsBean("guiService")) {
                guiService = ctx.getBean(GuiService.class);
                log.info("AOT training: waiting for the GUI to initialize...");
                while (guiService.findGui().isEmpty() && System.currentTimeMillis() < deadline)
                    Thread.sleep(GUI_POLL_INTERVAL_MS);

                if (guiService.findGui().isEmpty())
                    log.warn("AOT training: GUI did not initialize within {} ms. Shutting down anyway.", READINESS_TIMEOUT_MS);
                else
                    log.info("AOT training: GUI initialized. Letting the application settle before shutdown...");
            } else {
                log.info("AOT training: running headless. Letting the service settle before shutdown...");
            }
            Thread.sleep(SETTLE_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.warn("AOT training: error while waiting for application readiness.", e);
        } finally {
            log.info("AOT training run complete. Shutting down cleanly so the AOT cache is written...");
            try {
                if (guiService != null)
                    guiService.shutdown();
            } catch (Exception e) {
                log.warn("AOT training: error during GUI shutdown.", e);
            }
            try {
                if (ctx instanceof ConfigurableApplicationContext configurableCtx)
                    configurableCtx.close();
            } catch (Exception e) {
                log.warn("AOT training: error while closing the application context.", e);
            }
            System.exit(0);
        }
    }
}
