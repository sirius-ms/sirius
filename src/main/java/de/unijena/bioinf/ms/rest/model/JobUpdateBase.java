package de.unijena.bioinf.ms.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.jetbrains.annotations.Nullable;

@JsonIgnoreProperties(ignoreUnknown = true)
class JobUpdateBase {
    public final JobId jobId;
    public final JobState state;
    @Nullable public final String errorMessage;


    private JobUpdateBase() {
            this(null,null,null);
    }

    protected JobUpdateBase(JobId jobId, JobState state, String errorMessage) {
        this.jobId = jobId;
        this.state = state;
        this.errorMessage = errorMessage;
    }
}
