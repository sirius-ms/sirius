package de.unijena.bioinf.ms.middleware;

import de.unijena.bioinf.ms.frontend.Run;
import de.unijena.bioinf.ms.frontend.SiriusCLIApplication;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.CLIRootOptions;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.frontend.subtools.middleware.MiddlewareAppOptions;
import de.unijena.bioinf.ms.frontend.workflow.SimpleInstanceBuffer;
import de.unijena.bioinf.ms.frontend.workfow.MiddlewareWorkflowBuilder;
import de.unijena.bioinf.projectspace.ProjectSpaceManagerFactory;
import de.unijena.bioinf.projectspace.SiriusProjectSpace;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;

@SpringBootApplication
public class SiriusMiddlewareApplication extends SiriusCLIApplication implements CommandLineRunner {


    protected static CLIRootOptions rootOptions;
    protected final SiriusContext context;
    protected static ConfigurableApplicationContext appContext = null;

    public SiriusMiddlewareApplication(SiriusContext context) {
        this.context = context;
    }

    public static void main(String[] args) {
        ApplicationCore.DEFAULT_LOGGER.info("Init AppCore");
        try {
            configureShutDownHook(shutdownWebservice());
            final DefaultParameterConfigLoader configOptionLoader = new DefaultParameterConfigLoader();
            rootOptions = new CLIRootOptions<>(configOptionLoader, new ProjectSpaceManagerFactory.Default());
            if (RUN != null)
                throw new IllegalStateException("Application can only run Once!");
            measureTime("init Run");
            RUN = new Run(new MiddlewareWorkflowBuilder<>(rootOptions, configOptionLoader, new SimpleInstanceBuffer.Factory()));
            measureTime("Start Parse args");
            boolean b = RUN.parseArgs(args);
            measureTime("Parse args Done!");
            if (b) {
                WebApplicationType webType = WebApplicationType.NONE;
                if (RUN.getFlow() instanceof MiddlewareAppOptions.Flow) //run rest service
                    webType = WebApplicationType.SERVLET;
                measureTime("Configure Boot Environment");
                //configure boot app
                final SpringApplicationBuilder appBuilder = new SpringApplicationBuilder(SiriusMiddlewareApplication.class)
                        .web(webType)
                        .headless(true)
                        .bannerMode(Banner.Mode.OFF);
                measureTime("Start Workflow");
                appContext = appBuilder.run(args);

                measureTime("Workflow DONE!");
            }
        } catch (IOException e) {
            e.printStackTrace();
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