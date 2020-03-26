package de.unijena.bioinf.ms.frontend.subtools.middleware;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.ms.frontend.subtools.PreprocessingJob;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.frontend.subtools.StandaloneTool;
import de.unijena.bioinf.ms.frontend.workflow.ServiceWorkflow;
import de.unijena.bioinf.ms.middleware.SiriusMiddlewareApplication;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import picocli.CommandLine;

@CommandLine.Command(name = "asService", aliases = {"rest", "REST"}, description = "Starts SIRIUS as a background (REST) service that can be requested via a REST-API",  versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true)
public class MiddlewareAppOptions implements StandaloneTool<MiddlewareAppOptions.Flow> {

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
    public Flow makeWorkflow(RootOptions<?,?,?> rootOptions, ParameterConfig config) {
        return new Flow((RootOptions<ProjectSpaceManager, PreprocessingJob<ProjectSpaceManager>, ?>) rootOptions, config);

    }



    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public class Flow implements ServiceWorkflow {

        private final PreprocessingJob<ProjectSpaceManager> preproJob;
        private final ParameterConfig config;
        private ConfigurableApplicationContext appContext = null;


        private Flow(RootOptions<ProjectSpaceManager, PreprocessingJob<ProjectSpaceManager> ,?> opts, ParameterConfig config) {
            this.preproJob = opts.makeDefaultPreprocessingJob();
            this.config = config;
        }

        @Override
        public void run() {
            System.out.println(System.getProperty("management.endpoints.web.exposure.include"));
            //do the project importing from the commandline
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
