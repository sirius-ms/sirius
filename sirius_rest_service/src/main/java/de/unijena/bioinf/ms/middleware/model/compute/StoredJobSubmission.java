package de.unijena.bioinf.ms.middleware.model.compute;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class StoredJobSubmission {
    /**
     * Unique name to identify this JobSubmission (job config).
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    /**
     * False for predefined configs which are not editable and not removable.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean editable;

    /**
     * The JobSubmission identified by the name
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private JobSubmission jobSubmission;
}
