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

@CommandLine.Command(name = "asService", aliases = {"REST"}, description = "Starts the graphical user interface of SIRIUS", defaultValueProvider = Provide.Defaults.class, versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true)
public class MiddlewareAppOptions implements SingletonTool {


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
            appContext = app.run();
        }

        @Override
        public void cancel() {
            if (appContext != null)
                appContext.close();
        }
    }
}
