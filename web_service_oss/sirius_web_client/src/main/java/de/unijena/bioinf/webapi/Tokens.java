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

package de.unijena.bioinf.webapi;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import de.unijena.bioinf.auth.AuthService;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.ms.rest.model.info.Term;
import de.unijena.bioinf.ms.rest.model.license.Subscription;
import de.unijena.bioinf.ms.rest.model.license.SubscriptionData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Tokens {
    public static final String ACTIVE_SUBSCRIPTION_KEY = "de.unijena.bioinf.sirius.security.subscription";

    @NotNull
    public static Boolean isUserEmailVerified(@Nullable AuthService.Token token) {
        return parseATClaim(token, "https://bright-giant.com/email_verified").map(Claim::asBoolean).orElse(false);
    }

    @NotNull
    public static Optional<URI> getUserImage(@Nullable AuthService.Token token) {
        return parseIDClaim(token, "picture").map(Claim::asString).map(URI::create);
    }

    @NotNull
    public static Optional<String> getUserId(@Nullable AuthService.Token token) {
        return parseIDClaim(token, "sub").map(Claim::asString);
    }

    @NotNull
    public static Optional<String> getUserEmail(@Nullable AuthService.Token token) {
        return parseIDClaim(token, "email").map(Claim::asString);
    }
    
    private static Optional<Claim> parseATClaim(@Nullable AuthService.Token token, @NotNull String key) {
        if (token == null)
            return Optional.empty();
        return parseClaim(token.getDecodedAccessToken(),key);
    }
    private static Optional<Claim> parseIDClaim(@Nullable AuthService.Token token, @NotNull String key) {
        if (token == null)
            return Optional.empty();
        return parseClaim(token.getDecodedIdToken(),key);
    }
    private static Optional<Claim> parseClaim(@Nullable DecodedJWT token, @NotNull String key) {
        if (token == null)
            return Optional.empty();
        Claim claim = token.getClaim(key);
        if (claim.isNull())
            return Optional.empty();
        return Optional.of(claim);
    }

    @NotNull
    public static List<Term> getAcceptedTerms(@Nullable AuthService.Token token) {
        return parseATClaim(token, "https://bright-giant.com/app_metadata")
                .map(Claim::asMap)
                .map(m -> (List<String>) m.get("acceptedTerms"))
                .map(t -> t.stream().map(Term::of).collect(Collectors.toList()))
                .orElse(List.of());
    }

    @NotNull
    public static List<Term> getActiveSubscriptionTerms(@Nullable AuthService.Token token) {
        @Nullable Subscription sub = Tokens.getActiveSubscription(token);
        if (sub == null)
            return List.of();
        return List.of(Term.of(sub.getPp()), Term.of(sub.getTos()));
    }


    @NotNull
    public static Optional<SubscriptionData> getLicenseData(@Nullable AuthService.Token token) {
        return parseATClaim(token,"https://bright-giant.com/licenseData").map(c -> c.as(SubscriptionData.class));
    }

    @NotNull
    public static List<Subscription> getSubscriptions(@Nullable AuthService.Token token) {
        return getLicenseData(token).map(SubscriptionData::getSubscriptions).orElse(List.of());
    }

    @Nullable
    public static Subscription getActiveSubscription(@NotNull List<Subscription> subs) {
        return getActiveSubscription(subs, PropertyManager.getProperty(ACTIVE_SUBSCRIPTION_KEY));
    }

    @Nullable
    public static Subscription getActiveSubscription(@NotNull List<Subscription> subs, @Nullable String sid) {
        return getActiveSubscription(subs, sid, true);
    }

    @Nullable
    public static Subscription getActiveSubscription(@NotNull List<Subscription> subs, @Nullable String sid, boolean useFallback) {
        Subscription sub = null;
        if (sid != null && !sid.isBlank()) {
            sub = subs.stream().filter(s -> s.getSid().equals(sid)).findFirst()
                    .orElse(null);
        }
        if (sub == null && useFallback)
            sub = subs.stream().findFirst().orElse(null);

        return sub;
    }

    public static Subscription getActiveSubscription(@Nullable AuthService.Token token, @Nullable String sid, boolean useFallback) {
        return getActiveSubscription(getSubscriptions(token), sid, useFallback);
    }

    @Nullable
    public static Subscription getActiveSubscription(@Nullable AuthService.Token token, @Nullable String sid) {
        return getActiveSubscription(getSubscriptions(token), sid);

    }

    @Nullable
    public static Subscription getActiveSubscription(@Nullable AuthService.Token token) {
        return getActiveSubscription(getSubscriptions(token));
    }

    public static boolean hasSubscriptions(AuthService.Token token) {
        return !getSubscriptions(token).isEmpty();
    }


}
