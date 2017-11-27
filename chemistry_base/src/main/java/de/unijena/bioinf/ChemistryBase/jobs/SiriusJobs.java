package de.unijena.bioinf.ChemistryBase.jobs;

import de.unijena.bioinf.jjobs.JobManager;

public class SiriusJobs {
    private static JobManager globalJobManager = null;


    public static void setGlobalJobManager(int cpuThreads) {
        globalJobManager = new JobManager(cpuThreads);
    }

    public static void setGlobalJobManager(JobManager manager) {
        globalJobManager = manager;
    }

    public static JobManager getGlobalJobManager() {
        if (globalJobManager == null) {
            setGlobalJobManager(Runtime.getRuntime().availableProcessors() / 2);
        }
        return globalJobManager;
    }


}
