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
import de.unijena.bioinf.auth.AuthService;
import de.unijena.bioinf.auth.AuthServices;
import de.unijena.bioinf.jjobs.SwingJobManager;
import de.unijena.bioinf.ms.annotations.PrintCitations;
import de.unijena.bioinf.ms.frontend.BackgroundRuns;
import de.unijena.bioinf.ms.frontend.Run;
import de.unijena.bioinf.ms.frontend.SiriusCLIApplication;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.core.Workspace;
import de.unijena.bioinf.ms.frontend.subtools.CLIRootOptions;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.frontend.subtools.fingerblast.FingerblastSubToolJob;
import de.unijena.bioinf.ms.frontend.subtools.middleware.MiddlewareAppOptions;
import de.unijena.bioinf.ms.frontend.workflow.WorkflowBuilder;
import de.unijena.bioinf.ms.middleware.model.projects.ProjectInfo;
import de.unijena.bioinf.ms.middleware.service.gui.GuiService;
import de.unijena.bioinf.ms.middleware.service.projects.SiriusProjectSpaceProviderImpl;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.*;
import de.unijena.bioinf.projectspace.fingerid.*;
import de.unijena.bioinf.rest.ProxyManager;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.ApplicationPidFileWriter;
import org.springframework.boot.web.context.WebServerPortFileWriter;
import org.springframework.context.ConfigurableApplicationContext;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

@SpringBootApplication
@Slf4j
public class SiriusMiddlewareApplication extends SiriusCLIApplication implements CommandLineRunner, DisposableBean {
    private static CLIRootOptions<?, ?> rootOptions;

    public static CLIRootOptions<?, ?> getRootOptions() {
        return rootOptions;
    }

