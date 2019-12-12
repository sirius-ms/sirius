package de.unijena.bioinf.ms.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.jetbrains.annotations.Nullable;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

@JsonIgnoreProperties(ignoreUnknown = true)
@MappedSuperclass
public class JobBase {
    @Column public Long jobId;
    @Column public JobState state;
    @Column public String securityToken;
    @Column public String errorMessage;

    public JobTable jobTable;

    private JobBase() {
        this(null, null, null, null, null);
    }

    protected JobBase(Long jobId, JobState state, @Nullable String securityToken, @Nullable String errorMessage, @Nullable JobTable table) {
        this.jobId = jobId;
        this.state = state;
        this.securityToken = securityToken;
        this.errorMessage = errorMessage;
        this.jobTable = table;
    }
}
