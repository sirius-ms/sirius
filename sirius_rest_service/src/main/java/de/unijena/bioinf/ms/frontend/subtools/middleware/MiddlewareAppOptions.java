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

@CommandLine.Command(name = "asService", aliases = {"rest", "REST"}, description = "Starts SIRIUS as a background (REST) service that can be requested via a REST-API", defaultValueProvider = Provide.Defaults.class, versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true)
public class MiddlewareAppOptions implements SingletonTool<MiddlewareAppOptions.Flow> {

    @CommandLine.Option(names = {"--port", "-p"}, description = "Specify the port on which the SIRIUS REST Service should run (Default: 8080).", defaultValue = "8080")
    private void setPort(int port) {
        System.getProperties().setProperty("server.port", String.valueOf(port));
    }

    @CommandLine.Option(names = {"--enable-rest-shutdown", "-s"}, description = "Allows to shut down the SIRIUS REST Service via a rest api call (/actuator/shutdown)", defaultValue = "false")
    private void setShutdown(boolean enableRestShutdown) {
        if (enableRestShutdown)
            System.getProperties().setProperty("management.endpoints.web.exposure.include", "info,health,shutdown");
        else
            System.getProperties().setProperty("management.endpoints.web.exposure.include", "info,health");

    }

    @Override
    public Flow makeSingletonWorkflow(PreprocessingJob<?> preproJob, ParameterConfig config) {
        return new Flow((PreprocessingJob<ProjectSpaceManager>) preproJob, config);
    }


    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public class Flow implements ServiceWorkflow {

        private final PreprocessingJob<ProjectSpaceManager> preproJob;
        private final ParameterConfig config;
        private ConfigurableApplicationContext appContext = null;


        private Flow(PreprocessingJob<ProjectSpaceManager> preproJob, ParameterConfig config) {
            this.preproJob = preproJob;
            this.config = config;
        }

        @Override
        public void run() {
            System.out.println(System.getProperty("management.endpoints.web.exposure.include"));
            //todo needed
            final ProjectSpaceManager projectSapace = SiriusJobs.getGlobalJobManager().submitJob(preproJob).takeResult();
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
