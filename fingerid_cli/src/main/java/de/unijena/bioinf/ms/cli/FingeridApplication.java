package de.unijena.bioinf.ms.cli;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import org.slf4j.LoggerFactory;

public class FingeridApplication {
    public static void main(String[] args) throws InterruptedException {
        long t = System.currentTimeMillis();
        try {
            final FingeridCLI<FingerIdOptions> cli = new FingeridCLI<>();
            cli.parseArgsAndInit(args, FingerIdOptions.class);
            cli.compute();
        } catch (Exception e) {
            LoggerFactory.getLogger(SiriusApplication.class).error("Unkown Error!", e);
        } finally {
            System.out.println("Time: " + ((double) (System.currentTimeMillis() - t)) / 1000d);
            SiriusJobs.getGlobalJobManager().shutdown();
        }
    }
}
