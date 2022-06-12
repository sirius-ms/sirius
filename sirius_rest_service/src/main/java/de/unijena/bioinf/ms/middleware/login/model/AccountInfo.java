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

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import de.unijena.bioinf.auth.AuthService;
import de.unijena.bioinf.ms.rest.model.license.Subscription;
import de.unijena.bioinf.webapi.Tokens;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class AccountInfo {
    //todo maybe move CLASS to auth service module
    String userID;
    String username;
    String emailEmail;
    List<Subscription> subscriptions;

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "sid")
    @JsonIdentityReference(alwaysAsId = true)
    Subscription activeSubscription;

    public static AccountInfo of(AuthService.Token token, boolean includeSubscription) {
        AccountInfo ai = new AccountInfo();

        if (includeSubscription) {
            ai.setSubscriptions(Tokens.getSubscriptions(token));
            ai.setActiveSubscription(Tokens.getActiveSubscription(ai.getSubscriptions()));
        }
        return ai;
    }

    public static AccountInfo of(AuthService.Token token, Subscription activeSubscription) {
        AccountInfo ai = new AccountInfo();
        ai.setSubscriptions(Tokens.getSubscriptions(token));
        ai.setActiveSubscription(Tokens.getActiveSubscription(ai.getSubscriptions(), activeSubscription.getSid()));
        return ai;
    }

    public static AccountInfo of(AuthService.Token token) {
        return of(token, false);
    }
}
