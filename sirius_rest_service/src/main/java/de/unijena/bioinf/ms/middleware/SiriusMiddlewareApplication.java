package de.unijena.bioinf.ms.middleware;

import de.unijena.bioinf.ms.frontend.SiriusCLIApplication;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.io.projectspace.ProjectSpaceManagerFactory;
import de.unijena.bioinf.ms.frontend.subtools.CLIRootOptions;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.frontend.workflow.ServiceWorkflow;
import de.unijena.bioinf.ms.frontend.workfow.MiddlewareWorkflowBuilder;
import de.unijena.bioinf.projectspace.SiriusProjectSpace;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;

@SpringBootApplication
public class SiriusMiddlewareApplication extends SiriusCLIApplication implements CommandLineRunner {


    protected static CLIRootOptions rootOptions;
    protected final SiriusContext context;

    public SiriusMiddlewareApplication(SiriusContext context) {
        this.context = context;
    }

    public static void main(String[] args) {
        ApplicationCore.DEFAULT_LOGGER.info("Init AppCore");
        try {
            configureShutDownHook(() -> {
            });

            final DefaultParameterConfigLoader configOptionLoader = new DefaultParameterConfigLoader();
            rootOptions = new CLIRootOptions(configOptionLoader, new ProjectSpaceManagerFactory.Default());
            run(args, () -> new MiddlewareWorkflowBuilder<>(rootOptions, configOptionLoader));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (!(RUN.getFlow() instanceof ServiceWorkflow)) {
                System.exit(0);
            }
        }
    }


    @Override
    public void run(String... args) throws Exception {
        final SiriusProjectSpace ps = rootOptions.getProjectSpace().projectSpace();
        context.addProjectSpace(ps.getLocation().getFileName().toString(), ps);
    }
}