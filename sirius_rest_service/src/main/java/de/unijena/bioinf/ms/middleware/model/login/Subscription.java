/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.middleware.model.login;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.sql.Date;

@Getter
@Setter
@Builder
public class Subscription {
    private String sid;

    /**
     * ID of the owner of the subscription.
     * This can be the ID of any SubscriptionOwner (e.g.  Group or  User)
     * depending on the level on which a subscription should be is valid.
     */
    private String subscriberId;

    private String subscriberName;

    private Date expirationDate;

    private Date startDate;

    private boolean countQueries;

    /**
     * Limit of instances (features) that can be computed with this subscription
     */
    private Integer instanceLimit;

    /**
     * Hash is used to allow recomputing identical data without increasing counted instances (features).
     * The recording time is the amount of time an instance is memorized is
     */
    private Integer instanceHashRecordingTime;

    /**
     * Maximum number of queries (e.g. prediction) that can be performed
     * for one instance before it is counted another time.
     */
    private Integer maxQueriesPerInstance;

    private Integer maxUserAccounts;

    private String serviceUrl;

    private String description;

    private String name;

    private String tos;

    private String pp;

    public static Subscription of(de.unijena.bioinf.ms.rest.model.license.Subscription s){
        return Subscription.builder()
                .sid(s.getSid())
                .subscriberId(s.getSubscriberId())
                .subscriberName(s.getSubscriberName())
                .expirationDate(s.getExpirationDate())
                .startDate(s.getStartDate())
                .countQueries(s.getCountQueries())
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
