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

package de.unijena.bioinf.ms.rest.model.info;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import de.unijena.bioinf.ms.rest.model.license.Subscription;
import de.unijena.bioinf.ms.rest.model.license.SubscriptionConsumables;

import java.sql.Date;
@JsonIgnoreProperties(ignoreUnknown = true)
public class LicenseInfo {
    private String userEmail;
    private String userId;
    private Subscription subscription;
    private SubscriptionConsumables consumables;


    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Subscription getSubscription() {
        return subscription;
    }

    public void setSubscription(Subscription subscription) {
        this.subscription = subscription;
    }

    public SubscriptionConsumables getConsumables() {
        return consumables;
    }

    public void setConsumables(SubscriptionConsumables consumables) {
        this.consumables = consumables;
    }

    public String getLicensee() {
        return subscription.getSubscriberName();
    }


    public boolean isCountQueries() {
        return subscription.getCountQueries();
    }

    public int getCompoundHashRecordingTime() {
        return subscription.getCompoundHashRecordingTime();
    }


    public int getMaxQueriesPerCompound() {
        return subscription.getMaxQueriesPerCompound();
    }

    public int getCompoundLimit() {
        return subscription.getCompoundLimit();
    }

    public int getCountedCompounds() {
        return consumables.getCountedCompounds();
    }

    public String getSid() {
        return subscription.getSid();
    }

    public Date getExpirationDate() {
        return subscription.getExpirationDate();
    }

    public String getDescription() {
        return subscription.getDescription();
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

    @JsonIgnore
    public long getExpirationTime() {
        Date d = getExpirationDate();
        if (d == null)
            return -1;
        return d.getTime();
    }

    @JsonIgnore
    public boolean hasCompoundLimit() {
        return getCompoundLimit() > 0;
    }

    @JsonIgnore
    public LicenseInfo copyWithUpdate(SubscriptionConsumables consumables){
        LicenseInfo nu  = new LicenseInfo();
        nu.setConsumables(consumables);
        nu.setSubscription(getSubscription());
        return nu;
    }
}
