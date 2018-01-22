package de.unijena.bioinf.ms.cli;

import de.unijena.bioinf.jjobs.JobManager;
import org.slf4j.LoggerFactory;

public class SiriusCLIApplication {
    public static void main(String[] args) throws InterruptedException {
        long t = System.currentTimeMillis();
        try {
            final FingeridCLI<FingerIdOptions> cli = new FingeridCLI<>();
            cli.parseArgsAndInit(args, FingerIdOptions.class);
            cli.compute();
        } catch (Exception e) {
            LoggerFactory.getLogger(SiriusCLIApplication.class).error("Unkown Error!", e);
        } finally {
            System.out.println("Time: " + ((double) (System.currentTimeMillis() - t)) / 1000d);
            try {
                JobManager.shutDownAllInstances();
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.exit(0);
            }
        }
    }
}
