package de.unijena.bioinf.ms.rest.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.jetbrains.annotations.Nullable;

@JsonDeserialize(using = JobUpdateDeserializer.class)
public class JobUpdate<D> extends JobBase {
    @Nullable
    public final D data;


    protected JobUpdate() {
        this(null, null, null, null, null);
    }

    protected JobUpdate(JobBase base, D data) {
        this(base.jobId, base.state, base.securityToken, base.errorMessage, base.jobTable, data);
    }


    public JobUpdate(Long id, JobState state, @Nullable String securityToken, @Nullable String errorMessage, @Nullable JobTable table, @Nullable D data) {
        super(id, state, securityToken, errorMessage, table);
        this.data = data;
    }

    public JobUpdate(JobId jobId, JobState state, @Nullable String securityToken, @Nullable String errorMessage, @Nullable D data) {
        this(jobId.jobId, state, securityToken, errorMessage, jobId.jobTable, data);
    }

    public JobId getGlobalId() {
        return new JobId(jobId, jobTable);
    }
}
