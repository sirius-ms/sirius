package de.unijena.bioinf.ms.frontend;

import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.net.ProxyManager;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutionException;


public class SiriusCLIApplication {
    public static void main(String[] args) {
        //shut down hook to clean up when sirius is shutting down
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ApplicationCore.DEFAULT_LOGGER.info("CLI shut down hook: SIRIUS is cleaning up threads and shuts down...");
            try {
                JobManager.shutDownNowAllInstances();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                ProxyManager.disconnect();
            }
        }));

        try {
            run(args);
        } catch (Exception e) {
            LoggerFactory.getLogger(SiriusCLIApplication.class).error("Unknown Error!", e);
        } finally {
            ApplicationCore.cite();
            System.exit(0);
        }
    }

    public static void run(String[] args) throws IOException, ExecutionException {
        CLIRun cliRun = new CLIRun();
        cliRun.parseArgs(args);
        cliRun.compute();
        System.out.println("DONE!");
    }
}
