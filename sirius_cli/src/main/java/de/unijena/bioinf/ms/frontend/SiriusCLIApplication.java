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

package de.unijena.bioinf.ms.frontend;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.auth.AuthService;
import de.unijena.bioinf.auth.AuthServices;
import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.ms.annotations.PrintCitations;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.CLIRootOptions;
import de.unijena.bioinf.ms.frontend.subtools.StandaloneTool;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.frontend.workflow.WorkFlowSupplier;
import de.unijena.bioinf.ms.frontend.workflow.WorkflowBuilder;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.NitriteProjectSpaceManagerFactory;
import de.unijena.bioinf.projectspace.SiriusProjectSpaceManagerFactory;
import de.unijena.bioinf.rest.ProxyManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Stream;


public class SiriusCLIApplication {

    public static final String APP_TYPE_PROPERTY_KEY = "de.unijena.bioinf.sirius.apptype";
    protected static Run RUN = null;
    protected static boolean successfulParsed;

    protected static final boolean TIME = false;
    protected static long t1;

    public static void main(String[] args) {
        runMain(args, List.of());
    }

    public static void runMain(String[] args, List<StandaloneTool<?>> injectTools) {
        System.setProperty(APP_TYPE_PROPERTY_KEY, "CLI");
        {
            List<String> argsl = List.of(args);
            int i = argsl.indexOf("--workspace");
            if (i >= 0)
                System.setProperty("de.unijena.bioinf.sirius.ws.location", args[i + 1].replace("'", "").replace("\"", ""));
        }
        if (TIME)
            t1 = System.currentTimeMillis();
        try {
            // The spring app classloader seems not to be correctly inherited to sub thread
            // So we need to ensure that the apache.configuration2 libs gets access otherwise.
            if (Boolean.parseBoolean(System.getProperty("de.unijena.bioinf.sirius.springSupport", "false")))
                SiriusJobs.enforceClassLoaderGlobally(Thread.currentThread().getContextClassLoader());

            configureShutDownHook(shutdownWebservice());
            measureTime("Start Run method");
            run(args, () -> new WorkflowBuilder(
                    PropertyManager.getProperty("sirius.middleware.project-space", null, "NITRITE-NOSQL").equals("NITRITE-NOSQL")
                            ? new CLIRootOptions(new DefaultParameterConfigLoader(), new NitriteProjectSpaceManagerFactory())
                            : new CLIRootOptions(new DefaultParameterConfigLoader(), new SiriusProjectSpaceManagerFactory())
                    , injectTools));
        } finally {
            System.exit(0);
        }
    }

    public static void measureTime(String message) {
        if (TIME) {
            long t2 = System.currentTimeMillis();
            System.err.println("==> " + message + " - " + (t2 - t1) / 1000d);
            t1 = t2;
        }
    }

    public static void configureShutDownHook(@NotNull final Runnable... additionalActions) {
        //shut down hook to clean up when sirius is shutting down
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ApplicationCore.DEFAULT_LOGGER.info("CLI shut down hook: SIRIUS is cleaning up threads and shuts down...");
            try {
                if (SiriusCLIApplication.RUN != null)
                    SiriusCLIApplication.RUN.cancel();
                Stream.of(additionalActions).forEach(Runnable::run);
                JobManager.shutDownNowAllInstances();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                try {
                    AuthService as = ApplicationCore.WEB_API().getAuthService();
                    if (as.isLoggedIn())
                        AuthServices.writeRefreshToken(ApplicationCore.WEB_API().getAuthService(), ApplicationCore.TOKEN_FILE, true);
                    else
                        Files.deleteIfExists(ApplicationCore.TOKEN_FILE);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    ProxyManager.disconnect();
                    if (successfulParsed && PropertyManager.DEFAULTS.createInstanceWithDefaults(PrintCitations.class).value)
                        ApplicationCore.BIBTEX.citeToSystemErr();
                }

            }
        }));
    }

    public static Runnable shutdownWebservice() {
        return () -> {
            try {
                ApplicationCore.WEB_API().shutdown();
            } catch (IOException e) {
                LoggerFactory.getLogger(SiriusCLIApplication.class).warn("Could not clean up Server data! " + e.getMessage());
                LoggerFactory.getLogger(SiriusCLIApplication.class).debug("Could not clean up Server data!", e);
            }
        };
    }

    public static void run(String[] args, WorkFlowSupplier supplier) {
        try {
            if (RUN != null)
                throw new IllegalStateException("Application can only run Once!");
            measureTime("init Run");
            RUN = new Run(supplier.make(), true);
            measureTime("Start Parse args");
            RUN.parseArgs(args);
            successfulParsed = RUN.makeWorkflow() != null;
            measureTime("Parse args Done!");
            if (successfulParsed) {
                measureTime("Compute");
                RUN.compute();
                measureTime("Compute DONE!");
            }
        } catch (Throwable e) {
            LoggerFactory.getLogger(SiriusCLIApplication.class).error("Unexpected Error!", e);
        }
    }
}
