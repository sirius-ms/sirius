package de.unijena.bioinf.ChemistryBase.jobs;

import de.unijena.bioinf.ChemistryBase.properties.PropertyManager;
import de.unijena.bioinf.jjobs.JobManager;
import org.slf4j.LoggerFactory;

public class SiriusJobs {

    private static volatile JobManager globalJobManager = null;


    public static void setGlobalJobManager(int cpuThreads) {
        replace(new JobManager(cpuThreads));
    }

    private static void replace(JobManager jobManager) {
        final JobManager oldManager = globalJobManager;
        globalJobManager = jobManager;
        if (oldManager!=null) {
            try {
                globalJobManager.shutdown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

    }

    public static void setGlobalJobManager(JobManager manager) {
        replace(manager);
    }

    public static JobManager getGlobalJobManager() {
        if (globalJobManager == null) {
            setGlobalJobManager(PropertyManager.getNumberOfCores());
            LoggerFactory.getLogger(SiriusJobs.class).info("Job manager successful initialized with " + globalJobManager.getCPUThreads() + " CPU thread(s) and " + globalJobManager.getIOThreads() + " IO thread(s).");
        }
        return globalJobManager;
    }


}
