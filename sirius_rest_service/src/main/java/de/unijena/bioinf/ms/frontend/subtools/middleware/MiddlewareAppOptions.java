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

import java.util.Collections;

@CommandLine.Command(name = "asService", aliases = {"REST"}, description = "Starts SIRIUS as a background service that can be requested via a REST-API", defaultValueProvider = Provide.Defaults.class, versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true)
public class MiddlewareAppOptions implements SingletonTool {

    @CommandLine.Option(names = {"--port", "-p"}, description = "Specify the port on which the SIRIUS REST Service should run (Default: 8080).", defaultValue = "8080")
    private int port = 8080;

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
            SiriusJobs.getGlobalJobManager().submitJob(preproJob).takeResult();
            SpringApplication app = new SpringApplication(SiriusMiddlewareApplication.class);
            app.setDefaultProperties(Collections
                    .singletonMap("server.port", String.valueOf(port)));
            appContext = app.run();
        }

        @Override
        public void cancel() {
            if (appContext != null)
                appContext.close();
        }
    }
}
