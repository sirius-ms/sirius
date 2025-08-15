package de.unijena.bioinf.ms.rest.model;

import com.fasterxml.jackson.annotation.*;
import de.unijena.bioinf.ChemistryBase.utils.Utils;
import lombok.Getter;
import lombok.Setter;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY)
public class ProblemResponse {
    public static final String ERROR_CODE_SEPARATOR = ":;:";
    public static final String TERMS_MISSING =  "terms_missing";
    public static final String EMAIL_VERIFICATION_MISSING =  "email_verification_missing";
    public static final String SUB_EXPIRED =  "subscription_expired";
    public static final String LIMIT_REACHED =  "limit_reached";
    public static final String FORBIDDEN =  "forbidden";

    @Getter
    @Setter
    private URI type;
    @Getter
    @Setter
    private String title;
    @Getter
    @Setter
    private int status;
    @Getter
    @Setter
    private String detail;
    @Getter
    @Setter
    private URI instance;
    @Getter
    @Setter
    private String errorCode;

    private final Map<String, Object> properties = new LinkedHashMap<>();
    /**
     * This annotation tells Jackson to call this method for any unrecognized
     * field during deserialization. This prevents an error and allows us to
     * capture all extra properties.
     */
    @JsonAnySetter
    public void setProperty(String name, Object value) {
        this.properties.put(name, value);
    }

    /**
     * This annotation tells Jackson to "unroll" this map into top-level
     * properties during serialization, preserving the original JSON structure.
     */
    @JsonAnyGetter
    public Map<String, Object> getProperties() {
        return properties;
    }

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
