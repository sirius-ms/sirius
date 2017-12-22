package de.unijena.bioinf.ms.cli;

import de.unijena.bioinf.jjobs.JobManager;
import org.slf4j.LoggerFactory;

public class SiriusApplication {
    public static void main(String[] args) {
        long t = System.currentTimeMillis();
        try {
            final ZodiacCLI<ZodiacOptions> cli = new ZodiacCLI<>();
            cli.parseArgsAndInit(args, ZodiacOptions.class);
            cli.compute();
        } catch (Exception e) {
            LoggerFactory.getLogger(SiriusApplication.class).error("Unkown Error!", e);
        } finally {
            System.err.println("Time: " + ((double) (System.currentTimeMillis() - t)) / 1000d);
            try {
                JobManager.shutDownAllInstances();
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.exit(0);
            }
        }
    }
}