    //todo nightsky implement GUI only mode
    public static void main(String[] args) {
        //enable gui support
        {
            // modify fingerid subtool so that it works with reduced GUI candidate list.
            FingerblastSubToolJob.formulaResultComponentsToClear.add(FBCandidatesTopK.class);
            FingerblastSubToolJob.formulaResultComponentsToClear.add(FBCandidateFingerprintsTopK.class);

            final @NotNull Supplier<ProjectSpaceConfiguration> dc = ProjectSpaceManager.DEFAULT_CONFIG;
            ProjectSpaceManager.DEFAULT_CONFIG = () -> {
                final ProjectSpaceConfiguration config = dc.get();
                config.registerComponent(FormulaResult.class, FBCandidatesTopK.class, new FBCandidatesSerializerTopK(FBCandidateNumber.GUI_DEFAULT));
                config.registerComponent(FormulaResult.class, FBCandidateFingerprintsTopK.class, new FBCandidateFingerprintSerializerTopK(FBCandidateNumber.GUI_DEFAULT));
                return config;
            };
        }

        if (args == null || args.length == 0)
            args = new String[]{"rest", "-s", "--gui"};
        if (Arrays.stream(args).anyMatch(it ->
                it.equalsIgnoreCase(MiddlewareAppOptions.class.getAnnotation(CommandLine.Command.class).name())
                        || Arrays.stream(MiddlewareAppOptions.class.getAnnotation(CommandLine.Command.class).aliases())
                        .anyMatch(cmd -> cmd.equalsIgnoreCase(it))
                        || it.equalsIgnoreCase("-h") || it.equalsIgnoreCase("--help") // just to get Middleware help.
        )) {

            System.setProperty("de.unijena.bioinf.sirius.springSupport", "true");
            System.setProperty(APP_TYPE_PROPERTY_KEY, "SERVICE");
//            SiriusJobs.enforceClassLoaderGlobally(Thread.currentThread().getContextClassLoader());
            measureTime("Init Swing Job Manager");
            // The spring app classloader seems not to be correctly inherited to sub thread
            // So we need to ensure that the apache.configuration2 libs gets access otherwise.
            // SwingJobManager is needed to show loading screens in GUI
            SiriusJobs.setJobManagerFactory((cpuThreads) -> new SwingJobManager(
                    cpuThreads,
                    Math.min(PropertyManager.getNumberOfThreads(), 4),
                    Thread.currentThread().getContextClassLoader()
            ));

            ApplicationCore.DEFAULT_LOGGER.info("Starting Application Core");

            // todo convert to a native spring based approach
            try {
                PropertyManager.setProperty("de.unijena.bioinf.sirius.BackgroundRuns.autoremove", "false");
                final DefaultParameterConfigLoader configOptionLoader = new DefaultParameterConfigLoader();

                final ProjectSpaceManagerFactory<?, ?> psf = new ProjectSpaceManagerFactory.Default();

                rootOptions = new CLIRootOptions<>(configOptionLoader, psf);
                MiddlewareAppOptions<Instance, ProjectSpaceManager<Instance>> middlewareOpts = new MiddlewareAppOptions<>();

                if (RUN != null)
                    throw new IllegalStateException("Application can only run Once!");
                measureTime("init Run");
                RUN = new Run(new WorkflowBuilder<>(rootOptions, configOptionLoader, BackgroundRuns.getBufferFactory(),
                        List.of(middlewareOpts)));
                measureTime("Start Parse args");
                successfulParsed = RUN.parseArgs(args);
                measureTime("Parse args Done!");

                if (successfulParsed) {
                    // decides whether the app runs infinitely
                    WebApplicationType webType = WebApplicationType.NONE;
                    if (RUN.getFlow() instanceof MiddlewareAppOptions.Flow) //run rest service (infinitely)
                        webType = WebApplicationType.SERVLET;

                    measureTime("Configure Boot Environment");
                    //configure boot app
                    final SpringApplicationBuilder appBuilder = new SpringApplicationBuilder(SiriusMiddlewareApplication.class)
                            .web(webType)
                            .headless(webType.equals(WebApplicationType.NONE))
                            .bannerMode(Banner.Mode.OFF);

                    measureTime("Start Workflow");
                    SpringApplication app = appBuilder.application();
                    app.addListeners(new ApplicationPidFileWriter(Workspace.WORKSPACE.resolve("sirius.pid").toFile()));
                    app.addListeners(new WebServerPortFileWriter(Workspace.WORKSPACE.resolve("sirius.port").toFile()));
                    ConfigurableApplicationContext appContext = app.run(args);

                    //add default project to project service
                    //todo make generic that it works with arbitrary provider ore remove if not needed...
                    final SiriusProjectSpace ps = rootOptions.getProjectSpace().projectSpace();
                    ProjectInfo startPs = appContext
                            .getBean("projectsProvider", SiriusProjectSpaceProviderImpl.class)
                            .addProjectSpace(ps.getLocation().getFileName().toString(), ps);

                    measureTime("Workflow DONE!");
                    System.err.println("SIRIUS Service started successfully!");

                    if (middlewareOpts.isStartGui()) ((GuiService<?>)appContext.getBean("guiService"))
                            .createGuiInstance(startPs.getProjectId());
                } else {
                    System.exit(0);// Zero because this is the help message case
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            SiriusCLIApplication.main(args);
        }
    }


    @Override
    public void run(String... args) {
        RUN.compute();
    }

    @Override
    public void destroy() {
        System.out.println("SIRIUS context shutdown...");
        log.info("LOMBOK: SIRIUS is cleaning up threads and shuts down...");
        LoggerFactory.getLogger(getClass()).info("DEFAULT: SIRIUS is cleaning up threads and shuts down...");

        // ensure that token is not in bad state after shut down.
        try {
            AuthService as = ApplicationCore.WEB_API.getAuthService();
            if (as.isLoggedIn())
                AuthServices.writeRefreshToken(ApplicationCore.WEB_API.getAuthService(), ApplicationCore.TOKEN_FILE, true);
            else
                Files.deleteIfExists(ApplicationCore.TOKEN_FILE);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            ProxyManager.disconnect();
        }

        ApplicationCore.DEFAULT_LOGGER.info("CLI shut down hook: SIRIUS is cleaning up threads and shuts down...");
        try {
            if (RUN != null) {
                RUN.cancel();
            }
        } finally {
            if (successfulParsed && PropertyManager.DEFAULTS.createInstanceWithDefaults(PrintCitations.class).value)
                ApplicationCore.BIBTEX.citeToSystemErr();
        }

        System.out.println("SIRIUS context DONE");
    }
}