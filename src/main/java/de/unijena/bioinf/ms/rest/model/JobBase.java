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
    protected String securityToken;
    protected String errorMessage;


    //just for jackson
    private JobBase() {
        this(null, null, null, null);
    }

    protected JobBase(Long jobId, JobState state, @Nullable String securityToken, @NotNull JobTable table) {
        this(jobId, state, securityToken, table, null);
    }

    protected JobBase(Long jobId, JobState state, @Nullable String securityToken, @NotNull JobTable table, String errorMessage) {
        this.jobId = jobId;
        this.state = state;
        this.securityToken = securityToken;
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

    public String getSecurityToken() {
        return securityToken;
    }

    public void setSecurityToken(String securityToken) {
        this.securityToken = securityToken;
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
