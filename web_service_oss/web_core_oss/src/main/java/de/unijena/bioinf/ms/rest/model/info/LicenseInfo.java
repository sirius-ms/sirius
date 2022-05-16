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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LicenseInfo {
    @Nullable
    private String userEmail;
    @Nullable
    private String userId;
    @Nullable
    private Subscription subscription;
    @Nullable
    private SubscriptionConsumables consumables;

    @Nullable
    public String getUserEmail() {
        return userEmail;
    }
    @JsonIgnore
    @NotNull
    public Optional<String> userEmail() {
        return Optional.ofNullable(userEmail);
    }

    public void setUserEmail(@Nullable String userEmail) {
        this.userEmail = userEmail;
    }

    @Nullable
    public String getUserId() {
        return userId;
    }
    @JsonIgnore
    @NotNull
    public Optional<String> userId() {
        return Optional.ofNullable(userId);
    }

    public void setUserId(@Nullable String userId) {
        this.userId = userId;
    }


    @JsonIgnore
    @NotNull
    public Optional<SubscriptionConsumables> consumables(){
        return Optional.ofNullable(consumables);
    }

    @JsonIgnore
    @NotNull
    public Optional<Subscription> subscription(){
        return Optional.ofNullable(subscription);
    }

    @Nullable
    public Subscription getSubscription() {
        return subscription;
    }

    public void setSubscription(@Nullable Subscription subscription) {
        this.subscription = subscription;
    }

    @Nullable
    public SubscriptionConsumables getConsumables() {
        return consumables;
    }

    public void setConsumables(@Nullable SubscriptionConsumables consumables) {
        this.consumables = consumables;
    }


    @JsonIgnore
    public LicenseInfo copyWithUpdate(SubscriptionConsumables consumables){
        LicenseInfo nu  = new LicenseInfo();
        nu.setConsumables(consumables);
        nu.setSubscription(subscription);
        nu.setUserId(userId);
        nu.setUserEmail(userEmail);
        return nu;
    }
}
