package de.unijena.bioinf.ms.cli;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;

public class SiriusApplication {
    public static void main(String[] args) throws InterruptedException {

        try {
            long t = System.currentTimeMillis();
            final ZodiacCLI<ZodiacOptions> cli = new ZodiacCLI<>();
            cli.parseArgsAndInit(args, ZodiacOptions.class);
            cli.compute();
            System.out.println("Time: " + ((double) (System.currentTimeMillis() - t)) / 1000d);
        } finally {
            SiriusJobs.getGlobalJobManager().shutdown();
        }
    }
}
