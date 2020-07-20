package de.unijena.bioinf.ChemistryBase.jobs;

import de.unijena.bioinf.jjobs.JobManager;

@FunctionalInterface
public interface JobManagerFactory<M extends JobManager> {
    M createJobManager(int cpuThreads);
}
