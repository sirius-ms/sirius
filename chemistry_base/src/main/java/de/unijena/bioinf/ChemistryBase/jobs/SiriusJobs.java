package de.unijena.bioinf.ChemistryBase.jobs;

import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

public class SiriusJobs {

    private static volatile JobManager globalJobManager = null;

    private SiriusJobs() {/*prevent instantiation*/}

    public static void setGlobalJobManager(int cpuThreads) {
        replace(new JobManager(cpuThreads, Math.min(cpuThreads, 3)));
    }

    private static void replace(JobManager jobManager) {
        final JobManager oldManager = globalJobManager;
        globalJobManager = jobManager;
        if (oldManager != null) {
            try {
                oldManager.shutdown();
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

    public static TinyBackgroundJJob<Boolean> runInBackground(final Runnable task) {
        final TinyBackgroundJJob<Boolean> t = new TinyBackgroundJJob<>() {
            @Override
            protected Boolean compute() {
                task.run();
                return true;
            }
        };
        return getGlobalJobManager().submitJob(t);
    }

    public static <T> TinyBackgroundJJob<T> runInBackground(TinyBackgroundJJob<T> task) {
        return getGlobalJobManager().submitJob(task);
    }

    public static <T> TinyBackgroundJJob<T> runInBackground(Callable<T> task) {
        return getGlobalJobManager().submitJob(new TinyBackgroundJJob<T>() {
            @Override
            protected T compute() throws Exception {
                return task.call();
            }
        });
    }


}
