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
import de.unijena.bioinf.ms.middleware.service.gui.GuiService;
import de.unijena.bioinf.ms.middleware.service.projects.ProjectsProvider;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.ProjectSpaceManagerFactory;
import de.unijena.bioinf.rest.ProxyManager;
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
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.boot.web.context.WebServerPortFileWriter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
@SpringBootApplication
@OpenAPIDefinition
@Slf4j
public class SiriusMiddlewareApplication extends SiriusCLIApplication implements CommandLineRunner, DisposableBean {
    private static MiddlewareAppOptions<?> middlewareOpts;
    private static CLIRootOptions rootOptions;

    private final ApplicationContext appContext;

    public SiriusMiddlewareApplication(ApplicationContext appContext) {
        this.appContext = appContext;
    }

    public static void main(String[] args) {
        System.setProperty("de.unijena.bioinf.sirius.springSupport", "true");
        measureTime("Init Swing Job Manager");
        // The spring app classloader seems not to be correctly inherited to sub thread
        // So we need to ensure that the apache.configuration2 libs gets access otherwise.
        // SwingJobManager is needed to show loading screens in GUI
        SiriusJobs.setJobManagerFactory((cpuThreads) -> new SwingJobManager(
                cpuThreads,
                2,
                Thread.currentThread().getContextClassLoader()
        ));

        //start gui as default if no command is given
        if (args == null || args.length == 0)
            args = new String[]{"rest", "-s", "--gui"};

        //check if service mode is used before command line is really parsed to decide whether we need to
        //configure a spring app or not.
        if (Arrays.stream(args).anyMatch(it ->
                it.equalsIgnoreCase(MiddlewareAppOptions.class.getAnnotation(CommandLine.Command.class).name())
                        || Arrays.stream(MiddlewareAppOptions.class.getAnnotation(CommandLine.Command.class).aliases())
                        .anyMatch(cmd -> cmd.equalsIgnoreCase(it))
                        || it.equalsIgnoreCase("-h") || it.equalsIgnoreCase("--help") // just to get Middleware help.
        )) {
            try {

                System.setProperty(APP_TYPE_PROPERTY_KEY, "SERVICE");


                ApplicationCore.DEFAULT_LOGGER.info("Starting Application Core");
                PropertyManager.setProperty("de.unijena.bioinf.sirius.BackgroundRuns.autoremove", "false");
                //just store the sirius base dir
                PropertyManager.setProperty("de.unijena.bioinf.sirius.homeDir", Path.of(new ApplicationHome().getDir().getAbsolutePath()).getParent().toString());

                Splash splashScreen = null;
                if (Arrays.stream(args).anyMatch(it -> it.equalsIgnoreCase("--gui") || it.equalsIgnoreCase("-g"))) {
                    Path propsFile = Workspace.siriusPropsFile;
                    //override VM defaults from OS
                    if (!System.getProperties().containsKey("sun.java2d.uiScale"))
                        System.setProperty("sun.java2d.uiScale", "1");
                    //override with stored value if available
                    if (Files.exists(propsFile)) {
                        Properties props = new Properties();
                        try (BufferedReader r = Files.newBufferedReader(propsFile)) {
                            props.load(r);
                            if (props.containsKey("sun.java2d.uiScale"))
                                System.setProperty("sun.java2d.uiScale", props.getProperty("sun.java2d.uiScale"));
                        } catch (IOException e) {
                            log.error("Error when initializing Splash.", e);
                        }
                    }
                    splashScreen = new Splash(true);
                }


                //parse args before spring app starts so we can manipulate app behaviour via command line
                //Init without space factory, will be added later when spring is running.
                middlewareOpts = new MiddlewareAppOptions<>(splashScreen);
                rootOptions = new CLIRootOptions(new DefaultParameterConfigLoader(), null);
                measureTime("init Run");
                RUN = new Run(new WorkflowBuilder(rootOptions, List.of(middlewareOpts)));
                measureTime("Start Parse args");
                RUN.parseArgs(args);
                measureTime("Parse args Done");


                // decides whether the app runs infinitely
                WebApplicationType webType = WebApplicationType.SERVLET;
                measureTime("Configure Boot Environment");
                //configure boot app
                final SpringApplicationBuilder appBuilder = new SpringApplicationBuilder(SiriusMiddlewareApplication.class)
                        .web(webType)
                        .headless(webType.equals(WebApplicationType.NONE))
                        .bannerMode(Banner.Mode.OFF);

                measureTime("Start Workflow");
                SpringApplication app = appBuilder.application();
                app.addListeners((ApplicationListener<WebServerInitializedEvent>) event ->{
                        System.err.println("SIRIUS Service is running on port: " + event.getWebServer().getPort());
                        System.err.println("SIRIUS Service started successfully!");
                });
                app.addListeners(new ApplicationPidFileWriter(Workspace.WORKSPACE.resolve("sirius.pid").toFile()));
                app.addListeners(new WebServerPortFileWriter(Workspace.WORKSPACE.resolve("sirius.port").toFile()));

                app.run(args);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);// Zero because this is the help message case
            }
        } else {
            SiriusCLIApplication.runMain(args, List.of());
        }
    }


    @Override
    public void run(String... args) {
        middlewareOpts.setProjectsProvider(appContext.getBean(ProjectsProvider.class));
        middlewareOpts.setGuiService(appContext.getBean(GuiService.class));
        rootOptions.setSpaceManagerFactory(appContext.getBean(ProjectSpaceManagerFactory.class));

        successfulParsed = RUN.makeWorkflow(appContext.getBean(InstanceBufferFactory.class)) != null;
        measureTime("Parse args Done!");

        if (successfulParsed) {
            RUN.compute();
        } else {
            System.exit(0);// Zero because this is the help message case
        }
    }

    @Override
    public void destroy() {
        log.info("SIRIUS is cleaning up threads and shuts down...");
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
    }
}