package de.unijena.bioinf.ms.frontend;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.jjobs.SwingJobManager;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.projectspace.GPSMFactory;
import de.unijena.bioinf.ms.frontend.subtools.CLIRootOptions;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.frontend.workflow.ServiceWorkflow;
import de.unijena.bioinf.ms.frontend.workfow.GuiWorkflowBuilder;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.middleware.SiriusContext;
import de.unijena.bioinf.ms.middleware.SiriusMiddlewareApplication;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */

@SpringBootApplication
public class SiriusGUIApplication extends SiriusMiddlewareApplication {

    public SiriusGUIApplication(SiriusContext context) {
        super(context);
    }

    public static void main(String[] args) {
        configureShutDownHook(()->{
            Jobs.cancelALL();//cancel all instances to quit
        });

        ApplicationCore.DEFAULT_LOGGER.info("Application Core started");
        final int cpuThreads = Integer.valueOf(PropertyManager.getProperty("de.unijena.bioinf.sirius.cpu.cores", null, "1"));
        SiriusJobs.setGlobalJobManager(new SwingJobManager(PropertyManager.getNumberOfThreads(), Math.min(cpuThreads, 3)));
        ApplicationCore.DEFAULT_LOGGER.info("Swing Job MANAGER initialized! " + SiriusJobs.getGlobalJobManager().getCPUThreads() + " : " + SiriusJobs.getGlobalJobManager().getIOThreads());

        //todo why do we need this?
//        if (ProxyManager.getProxyStrategy() == null)
//            SiriusProperties.SIRIUS_PROPERTIES_FILE().setAndStoreProperty("de.unijena.bioinf.sirius.proxy", ProxyManager.DEFAULT_STRATEGY.name());

        run(args, () -> {
            final DefaultParameterConfigLoader configOptionLoader = new DefaultParameterConfigLoader();
            rootOptions = new CLIRootOptions(configOptionLoader, new GPSMFactory());
            return new GuiWorkflowBuilder<>(rootOptions, configOptionLoader);
        });

        if (!(RUN.getFlow() instanceof ServiceWorkflow)) {
            System.exit(0);
        }
    }
}
