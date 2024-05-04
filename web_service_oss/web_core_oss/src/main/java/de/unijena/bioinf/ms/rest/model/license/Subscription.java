package de.unijena.bioinf.ms.rest.model.license;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import java.sql.Date;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
@Jacksonized
@ToString
public class Subscription {
    /**
     * Unique identifier of this subscription
     */
    private String sid;

    /**
     * ID of the owner of the subscription.
     * This can be the ID of any SubscriptionOwner (e.g.  Group or  User)
     * depending on the level on which a subscription should be is valid.
     */
    private String subscriberId;

    /**
     * Optional name of the owner of this subscription
     */
    private String subscriberName;

    private Date expirationDate;

    private Date startDate;

    private boolean countQueries;

    /**
     * Limit of instances (features) that can be computed with this subscription
     */
    private Integer compoundLimit;

    /**
     * Hash is used to allow recomputing identical data without increasing counted instances (features). The recording time is the amount of time an instance is memorized.
     */
    private Integer compoundHashRecordingTime;

    /**
     * Maximum number of queries (e.g. prediction) that can be performed for one instance before it is counted another time.
     */
    private Integer maxQueriesPerCompound;

    @Schema(nullable = true)
    private Integer maxUserAccounts;

    private String serviceUrl;

    @Schema(nullable = true)
    private String description;

    @Schema(nullable = true)
    private String name;

    @Schema(nullable = true)
    private String tos;

    @Schema(nullable = true)
    private String pp;

    @JsonIgnore
    public boolean hasCompoundLimit() {
        Integer l = getCompoundLimit();
        return  l != null && l > 0;
    }

    @JsonIgnore
    public boolean hasExpirationTime(){
        return getExpirationDate() != null;
    }

    @JsonIgnore
    public boolean isExpired() {
        if (!hasExpirationTime())
            return false;
        return getExpirationDate().getTime() < System.currentTimeMillis();
    }
}
