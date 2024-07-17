package de.unijena.bioinf.ms.rest.model;

import java.util.EnumSet;

//specifies job from which jobtable a worker has to use see @JobTable
public enum WorkerType {
    FORMULA_ID(EnumSet.noneOf(JobTable.class)),
    FINGER_ID(EnumSet.of(JobTable.JOBS_FINGERID)),
    IOKR(EnumSet.noneOf(JobTable.class)),
    CANOPUS(EnumSet.of(JobTable.JOBS_CANOPUS)),
    COVTREE(EnumSet.of(JobTable.JOBS_COVTREE));

    private final EnumSet<JobTable> jobTables;

    WorkerType(EnumSet<JobTable> jobTableNames) {
        jobTables = jobTableNames;
    }

    public EnumSet<JobTable> jobTables() {
        return jobTables;
    }
}

