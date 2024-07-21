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

package de.unijena.bioinf.ms.middleware.model.info;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import de.unijena.bioinf.ms.middleware.model.login.Subscription;
import de.unijena.bioinf.ms.rest.model.info.Term;
import de.unijena.bioinf.ms.rest.model.license.SubscriptionConsumables;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class LicenseInfo {
    /**
     * Email address of the user account this license information belongs to.
     */
    @Schema(nullable = true)
    private String userEmail;
    /**
     * User ID (uid) of the user account this license information belongs to.
     */
    @Schema(nullable = true)
    private String userId;
    /**
     * The active subscription that was used the requested the information
     */
    @Schema(nullable = true)
    private Subscription subscription;
    /**
     * Status of the consumable resources of the {@link Subscription}.
     */
    @Schema(nullable = true)
    private SubscriptionConsumables consumables;

    @Schema(nullable = true)
    private List<Term> terms;

    @JsonIgnore
    @NotNull
    public Optional<String> userEmail() {
        return Optional.ofNullable(userEmail);
    }

    @JsonIgnore
    @NotNull
    public Optional<String> userId() {
        return Optional.ofNullable(userId);
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
