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

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ms.annotations.PrintCitations;
import de.unijena.bioinf.ms.frontend.BackgroundRuns;
import de.unijena.bioinf.ms.frontend.Run;
import de.unijena.bioinf.ms.frontend.SiriusCLIApplication;
import de.unijena.bioinf.ms.frontend.SiriusGUIApplication;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.CLIRootOptions;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.frontend.subtools.gui.GuiAppOptions;
import de.unijena.bioinf.ms.frontend.subtools.middleware.MiddlewareAppOptions;
import de.unijena.bioinf.ms.frontend.workflow.WorkflowBuilder;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.ProjectSpaceManagerFactory;
import de.unijena.bioinf.projectspace.SiriusProjectSpace;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@SpringBootApplication
public class SiriusMiddlewareApplication extends SiriusCLIApplication implements CommandLineRunner {
    protected static CLIRootOptions rootOptions;
    protected final SiriusContext context;
    protected static ConfigurableApplicationContext appContext = null;

    public SiriusMiddlewareApplication(SiriusContext context) {
        this.context = context;
    }

    public static void main(String[] args) {
        System.setProperty("de.unijena.bioinf.sirius.springSupport", "true");
        //run gui if not parameter ist given, to get rid of a second launcher
        if (args == null || args.length == 0)
            args = new String[]{"gui"};

        if (Arrays.stream(args).anyMatch(it ->
                it.equalsIgnoreCase("asService") ||
                it.equalsIgnoreCase("rest") ||
                it.equalsIgnoreCase("-h") ||
                it.equalsIgnoreCase("--help")
        )) {
            System.setProperty(APP_TYPE_PROPERTY_KEY, "SERVICE");
            SiriusJobs.enforceClassLoaderGlobally(Thread.currentThread().getContextClassLoader());
            ApplicationCore.DEFAULT_LOGGER.info("Starting Application Core");

            //todo convert to a native spring based approach
            try {
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    ApplicationCore.DEFAULT_LOGGER.info("CLI shut down hook: SIRIUS is cleaning up threads and shuts down...");
                    try {
                        if (SiriusCLIApplication.RUN != null)
                            SiriusCLIApplication.RUN.cancel();
                    } finally {
                        if (successfulParsed && PropertyManager.DEFAULTS.createInstanceWithDefaults(PrintCitations.class).value)
                            ApplicationCore.BIBTEX.citeToSystemErr();
                    }
                }));

                PropertyManager.setProperty("de.unijena.bioinf.sirius.BackgroundRuns.autoremove", "false");
                final DefaultParameterConfigLoader configOptionLoader = new DefaultParameterConfigLoader();

                final ProjectSpaceManagerFactory<?, ?> psf = new ProjectSpaceManagerFactory.Default();
                /*if (Arrays.stream(args).anyMatch(it -> it.equalsIgnoreCase("gui"))) {
                    psf = new GuiProjectSpaceManagerFactory();
                } else {
                    psf = new ProjectSpaceManagerFactory.Default();
                }*/


                rootOptions = new CLIRootOptions<>(configOptionLoader, psf);
                if (RUN != null)
                    throw new IllegalStateException("Application can only run Once!");
                measureTime("init Run");
                RUN = new Run(new WorkflowBuilder<>(rootOptions, configOptionLoader, BackgroundRuns.getBufferFactory(),
                        List.of(new GuiAppOptions(null), new MiddlewareAppOptions<>())));
                measureTime("Start Parse args");
                boolean b = RUN.parseArgs(args);
                measureTime("Parse args Done!");

                if (b) {
                    // decides whether the app runs infinitely
                    WebApplicationType webType = WebApplicationType.NONE;
                    if (RUN.getFlow() instanceof MiddlewareAppOptions.Flow) //run rest service (infinitely)
                        webType = WebApplicationType.SERVLET;

                    measureTime("Configure Boot Environment");
                    //configure boot app
                    final SpringApplicationBuilder appBuilder = new SpringApplicationBuilder(SiriusMiddlewareApplication.class)
                            .web(webType)
                            .headless(webType.equals(WebApplicationType.NONE))
//                            .headless(!(psf instanceof GuiProjectSpaceManagerFactory))
                            .bannerMode(Banner.Mode.OFF);
                    measureTime("Start Workflow");
                    appContext = appBuilder.run(args);
//                    appContext.close();

                    measureTime("Workflow DONE!");
                    System.err.println("SIRIUS Service started successfully!");
                } else {
                    System.exit(0); //todo real error codes
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else {
            SiriusGUIApplication.main(args);
        }
    }


    @Override
    public void run(String... args) throws Exception {
        measureTime("Add PS to servlet Context");
        final SiriusProjectSpace ps = rootOptions.getProjectSpace().projectSpace();
        context.addProjectSpace(ps.getLocation().getFileName().toString(), ps);
        RUN.compute();
    }
}