// TokenWrapper.java
package io.sirius.ms.utils.jwt;

import com.fasterxml.jackson.core.type.TypeReference;
import io.sirius.ms.utils.jwt.claims.ClaimKey;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * An abstraction for a JWT, providing implementation-agnostic access to its claims.
 */
public interface TokenWrapper {

    Optional<String> getStringClaim(ClaimKey key);

    Optional<Boolean> getBooleanClaim(ClaimKey key);

    Optional<Instant> getInstantClaim(ClaimKey key);

    <T> Optional<T> getClaimAs(ClaimKey key, Class<T> clazz);

    <T> Optional<T> getClaimAs(ClaimKey key, TypeReference<T> typeReference);

    Optional<Map<String, Object>> getMapClaim(ClaimKey key);

    Optional<List<String>> getStringListClaim(ClaimKey key);

    URI getPicture();

    String getSubject();
}