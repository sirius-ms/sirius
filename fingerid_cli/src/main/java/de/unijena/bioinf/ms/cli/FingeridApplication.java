package de.unijena.bioinf.ms.cli;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;

public class FingeridApplication {
    public static void main(String[] args) throws InterruptedException {
        try {
            final FingeridCLI<FingerIdOptions> cli = new FingeridCLI<>();
            cli.parseArgsAndInit(args, FingerIdOptions.class);
            cli.compute();
        } finally {
            SiriusJobs.getGlobalJobManager().shutdown();
        }
    }
}
