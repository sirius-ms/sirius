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

import com.brightgiant.jxsupport.JxSupport;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.auth.AuthService;
import de.unijena.bioinf.auth.AuthServices;
import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.jjobs.SwingJobManager;
import de.unijena.bioinf.ms.frontend.DefaultParameter;
import de.unijena.bioinf.ms.frontend.Run;
import de.unijena.bioinf.ms.frontend.SiriusCLIApplication;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.core.Workspace;
import de.unijena.bioinf.ms.frontend.splash.Splash;
import de.unijena.bioinf.ms.frontend.subtools.CLIRootOptions;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.frontend.subtools.middleware.MiddlewareAppOptions;
import de.unijena.bioinf.ms.frontend.workflow.InstanceBufferFactory;
import de.unijena.bioinf.ms.frontend.workflow.WorkflowBuilder;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.middleware.service.gui.GuiService;
import de.unijena.bioinf.ms.middleware.service.projects.ProjectsProvider;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.ProjectSpaceManagerFactory;
import io.sirius.ms.sdk.SiriusSDK;
import io.sirius.ms.sdk.model.GuiInfo;
import io.sirius.ms.sdk.model.ProjectInfo;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.ApplicationPidFileWriter;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.boot.web.server.PortInUseException;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.boot.web.server.context.WebServerPortFileWriter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ApplicationListener;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import picocli.CommandLine;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
@SpringBootApplication
@OpenAPIDefinition
@Slf4j
public class SiriusMiddlewareApplication extends SiriusCLIApplication implements CommandLineRunner, DisposableBean {

    private static final int PORT_IN_USE_EXIT_CODE = 10;

    /**
     * Hidden run mode that boots the full "rest -s --gui" startup, waits until the application is
     * fully initialized and then shuts down cleanly. Combined with a JVM launched with
     * {@code -XX:AOTCacheOutput=...} (as the SIRIUS launcher does on a cold cache) this records a
     * representative Project Leyden AOT cache for the user's exact environment.
     */
    private static final String AOT_TRAIN_COMMAND = "aot-train";
    public static final String AOT_TRAINING_PROPERTY = "de.unijena.bioinf.sirius.aot.training";

    private static MiddlewareAppOptions<?> middlewareOpts;
    private static CLIRootOptions rootOptions;

    private final ApplicationContext appContext;

    public SiriusMiddlewareApplication(ApplicationContext appContext) {
        this.appContext = appContext;
    }

