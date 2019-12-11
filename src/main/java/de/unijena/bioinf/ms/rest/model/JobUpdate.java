package de.unijena.bioinf.ms.rest.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.jetbrains.annotations.Nullable;

@JsonDeserialize(using = JobUpdateDeserializer.class)
public class JobUpdate<D> extends JobUpdateBase {
    @Nullable
    public final D data;


    protected JobUpdate() {
        this(null, null, null, null);
    }

    protected JobUpdate(JobUpdateBase base, D data) {
        this(base.jobId, base.state, base.errorMessage, data);
    }


    public JobUpdate(Long id, JobTable table,JobState state, String errorMessage, D data) {
        this(new JobId(id, table), state, errorMessage, data);
    }

    public JobUpdate(JobId jobId, JobState state, String errorMessage, @Nullable D data) {
        super(jobId, state, errorMessage);
        this.data = data;
    }
}
