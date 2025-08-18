// Auth0JwtToken.java
package io.sirius.ms.utils.jwt.auth0;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.type.TypeReference;
import io.sirius.ms.utils.jwt.TokenWrapper;
import io.sirius.ms.utils.jwt.claims.ClaimKey;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.sirius.ms.utils.jwt.claims.StandardClaimKeys.ID_IMAGE;

public class Auth0JwtToken implements TokenWrapper {

    private final DecodedJWT decodedJWT;

    public Auth0JwtToken(Object decodedJWT) {
        this((DecodedJWT) decodedJWT);
    }

    public Auth0JwtToken(DecodedJWT decodedJWT) {
        this.decodedJWT = decodedJWT;
    }

    private Optional<Claim> getClaim(ClaimKey key) {
        Claim claim = decodedJWT.getClaim(key.getKey());
        return (claim == null || claim.isNull()) ? Optional.empty() : Optional.of(claim);
    }

    @Override
    public Optional<String> getStringClaim(ClaimKey key) {
        return getClaim(key).map(Claim::asString);
    }

    @Override
    public Optional<Boolean> getBooleanClaim(ClaimKey key) {
        return getClaim(key).map(Claim::asBoolean);
    }

    @Override
    public Optional<Instant> getInstantClaim(ClaimKey key) {
        return getClaim(key).map(c -> c.asDate().toInstant());
    }

    @Override
    public <T> Optional<T> getClaimAs(ClaimKey key, Class<T> clazz) {
        return getClaim(key).map(c -> c.as(clazz));
    }
    
    @Override
    public <T> Optional<T> getClaimAs(ClaimKey key, TypeReference<T> typeReference) {
         return getClaim(key).map(claim -> claim.as(getRawClass(typeReference)));
    }

    @Override
    public Optional<Map<String, Object>> getMapClaim(ClaimKey key) {
        return getClaim(key).map(Claim::asMap);
    }

    @Override
    public Optional<List<String>> getStringListClaim(ClaimKey key) {
        return getClaim(key).map(c -> c.asList(String.class));
    }

    @Override
    public URI getPicture() {
        return getStringClaim(ID_IMAGE).map(URI::create).orElse(null);
    }

    @Override
    public String getSubject() {
        return decodedJWT.getSubject();
    }

    /**
     * Extracts the raw Class from a Jackson TypeReference.
     *
     * @param ref The TypeReference from which to extract the class.
     * @return The raw Class object.
     * @throws IllegalArgumentException if the raw class cannot be determined.
     */
    private static <T> Class<T> getRawClass(TypeReference<T> ref) {
        Type type = ref.getType();

        if (type instanceof Class<?>) {
            // This handles simple, non-generic types like TypeReference<String>
            return (Class<T>) type;
        } else if (type instanceof ParameterizedType) {
            // This handles generic types like TypeReference<List<String>>
            // It returns the raw type, e.g., List.class
            return (Class<T>) ((ParameterizedType) type).getRawType();
        }

        throw new IllegalArgumentException("Cannot determine raw class for TypeReference: " + type);
    }
}