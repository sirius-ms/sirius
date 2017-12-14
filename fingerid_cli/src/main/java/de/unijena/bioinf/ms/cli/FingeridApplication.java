package de.unijena.bioinf.ms.cli;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;

public class FingeridApplication {
    public static void main(String[] args) throws InterruptedException {
        try {
            long t = System.currentTimeMillis();
            final FingeridCLI<FingerIdOptions> cli = new FingeridCLI<>();
            cli.parseArgsAndInit(args, FingerIdOptions.class);
            cli.compute();
            System.out.println("Time: " + ((double) (System.currentTimeMillis() - t)) / 1000d);
        } finally {
            SiriusJobs.getGlobalJobManager().shutdown();
        }
    }
}
