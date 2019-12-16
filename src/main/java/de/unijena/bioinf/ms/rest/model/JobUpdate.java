package de.unijena.bioinf.ms.rest.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@JsonDeserialize(using = JobUpdateDeserializer.class)
public class JobUpdate<D> extends JobBase {
    @Nullable
    public final D data;

    public JobUpdate(JobBase base, @Nullable D data) {
        super(base.jobId, base.state, base.jobTable, base.errorMessage);
        this.data = data;
    }

    public JobUpdate(Long jobId, JobState state, @NotNull JobTable table, String errorMessage, @Nullable D data) {
        super(jobId, state, table, errorMessage);
        this.data = data;
    }

    public JobUpdate(Long id, JobState state, @NotNull JobTable table, @Nullable D data) {
        this(id, state,  table, null, data);
    }

    public JobUpdate(JobId jobId, JobState state, @Nullable D data) {
        this(jobId.jobId, state, jobId.jobTable, data);
    }

    public JobId getGlobalId() {
        return new JobId(jobId, jobTable);
    }
}
