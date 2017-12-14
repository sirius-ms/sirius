package de.unijena.bioinf.ChemistryBase.jobs;

import de.unijena.bioinf.ChemistryBase.properties.PropertyManager;
import de.unijena.bioinf.jjobs.JobManager;
import org.slf4j.LoggerFactory;

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
            setGlobalJobManager(PropertyManager.getNumberOfCores());
            LoggerFactory.getLogger(SiriusJobs.class).info("Job manager initialized with " + globalJobManager.getCPUThreads() + "CPU threads and " + globalJobManager.getIOThreads() + " IO threads");
        }
        return globalJobManager;
    }


}
