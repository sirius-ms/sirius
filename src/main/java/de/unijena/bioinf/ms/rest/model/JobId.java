package de.unijena.bioinf.ms.rest.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class JobId {
    public final Long jobId;
    public final JobTable jobTable;

    protected JobId() {
        this(null, null);
    }


    public JobId(Long jobId, JobTable jobTable) {
        this.jobId = jobId;
        this.jobTable = jobTable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JobId)) return false;
        JobId id = (JobId) o;
        return jobId.equals(id.jobId) &&
                jobTable == id.jobTable;
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobId, jobTable);
    }
}
