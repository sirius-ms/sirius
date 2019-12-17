package de.unijena.bioinf.ms.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JobBase {
    @NotNull
    protected final JobTable jobTable;

    protected Long jobId;
    protected JobState state;
    protected String errorMessage;


    //just for jackson
    private JobBase() {
        this(null, null, null);
    }

    protected JobBase(Long jobId, JobState state, @NotNull JobTable table) {
        this(jobId, state, table, null);
    }

    protected JobBase(Long jobId, JobState state, @NotNull JobTable table, String errorMessage) {
        this.jobId = jobId;
        this.state = state;
        this.jobTable = table;
        this.errorMessage = errorMessage;
    }

    public JobState getStateEnum() {
        return state;
    }

    public void setStateEnum(JobState state) {
        this.state = state;
    }

    public int getState() {
        return getStateEnum().id();
    }

    public void setState(int state) {
        setStateEnum(JobState.withId(state));
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public JobTable getJobTable() {
        return jobTable;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }
}
