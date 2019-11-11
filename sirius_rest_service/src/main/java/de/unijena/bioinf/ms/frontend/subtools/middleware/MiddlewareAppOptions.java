package de.unijena.bioinf.ms.frontend.subtools.middleware;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ms.frontend.io.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.ms.frontend.subtools.PreprocessingJob;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.SingletonTool;
import de.unijena.bioinf.ms.frontend.workflow.ServiceWorkflow;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.middleware.SiriusMiddlewareApplication;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import picocli.CommandLine;

@CommandLine.Command(name = "asService", aliases = {"rest", "REST"}, description = "Starts SIRIUS as a background service that can be requested via a REST-API", defaultValueProvider = Provide.Defaults.class, versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true)
public class MiddlewareAppOptions implements SingletonTool {

    @CommandLine.Option(names = {"--port", "-p"}, description = "Specify the port on which the SIRIUS REST Service should run (Default: 8080).", defaultValue = "8080")
    private void setPort(int port) {
        System.getProperties().setProperty("server.port", String.valueOf(port));
    }

    @CommandLine.Option(names = {"--enable-rest-shutdown", "-s"}, description = "Allows to shut down the rest service via rest api call (/actuator/shutdown)", defaultValue = "false")
    private void setShutdown(boolean enableRestShutdown) {
        if (enableRestShutdown)
            System.getProperties().setProperty("management.endpoints.web.exposure.include", "info,health,shutdown");
        else
            System.getProperties().setProperty("management.endpoints.web.exposure.include", "info,health");

    }

    @Override
    public Workflow makeSingletonWorkflow(PreprocessingJob preproJob, ProjectSpaceManager projectSpace, ParameterConfig config) {
        return new Flow(preproJob, projectSpace, config);
    }


    private class Flow implements ServiceWorkflow {

        private final PreprocessingJob preproJob;
        private final ParameterConfig config;
        private final ProjectSpaceManager projecSapce;
        private ConfigurableApplicationContext appContext = null;


        public Flow(PreprocessingJob preproJob, ProjectSpaceManager projectSpace, ParameterConfig config) {
            this.preproJob = preproJob;
            this.projecSapce = projectSpace;
            this.config = config;
        }

        @Override
        public void run() {
            System.out.println(System.getProperty("management.endpoints.web.exposure.include"));
            SiriusJobs.getGlobalJobManager().submitJob(preproJob).takeResult();
            SpringApplication app = new SpringApplication(SiriusMiddlewareApplication.class);
            appContext = app.run();
        }

        @Override
        public void cancel() {
            if (appContext != null)
                appContext.registerShutdownHook();
        }
    }
}