    public static void main(String[] args) {
        System.setProperty("de.unijena.bioinf.sirius.springSupport", "true");

        // --- AOT BYPASS for build time scanning aot scanning ---
        if (Boolean.getBoolean("spring.aot.processing")) {
            System.setProperty("java.awt.headless", "true"); // Enforce headless just to be safe during build
            new SpringApplicationBuilder(SiriusMiddlewareApplication.class)
                    .web(WebApplicationType.SERVLET) // Ensures REST beans are processed!
                    .run(args);
            return;
        }

        // --- Project Leyden AOT training run ---
        // "aot-train" is a thin routing over the normal service startup: rewrite it to "rest -s"
        // (plus "--gui" only when NOT running headless) and flag training mode so we can register a
        // listener that shuts the app down once it is fully initialized. --gui and --headless are a
        // mutually exclusive picocli ArgGroup, so we must NOT add --gui when the caller asked for
        // headless training (e.g. the installer's SYSTEM-context run, which has no interactive
        // desktop) or when the environment itself is headless - otherwise arg parsing fails and no
        // real training happens.
        final boolean aotTraining = args != null && Arrays.stream(args).anyMatch(AOT_TRAIN_COMMAND::equalsIgnoreCase);
        if (aotTraining) {
            System.setProperty(AOT_TRAINING_PROPERTY, "true");
            boolean headlessTraining = (GraphicsEnvironment.isHeadless() || Arrays.stream(args).anyMatch("--headless"::equalsIgnoreCase))
                    && Arrays.stream(args).noneMatch("--no-headless"::equalsIgnoreCase);
            List<String> rewritten = new ArrayList<>(args.length + 2);
            for (String a : args) {
                if (AOT_TRAIN_COMMAND.equalsIgnoreCase(a)) {
                    rewritten.add("rest");
                    rewritten.add("-s");
                    if (!headlessTraining)
                        rewritten.add("--gui");
                } else {
                    rewritten.add(a);
                }
            }
            args = rewritten.toArray(new String[0]);
        }

        measureTime("Init Job Manager");
        // The spring app classloader seems not to be correctly inherited to sub thread
        // So we need to ensure that the apache.configuration2 libs gets access otherwise.
        boolean headless = (GraphicsEnvironment.isHeadless() || Arrays.stream(args).anyMatch("--headless"::equalsIgnoreCase))
                && Arrays.stream(args).noneMatch("--no-headless"::equalsIgnoreCase); //enforce headless of in case detection does detect false positive
        Properties baseProperties = new Properties();
        baseProperties.put("de.unijena.bioinf.sirius.headless", String.valueOf(headless));

        if (headless) {
            System.err.println("SIRIUS is running in headless mode. GUI feature are not available!");
            // use non-swing is to prevent errors on headless systems
            SiriusJobs.setJobManagerFactory((cpuThreads) -> new JobManager(
                    cpuThreads,
                    Thread.currentThread().getContextClassLoader()
            ));
        } else {
            JxSupport.activate();
            // SwingJobManager is needed to show loading screens in GUI
            SiriusJobs.setJobManagerFactory((cpuThreads) -> new SwingJobManager(
                    cpuThreads,
                    Thread.currentThread().getContextClassLoader()
            ));
            //start gui as default if no command is given and headless mode is not enabled
            if (args == null || args.length == 0)
                args = new String[]{"rest", "-s", "--gui"};
        }


        //hacky shortcut to print help fast without loading spring!
        if (args.length == 0 || Arrays.stream(args).anyMatch(it ->
                it.equalsIgnoreCase("-h") || it.equalsIgnoreCase("--help")
                || it.equalsIgnoreCase("--h") || it.equalsIgnoreCase("-help")
        )) {
            try {
                rootOptions = new CLIRootOptions(new DefaultParameterConfigLoader(), null);
                if (args.length < 1)
                    args = new String[]{"--help"};

                WorkflowBuilder builder = new WorkflowBuilder(rootOptions, List.of(new MiddlewareAppOptions<>(null)));
                builder.initRootSpec();
                final CommandLine commandline = new CommandLine(builder.getRootSpec());
                commandline.setUnmatchedArgumentsAllowed(true);
                commandline.setCaseInsensitiveEnumValuesAllowed(true);
                commandline.registerConverter(DefaultParameter.class, new DefaultParameter.Converter());
                CommandLine.ParseResult parseResult = commandline.parseArgs(args);
                CommandLine.printHelpIfRequested(parseResult);
                System.exit(0);
            } catch (Exception e) {
                log.error("Error printing help message", e);
                System.exit(1);
            }
            //check if service mode is used before command line is really parsed to decide whether we need to
            //configure a spring app or not.
        } else if (Arrays.stream(args).anyMatch(it ->
                it.equalsIgnoreCase(MiddlewareAppOptions.class.getAnnotation(CommandLine.Command.class).name())
                        || Arrays.stream(MiddlewareAppOptions.class.getAnnotation(CommandLine.Command.class).aliases())
                        .anyMatch(cmd -> cmd.equalsIgnoreCase(it))
        )) {
            try {
                final boolean startGui = !headless && Arrays.stream(args).anyMatch(it -> it.equalsIgnoreCase("--gui") || it.equalsIgnoreCase("-g"));

                //check for existing sirius instances.
                //Skipped in AOT training mode: we always want to boot our own context (on an
                //autodetected port) so the training run can record and dump the AOT cache.
                try (SiriusSDK sdk = aotTraining ? null : SiriusSDK.findAndConnectLocally(SiriusSDK.ShutdownMode.NEVER, true)) {
                    if (sdk != null) {
                        if (startGui) {
                            try {
                                List<GuiInfo> guis = sdk.gui().getGuis();// just to check for headless mode or other gui issue before creating project.
                                log.info("SIRIUS ALREADY RUNNING!\nHealthy SIRIUS instance is running at port {}.", sdk.getBasePath());
                                ProjectInfo pInfo = sdk.projects().create(null);
                                log.info("Starting new gui instance for tmp project '{}' using the running instance.", pInfo.getLocation());
                                sdk.gui().openGui(pInfo.getProjectId());
                                System.exit(0);
                            } catch (WebClientResponseException e) {
                                log.error("SIRIUS ALREADY RUNNING!\nHealthy SIRIUS instance is running at '{}' with process id '{}' but failed to start GUI. Maybe instance is running in headless mode!. Shutdown existing instance and try again. Details: {}", sdk.getBasePath(), sdk.getPID(), e.getMessage());
                                System.exit(1);
                            }
                        } else {
                            log.error("SIRIUS ALREADY RUNNING!\nHealthy SIRIUS instance is running at '{}' with process id '{}'. Multiple SIRIUS services are not allowed. Use the running one instead or shut it down before restarting!", sdk.getBasePath(), sdk.getPID());
                            System.exit(1);
                        }
                    }
                }

                System.setProperty(APP_TYPE_PROPERTY_KEY, "SERVICE");


                ApplicationCore.DEFAULT_LOGGER.info("Starting Application Core");
                PropertyManager.setProperty("de.unijena.bioinf.sirius.BackgroundRuns.autoremove", "false");
                // Finds directory where the sirius executable/startscript is located and adds it to properties.
                // On windows this directory corresponds to the sirius base/home directory.
                // On linux and mac the executables are located in subdirectories.
                // Keep this in mind when using this property.
                PropertyManager.setProperty("io.sirius-ms.exeDir", Path.of(new ApplicationHome().getDir().getAbsolutePath()).toString());
                // remove old pid and port file as early as possible
                Files.deleteIfExists(Workspace.PORT_FILE);
                Files.deleteIfExists(Workspace.PID_FILE);

                Splash splashScreen = null;
                if (!headless) { // ignore gui option if headless is enabled
                    if (Arrays.stream(args).anyMatch(it -> it.equalsIgnoreCase("--gui") || it.equalsIgnoreCase("-g"))) {
                        GuiUtils.initUI();
                        // During AOT training use the fake (invisible) splash: it still initializes the
                        // AWT framework (needed for jxbrowser) but keeps the training-mode overlay the
                        // single visible indicator instead of flashing the normal splash too.
                        splashScreen = new Splash(true, aotTraining);
                    } else {
                        splashScreen = new Splash(true, true);
                        Jobs.runEDTLater(splashScreen::dispose);
                    }
                }


                //parse args before spring app starts so we can manipulate app behaviour via command line
                //Init without space factory, will be added later when spring is running.
                middlewareOpts = new MiddlewareAppOptions<>(splashScreen);
                rootOptions = new CLIRootOptions(new DefaultParameterConfigLoader(), null);
                measureTime("init Run");
                RUN = new Run(new WorkflowBuilder(rootOptions, List.of(middlewareOpts)), false);
                measureTime("Start Parse args");
                RUN.parseArgs(args);
                measureTime("Parse args Done");


                // decides whether the app runs infinitely
                WebApplicationType webType = WebApplicationType.SERVLET;
                measureTime("Configure Boot Environment");
                //configure boot app
                final SpringApplicationBuilder appBuilder = new SpringApplicationBuilder(SiriusMiddlewareApplication.class)
                        .properties(baseProperties)
                        .web(webType)
                        .headless(splashScreen == null)
                        .bannerMode(Banner.Mode.OFF);

                measureTime("Start Workflow");
                SpringApplication app = appBuilder.application();
                app.addListeners((ApplicationListener<WebServerInitializedEvent>) event -> {
                    log.info("SIRIUS Service is running on port: {}", event.getWebServer().getPort());
                    log.info("SIRIUS Service started successfully!");
                });
                app.addListeners(new ApplicationPidFileWriter(Workspace.PID_FILE.toFile()));
                app.addListeners(new WebServerPortFileWriter(Workspace.PORT_FILE.toFile()));

                if (aotTraining) {
                    log.info("Starting SIRIUS in Project Leyden AOT training mode. " +
                            "The application will shut down automatically once fully initialized.");
                    app.addListeners(new AotTrainingShutdownListener());
                }

                app.run(args);
            } catch (ApplicationContextException springException) {
                log.error("Spring error", springException);
                if (springException.getCause() instanceof PortInUseException) {
                    System.exit(PORT_IN_USE_EXIT_CODE);
                }
                System.exit(1);
            } catch (Exception e) {
                log.error("Error starting service", e);
                System.exit(1);
            }
        } else {
            SiriusCLIApplication.runMain(args, List.of());
        }
    }


