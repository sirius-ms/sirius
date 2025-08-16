package de.unijena.bioinf.ms.middleware.security;

import de.unijena.bioinf.ChemistryBase.utils.Utils;
import de.unijena.bioinf.ms.rest.model.license.AllowedFeatures;
import de.unijena.bioinf.ms.rest.model.license.Subscription;
import io.sirius.ms.utils.jwt.AccessTokens;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Authorities {
    public static final String AUTHORITIES_CLAIM_NAME = "permissions";

    public static final String ALLOWED_FEATURE_PREFIX = "allowedFeature:";
    public static final String ACCOUNT_PREFIX = "account:";
    public static final String READ_PREFIX = "read:";
    public static final String WRITE_PREFIX = "write:";
    public static final String BYPASS_PREFIX = "bypass:";



    public static final GrantedAuthority BYPASS__EXPLORER =  new SimpleGrantedAuthority(BYPASS_PREFIX + "explorer");
    public static final GrantedAuthority BYPASS__GUI =  new SimpleGrantedAuthority(BYPASS_PREFIX + "gui");

    public static final GrantedAuthority ACCOUNT_VERIFIED = new SimpleGrantedAuthority(ACCOUNT_PREFIX + "verified");
    public static final GrantedAuthority ACCOUNT_UNPROTECTED = new SimpleGrantedAuthority(ACCOUNT_PREFIX + "unprotected");

    public static final GrantedAuthority ALLOWED_FEATURE__API;
    public static final GrantedAuthority ALLOWED_FEATURE__DENOVO;
    public static final GrantedAuthority ALLOWED_FEATURE__IMPORT_PEAKLISTS;
    public static final GrantedAuthority ALLOWED_FEATURE__IMPORT_CEF;
    public static final GrantedAuthority ALLOWED_FEATURE__IMPORT_MSRUNS;


    private static final Map<String, GrantedAuthority> AUTHORITIES = new HashMap<>();

    static {
        AUTHORITIES.put(ACCOUNT_VERIFIED.getAuthority(), ACCOUNT_VERIFIED);
        AUTHORITIES.put(ACCOUNT_UNPROTECTED.getAuthority(), ACCOUNT_UNPROTECTED);
        AUTHORITIES.put(BYPASS__EXPLORER.getAuthority(), BYPASS__EXPLORER);
        AUTHORITIES.put(BYPASS__GUI.getAuthority(), BYPASS__GUI);

        AllowedFeatures allTrue = new AllowedFeatures(true, true, true, true, true, true);
        createAuthoritiesForAllowedFeatures(allTrue, SimpleGrantedAuthority::new)
                .forEach(ga -> AUTHORITIES.putIfAbsent(ga.getAuthority(), ga));

        ALLOWED_FEATURE__API = AUTHORITIES.get(ALLOWED_FEATURE_PREFIX + "api");
        ALLOWED_FEATURE__DENOVO = AUTHORITIES.get(ALLOWED_FEATURE_PREFIX + "deNovo");
        ALLOWED_FEATURE__IMPORT_MSRUNS = AUTHORITIES.get(ALLOWED_FEATURE_PREFIX + "importMSRuns");
        ALLOWED_FEATURE__IMPORT_PEAKLISTS = AUTHORITIES.get(ALLOWED_FEATURE_PREFIX + "importPeakLists");
        ALLOWED_FEATURE__IMPORT_CEF = AUTHORITIES.get(ALLOWED_FEATURE_PREFIX + "importCef");
    }

    public static boolean hasAuthority(String authority, Authentication authentication) {
        if (authentication == null)
            return false;
        return hasAuthority(authority, authentication.getAuthorities());
    }

    public static boolean hasAuthority(@NotNull GrantedAuthority authority, Authentication authentication) {
        if (authentication == null)
            return false;
        return hasAuthority(authority, authentication.getAuthorities());
    }

    public static boolean hasAuthority(String authority, Collection<? extends GrantedAuthority> authorities) {
        if (Utils.isNullOrBlank(authority))
            return false;
        return hasAuthority(fromNameOrCreate(authority), authorities);
    }

    public static boolean hasAuthority(@NotNull GrantedAuthority authority, Collection<? extends GrantedAuthority> authorities) {
        return authorities.contains(authority);
    }

    public static boolean hasAnyAuthority(Authentication authentication, String... authorities) {
        if (authentication == null)
            return false;
        return hasAnyAuthority(authentication.getAuthorities(), authorities);
    }

    public static boolean hasAnyAuthority(Authentication authentication, GrantedAuthority... authorities) {
        if (authentication == null)
            return false;
        return hasAnyAuthority(authentication.getAuthorities(), authorities);
    }

    public static boolean hasAnyAuthority(Collection<? extends GrantedAuthority> authorities, String... authoritiesToCheck) {
       return Arrays.stream(authoritiesToCheck)
               .filter(Utils::notNullOrBlank).map(Authorities::fromNameOrCreate)
               .anyMatch(authorities::contains);
    }

    public static boolean hasAnyAuthority(Collection<? extends GrantedAuthority> authorities, GrantedAuthority... authoritiesToCheck) {
        return Arrays.stream(authoritiesToCheck).filter(Objects::nonNull).anyMatch(authorities::contains);
    }


    public static GrantedAuthority fromNameOrCreate(String name) {
        return Optional.ofNullable(AUTHORITIES.get(name)).orElse(new SimpleGrantedAuthority(name));
    }

    public static Optional<GrantedAuthority> fromName(String name) {
        return Optional.ofNullable(AUTHORITIES.get(name));
    }

    /**
     * Add additional authorities from jwt that are not covered by the:
     *  - Authorities claim {@value  AUTHORITIES_CLAIM_NAME}.
     *  - Active Subscription {@link Subscription}
     *
     * @param token suppoerted token type see {@link  AccessTokens} for details
     * @return List of GrantedAuthority that are derived from claims in the tokens
     */
    public static List<GrantedAuthority> getAdditionalFromAccessToken(Object token) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (AccessTokens.ACCESS_TOKENS.isUserEmailVerified(token))
            authorities.add(ACCOUNT_VERIFIED);
        if (!AccessTokens.ACCESS_TOKENS.isUserAccountProtected(token).orElse(true))
            authorities.add(ACCOUNT_UNPROTECTED);
        return authorities;
    }

    public static List<GrantedAuthority> getFromPrefix(String prefix) {
        return AUTHORITIES.values().stream()
                .filter(authority -> authority.getAuthority().startsWith(prefix))
                .collect(Collectors.toList());
    }

    public static List<GrantedAuthority> getFromAllowedFeatures(AllowedFeatures allowedFeatures) {
        return createAuthoritiesForAllowedFeatures(allowedFeatures, AUTHORITIES::get);
    }

    public static List<GrantedAuthority> getFromSubscription(Subscription subscription) {
        if (subscription == null)
            return List.of();
        return getFromAllowedFeatures(subscription.getAllowedFeatures());
    }


    /**
     * Dynamically creates a list of strings for each feature that is true.
     * <p>
     * This method uses reflection to inspect the record's components at runtime.
     * It will automatically work even if fields are added to or removed
     * from the {@code AllowedFeatures} record.
     *
     * @return A {@code List<GrantedAuthority>} containing all enabled features.
     */
    private static List<GrantedAuthority> createAuthoritiesForAllowedFeatures(@Nullable AllowedFeatures allowedFeatures, @NotNull Function<String, GrantedAuthority> function) {
        if (allowedFeatures == null)
            return List.of();

        final List<GrantedAuthority> enabledFeatures = new ArrayList<>();

        // Get all components of the record (i.e., its fields)
        RecordComponent[] components = allowedFeatures.getClass().getRecordComponents();

        for (RecordComponent component : components) {
            // We only care about boolean fields
            if (component.getType() == boolean.class) {
                try {
                    // Get the public accessor method for this component (e.g., "cli()")
                    Method accessor = component.getAccessor();
                    // Invoke the method on the 'features' instance to get its value
                    boolean isEnabled = (boolean) accessor.invoke(allowedFeatures);

                    if (isEnabled) {
                        // If true, add the formatted string using the component's name
                        enabledFeatures.add(function.apply(ALLOWED_FEATURE_PREFIX + component.getName()));
                    }
                } catch (Exception e) {
                    // This exception would indicate a deeper problem with reflection.
                    throw new RuntimeException("Could not inspect feature: " + component.getName(), e);
                }
            }
        }
        return enabledFeatures;

    }
}
