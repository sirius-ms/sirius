// AccessTokens.java
package io.sirius.ms.utils.jwt;

import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.ms.rest.model.info.Term;
import de.unijena.bioinf.ms.rest.model.license.Subscription;
import de.unijena.bioinf.ms.rest.model.license.SubscriptionData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static io.sirius.ms.utils.jwt.claims.StandardClaimKeys.*;

/**
 * A unified, implementation-agnostic utility for extracting claims and business data from JWTs.
 */
public class AccessTokens extends Tokens{
    public static final AccessTokens ACCESS_TOKENS = new AccessTokens();

    public AccessTokens() {
        super();
    }

    public AccessTokens(JwtTokenFactory tokenFactory) {
        super(tokenFactory);
    }

    @Override
    protected Optional<TokenWrapper> createToken(@Nullable Object jwt) {
        return createToken(jwt, TokenType.ACCESS_TOKEN);
    }

    // --- Core User Attribute Extraction ---

    @NotNull
    public Optional<String> getUserId(@Nullable Object jwt) {
        return createToken(jwt).map(TokenWrapper::getSubject);
    }

    @NotNull
    public Optional<String> getUserEmail(@Nullable Object jwt) {
        return createToken(jwt).flatMap(t -> t.getStringClaim(USER_EMAIL));
    }

    @NotNull
    public Optional<String> getUsername(@Nullable Object jwt) {
        return createToken(jwt).flatMap(t -> t.getStringClaim(USER_NAME));
    }

    public boolean isUserEmailVerified(@Nullable Object jwt) {
        // Now uses the namespaced key from Tokens.java
        return createToken(jwt).flatMap(t -> t.getBooleanClaim(USER_EMAIL_VERIFIED)).orElse(false);
    }

    public Optional<Boolean> isUserAccountProtected(@Nullable Object jwt) {
        Map<String, Object> appMetadata = getAppMetadata(jwt).orElse(null);
        if (appMetadata == null)
            return Optional.empty();
        return Optional.of((Boolean)appMetadata.getOrDefault(APP_META_DATA__PROTECTED.getKey(), Boolean.FALSE));
    }


    // --- Metadata Extraction ---
    @NotNull
    public Optional<Map<String, Object>> getAppMetadata(@Nullable Object jwt) {
        return createToken(jwt).flatMap(t -> t.getMapClaim(APP_METADATA));
    }

    @NotNull
    public Optional<Map<String, Object>> getUserMetadata(@Nullable Object jwt) {
        return createToken(jwt).flatMap(t -> t.getMapClaim(USER_METADATA));
    }

    // --- Subscription and License Logic ---

    @NotNull
    public Optional<SubscriptionData> getSubscriptionData(@Nullable Object jwt) {
        if (jwt == null)
            return Optional.empty();
        return tokenFactory.create(jwt)
                .getClaimAs(LICENSE_DATA, SubscriptionData.class);
    }


    @NotNull
    public List<Subscription> getSubscriptions(@Nullable Object jwt) {
        return getSubscriptionData(jwt).map(SubscriptionData::getSubscriptions).orElse(List.of());
    }

    @Nullable
    public Subscription getActiveSubscription(@NotNull List<Subscription> subs, @Nullable String defaultSid) {
        return getActiveSubscription(subs, PropertyManager.getProperty("de.unijena.bioinf.sirius.security.subscription"), defaultSid);
    }

    @Nullable
    public Subscription getActiveSubscription(@NotNull List<Subscription> subs, @Nullable String sid, @Nullable String defaultSid) {
        return getActiveSubscription(subs, sid, defaultSid, true);
    }

    @Nullable
    public Subscription getActiveSubscription(@NotNull List<Subscription> subs, @Nullable String sid, @Nullable String defaultSid, boolean useFallback) {
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

    public Subscription getActiveSubscription(@Nullable Object jwt, @Nullable String sid, @Nullable String defaultSid, boolean useFallback) {
        return getActiveSubscription(getSubscriptions(jwt), sid, defaultSid, useFallback);
    }

    @Nullable
    public Subscription getActiveSubscription(@Nullable Object jwt, @Nullable String sid) {
        return getActiveSubscription(getSubscriptions(jwt), sid, getDefaultSubscriptionId(jwt));

    }

    @Nullable
    public Subscription getActiveSubscription(@Nullable Object jwt) {
        return getActiveSubscription(getSubscriptions(jwt), getDefaultSubscriptionId(jwt));
    }

    @Nullable
    public String getDefaultSubscriptionId(Object jwt) {
        return getUserMetadata(jwt)
                .map(metadata -> (String) metadata.get(USER_METADATA__DEFAULT_SUBSCRIPTION.getKey()))
                .orElse(null);
    }

    public boolean hasSubscriptions(Object jwt) {
        return !getSubscriptions(jwt).isEmpty();
    }


    // --- Terms and Conditions Logic ---

    public List<Term> getAcceptedTerms(Object jwt) {
        return getAppMetadata(jwt)
                .flatMap(metadata -> Optional.ofNullable((List<String>) metadata.get(APP_METADATA__ACCEPTED_TERMS.getKey())))
                .map(termStrings -> termStrings.stream().map(Term::of).collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    @NotNull
    public List<Term> getActiveSubscriptionTerms(@Nullable Object jwt) {
        return getActiveSubscriptionTerms(getActiveSubscription(jwt));
    }

    @NotNull
    public List<Term> getActiveSubscriptionTerms(@Nullable Subscription sub) {
        if (sub == null)
            return List.of();

        List<Term> terms = new ArrayList<>();
        if (sub.getTos() != null)
            terms.add(Term.of("Terms of Service", sub.getTos()));
        if (sub.getPp() != null)
            terms.add(Term.of("Privacy Policy", sub.getPp()));

        return terms;
    }

    // --- Token State Logic ---

    public boolean isExpired(Object jwt, long buffer, ChronoUnit unit) {
        Optional<Instant> expiresAt = createToken(jwt).flatMap(t -> t.getInstantClaim(EXPIRES_AT));
        return expiresAt.map(exp -> exp.minus(buffer, unit).isBefore(Instant.now())).orElse(true);
    }

    public boolean isExpired(Object jwt) {
        return isExpired(jwt, 10, ChronoUnit.SECONDS); // Default buffer
    }
}