    @Override
    public void run(String... args) {
        middlewareOpts.setProjectsProvider(appContext.getBean(ProjectsProvider.class));
        if (appContext.containsBean("guiService"))
            middlewareOpts.setGuiService(appContext.getBean(GuiService.class));
        rootOptions.setSpaceManagerFactory(appContext.getBean(ProjectSpaceManagerFactory.class));

        successfulParsed = RUN.makeWorkflow(appContext.getBean(InstanceBufferFactory.class)) != null;
        measureTime("Parse args Done!");

        if (successfulParsed) {
            RUN.compute();
        } else {
            System.exit(0);
        }
    }

    @Override
    public void destroy() {
        log.info("SIRIUS is cleaning up threads and shuts down...");
        // ensure that token is not in bad state after shut down.
        try {
            AuthService as = ApplicationCore.WEB_API().getAuthService();
            if (as.isLoggedIn())
                AuthServices.writeRefreshToken(as, ApplicationCore.TOKEN_FILE, true);
            else
                Files.deleteIfExists(ApplicationCore.TOKEN_FILE);
        } catch (IOException e) {
            log.error("Error in clean up", e);
        }

        ApplicationCore.DEFAULT_LOGGER.info("CLI shut down hook: SIRIUS is cleaning up threads and shuts down...");
        try {
            if (RUN != null) {
                RUN.cancel();
            }

            if (!GraphicsEnvironment.isHeadless()) {
                for (Window window : Window.getWindows()) {
                    window.dispose();
                }
            }
        } finally {
            if (successfulParsed && PropertyManager.getBoolean("de.unijena.bioinf.sirius.printCitations", true))
                ApplicationCore.BIBTEX.citeToSystemErr();
        }
    }
}