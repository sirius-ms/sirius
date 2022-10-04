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

package de.unijena.bioinf.ms.middleware.login.model;

import de.unijena.bioinf.auth.AuthService;
import de.unijena.bioinf.ms.rest.model.license.Subscription;
import de.unijena.bioinf.webapi.Tokens;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@Getter
@Setter
@NoArgsConstructor
public class AccountInfo {
    //todo maybe move CLASS to auth service module
    String userID;
    String username;
    String userEmail;
    String gravatarURL;
    List<Subscription> subscriptions;

//    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class)
//    @JsonIdentityReference(alwaysAsId = true)
    String activeSubscriptionId;


    public static AccountInfo of(AuthService.Token token, @Nullable Subscription activeSubscription, boolean includeSubscription) {
        AccountInfo ai = new AccountInfo();
        Tokens.getUserEmail(token).ifPresent(ai::setUserEmail);
        Tokens.getUserId(token).ifPresent(ai::setUserID);
        Tokens.getUserEmail(token).ifPresent(ai::setUsername);
        Tokens.getUserImage(token).map(URI::toString).ifPresent(ai::setGravatarURL);
        if (includeSubscription) {
            ai.setSubscriptions(Tokens.getSubscriptions(token));
            ai.setActiveSubscriptionId(
                    Optional.ofNullable(Tokens.getActiveSubscription(ai.getSubscriptions(),
                    activeSubscription== null ? null : activeSubscription.getSid())).map(Subscription::getSid).orElse(null));
        }
        return ai;
    }

    public static AccountInfo of(AuthService.Token token) {
        return of(token, null, false);
    }
}
