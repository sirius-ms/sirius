/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.middleware.model.login;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.sql.Date;

@Getter
@Setter
@Builder
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
    @Schema(nullable = true)
    private String subscriberName;

    @Schema(nullable = true)
    private Date expirationDate;

    @Schema(nullable = true)
    private Date startDate;

    @Schema(nullable = true)
    private boolean countQueries;

    /**
     * Limit of instances (features) that can be computed with this subscription
     */
    @Schema(nullable = true)
    private Integer instanceLimit;

    /**
     * Hash is used to allow recomputing identical data without increasing counted instances (features).
     * The recording time is the amount of time an instance is memorized is
     */
    @Schema(nullable = true)
    private Integer instanceHashRecordingTime;

    /**
     * Maximum number of queries (e.g. prediction) that can be performed
     * for one instance before it is counted another time.
     */
    @Schema(nullable = true)
    private Integer maxQueriesPerInstance;

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
    public boolean hasInstanceLimit() {
        Integer l = getInstanceLimit();
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

    public static Subscription of(@Nullable de.unijena.bioinf.ms.rest.model.license.Subscription s){
        if (s == null)
            return null;
        return Subscription.builder()
                .sid(s.getSid())
                .subscriberId(s.getSubscriberId())
                .subscriberName(s.getSubscriberName())
                .expirationDate(s.getExpirationDate())
                .startDate(s.getStartDate())
                .countQueries(s.hasCompoundLimit())
                .instanceLimit(s.getCompoundLimit())
                .instanceHashRecordingTime(s.getCompoundHashRecordingTime())
                .maxQueriesPerInstance(s.getMaxQueriesPerCompound())
                .maxUserAccounts(s.getMaxUserAccounts())
                .serviceUrl(s.getServiceUrl())
                .description(s.getDescription())
                .name(s.getName())
                .tos(s.getTos())
                .pp(s.getPp())
                .build();
    }
}
