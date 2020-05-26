package de.unijena.bioinf.ms.frontend;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.jjobs.SwingJobManager;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.CLIRootOptions;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.frontend.subtools.gui.GuiAppOptions;
import de.unijena.bioinf.ms.frontend.subtools.middleware.MiddlewareAppOptions;
import de.unijena.bioinf.ms.frontend.workfow.GuiInstanceBufferFactory;
import de.unijena.bioinf.ms.frontend.workfow.GuiWorkflowBuilder;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.middleware.SiriusContext;
import de.unijena.bioinf.ms.middleware.SiriusMiddlewareApplication;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.GuiProjectSpaceManagerFactory;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */

@SpringBootApplication
public class SiriusGUIApplication extends SiriusMiddlewareApplication {

    public SiriusGUIApplication(SiriusContext context) {
        super(context);
    }

    public static void main(String[] args) {
        ApplicationCore.DEFAULT_LOGGER.info("Init AppCore");


        try {
            final int cpuThreads = Integer.valueOf(PropertyManager.getProperty("de.unijena.bioinf.sirius.cpu.cores", null, "1"));
            SiriusJobs.setGlobalJobManager(new SwingJobManager(Math.min(defaultThreadNumber(), cpuThreads), 1));
            ApplicationCore.DEFAULT_LOGGER.info("Swing Job MANAGER initialized! " + SiriusJobs.getGlobalJobManager().getCPUThreads() + " : " + SiriusJobs.getGlobalJobManager().getIOThreads());

            configureShutDownHook(() -> {
                Jobs.cancelALL();
                if (appContext != null)//todo maybe not needed because spring is doing this already
                    appContext.registerShutdownHook();
                shutdownWebservice();
            });

            final DefaultParameterConfigLoader configOptionLoader = new DefaultParameterConfigLoader();
            rootOptions = new CLIRootOptions<>(configOptionLoader, new GuiProjectSpaceManagerFactory());

            if (RUN != null)
                throw new IllegalStateException("Application can only run Once!");
            measureTime("init Run");
            RUN = new Run(new GuiWorkflowBuilder<>(rootOptions, configOptionLoader, new GuiInstanceBufferFactory()));
            measureTime("Start Parse args");
            boolean b = RUN.parseArgs(args);
            measureTime("Parse args Done!");
            //att some point it would be great to use headless mode for headless apps
            if (b) {
                measureTime("Configure Boot Environment");
                //configure boot app
                final SpringApplicationBuilder appBuilder = new SpringApplicationBuilder(SiriusGUIApplication.class)
                        .web(WebApplicationType.NONE)
                        .headless(false)
                        .bannerMode(Banner.Mode.OFF);
                measureTime("Start Workflow");
                if (RUN.getFlow() instanceof MiddlewareAppOptions.Flow) {//run rest service
//                    System.out.println(System.getProperty("management.endpoints.web.exposure.include"));
                  appContext = appBuilder.web(WebApplicationType.SERVLET).run(args);
                }else if (RUN.getFlow() instanceof GuiAppOptions.Flow){
                    appContext = appBuilder.run(args);
                }else {
                    appContext = appBuilder.run(args);
                    appContext.close();
                    System.exit(0);
                }
                measureTime("Workflow DONE!");
            } else {
                System.exit(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    public static int defaultThreadNumber(){
        int threadsAv = PropertyManager.getNumberOfThreads();
        return Math.max(1, threadsAv <= 8 ? threadsAv - 2 : threadsAv - threadsAv / 20);
    }
}
