package de.unijena.bioinf.ms.middleware.model.tags;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
@SuperBuilder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TagSubmission extends Tag {
    /**
     * ID of the object where the tag should be added.
     * Then Object type is taken from context of the API endpoint.
     */
    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String taggedObjectId;
}
