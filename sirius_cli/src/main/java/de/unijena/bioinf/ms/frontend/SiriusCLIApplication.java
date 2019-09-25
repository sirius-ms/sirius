package de.unijena.bioinf.ms.frontend;

import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.net.ProxyManager;
import org.slf4j.LoggerFactory;

import java.io.IOException;


public class SiriusCLIApplication {
    private static CLIRun RUN = null;

    public static void main(String[] args) {

        //shut down hook to clean up when sirius is shutting down
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ApplicationCore.DEFAULT_LOGGER.info("CLI shut down hook: SIRIUS is cleaning up threads and shuts down...");
            try {
                if (SiriusCLIApplication.RUN != null)
                    SiriusCLIApplication.RUN.cancel();
                JobManager.shutDownNowAllInstances();
                ApplicationCore.WEB_API.unregisterClientAndDeleteJobsFromServer();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                ProxyManager.disconnect();
            }
        }));

        try {
            if (RUN != null)
                throw new IllegalStateException("Aplication can only run Once!");
            RUN = run(args);
        } catch (Throwable e) {
            LoggerFactory.getLogger(SiriusCLIApplication.class).error("Unexpected Error!", e);
        } finally {
            ApplicationCore.cite();
            System.exit(0);
        }
    }

    public static CLIRun run(String[] args) throws IOException {
        CLIRun cliRun = new CLIRun();
        if (cliRun.parseArgs(args)) {
            cliRun.compute();
        }
        return cliRun;
    }
}
