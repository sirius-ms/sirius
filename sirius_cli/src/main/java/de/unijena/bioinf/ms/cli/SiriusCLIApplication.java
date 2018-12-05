package de.unijena.bioinf.ms.cli;

import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.sirius.core.ApplicationCore;
import de.unijena.bioinf.sirius.net.ProxyManager;
import org.slf4j.LoggerFactory;


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
            CombinedCLI combinedCLI = new CombinedCLI();
//            combinedCLI.parseArgsAndInit(args);
//            combinedCLI.compute();
        } catch (Exception e) {
            LoggerFactory.getLogger(SiriusCLIApplication.class).error("Unkown Error!", e);
        } finally {
            System.exit(0);
        }
    }
}
