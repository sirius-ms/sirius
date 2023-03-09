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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class Tokens {
    public static final String ACTIVE_SUBSCRIPTION_KEY = "de.unijena.bioinf.sirius.security.subscription";
    public static final String DEFAULT_SUB_KEY = "defaultSubscription";


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

    public static @NotNull Optional<String> getUsername(@Nullable AuthService.@Nullable Token token) {
        return parseIDClaim(token, "name").map(Claim::asString);
    }

    private static Optional<Claim> parseATClaim(@Nullable AuthService.Token token, @NotNull String key) {
        if (token == null)
            return Optional.empty();
        return parseClaim(token.getDecodedAccessToken(), key);
    }

    private static Optional<Claim> parseIDClaim(@Nullable AuthService.Token token, @NotNull String key) {
        if (token == null)
            return Optional.empty();
        return parseClaim(token.getDecodedIdToken(), key);
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
        return getAppMetaData(token)
                .map(m -> (List<String>) m.get("acceptedTerms"))
                .map(t -> t.stream().map(Term::of).collect(Collectors.toList()))
                .orElse(List.of());
    }

    public static Optional<Map<String, Object>> getAppMetaData(@Nullable AuthService.Token token){
        return parseATClaim(token, "https://bright-giant.com/app_metadata")
                .map(Claim::asMap);
    }

    public static Optional<Map<String, Object>> getUserMetaData(@Nullable AuthService.Token token){
        return parseATClaim(token, "https://bright-giant.com/user_metadata")
                .map(Claim::asMap);
    }

    @NotNull
    public static List<Term> getActiveSubscriptionTerms(@Nullable AuthService.Token token) {
        return getActiveSubscriptionTerms(getActiveSubscription(token));
    }

    @NotNull
    public static List<Term> getActiveSubscriptionTerms(@Nullable Subscription sub) {
        if (sub == null)
            return List.of();

        List<Term> terms = new ArrayList<>();
        if (sub.getTos() != null)
            terms.add(Term.of("Terms of Service", sub.getTos()));
        if (sub.getPp() != null)
            terms.add(Term.of("Privacy Policy", sub.getPp()));

        return terms;
    }


    @NotNull
    public static Optional<SubscriptionData> getSubscriptionData(@Nullable AuthService.Token token) {
        return parseATClaim(token, "https://bright-giant.com/licenseData").map(c -> c.as(SubscriptionData.class));
    }

    @NotNull
    public static List<Subscription> getSubscriptions(@Nullable AuthService.Token token) {
        return getSubscriptionData(token).map(SubscriptionData::getSubscriptions).orElse(List.of());
    }

    @Nullable
    public static Subscription getActiveSubscription(@NotNull List<Subscription> subs, @Nullable String defaultSid) {
        return getActiveSubscription(subs, PropertyManager.getProperty(ACTIVE_SUBSCRIPTION_KEY), defaultSid);
    }

    @Nullable
    public static Subscription getActiveSubscription(@NotNull List<Subscription> subs, @Nullable String sid, @Nullable String defaultSid) {
        return getActiveSubscription(subs, sid, defaultSid, true);
    }

    @Nullable
    public static Subscription getActiveSubscription(@NotNull List<Subscription> subs, @Nullable String sid, @Nullable String defaultSid, boolean useFallback) {
        Subscription sub = null;
        if (sid != null && !sid.isBlank()) {
            sub = subs.stream().filter(s -> s.getSid().equals(sid)).findFirst()
                    .orElse(null);
        }

        if (sub == null && defaultSid != null && !defaultSid.isBlank()){
            sub = subs.stream().filter(s -> s.getSid().equals(defaultSid)).findFirst()
                    .orElse(null);
        }

        if (sub == null && useFallback)
            sub = subs.stream().findFirst().orElse(null);

        return sub;
    }

    public static Subscription getActiveSubscription(@Nullable AuthService.Token token, @Nullable String sid, @Nullable String defaultSid, boolean useFallback) {
        return getActiveSubscription(getSubscriptions(token), sid, defaultSid, useFallback);
    }

    @Nullable
    public static Subscription getActiveSubscription(@Nullable AuthService.Token token, @Nullable String sid) {
        return getActiveSubscription(getSubscriptions(token), sid, getDefaultSubscriptionID(token));

    }

    @Nullable
    public static Subscription getActiveSubscription(@Nullable AuthService.Token token) {
        return getActiveSubscription(getSubscriptions(token), getDefaultSubscriptionID(token));
    }

    @Nullable
    public static String getDefaultSubscriptionID(@Nullable AuthService.Token token){
        return getUserMetaData(token).map(m -> (String) m.get(DEFAULT_SUB_KEY)).orElse(null);
    }

    public static boolean hasSubscriptions(AuthService.Token token) {
        return !getSubscriptions(token).isEmpty();
    }
}
