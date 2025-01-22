package de.unijena.bioinf.ms.rest.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.unijena.bioinf.ChemistryBase.utils.Utils;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY)
public class ProblemResponse {
    public static final String ERROR_CODE_SEPARATOR = ":;:";
    public static final String TERMS_MISSING =  "terms_missing";
    public static final String EMAIL_VERIFICATION_MISSING =  "email_verification_missing";
    public static final String SUB_EXPIRED =  "subscription_expired";
    public static final String LIMIT_REACHED =  "limit_reached";


    private String type;
    private String title;
    private int status;
    private String detail;
    private String instance;
    private String errorCode;

    //for backward compatibility with old custom error responses of spring.
    @Deprecated
    @JsonProperty("message")
    private String getMessage() {
        if (Utils.isNullOrBlank(errorCode))
            return Utils.isNullOrBlank(detail) ? title : detail;
        return  ERROR_CODE_SEPARATOR + errorCode + ERROR_CODE_SEPARATOR + " " + (Utils.isNullOrBlank(detail) ? title : detail);
    }

    @Deprecated
    @JsonProperty("error")
    private String getError() {
        return title;
    }
}
