package io.sirius.ms.utils.jwt.spring;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sirius.ms.utils.jwt.TokenWrapper;
import io.sirius.ms.utils.jwt.claims.ClaimKey;
import io.sirius.ms.utils.jwt.claims.StandardClaimKeys;
import org.springframework.security.oauth2.core.ClaimAccessor;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SpringJwtToken implements TokenWrapper {

    private final ClaimAccessor jwt;
    private final ObjectMapper objectMapper;

    public SpringJwtToken(Object jwt, ObjectMapper objectMapper) {
        this((ClaimAccessor)jwt, objectMapper);
    }
    public SpringJwtToken(ClaimAccessor jwt, ObjectMapper objectMapper) {
        this.jwt = jwt;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<String> getStringClaim(ClaimKey key) {
        return Optional.ofNullable(jwt.getClaimAsString(key.getKey()));
    }

    @Override
    public Optional<Boolean> getBooleanClaim(ClaimKey key) {
        return Optional.ofNullable(jwt.getClaimAsBoolean(key.getKey()));
    }

    @Override
    public Optional<Instant> getInstantClaim(ClaimKey key) {
        return Optional.ofNullable(jwt.getClaimAsInstant(key.getKey()));
    }

    @Override
    public <T> Optional<T> getClaimAs(ClaimKey key, Class<T> clazz) {
        Object claim = jwt.getClaim(key.getKey());
        if (claim == null) {
            return Optional.empty();
        }
        try {
            String json = objectMapper.writeValueAsString(claim);
            return Optional.of(objectMapper.readValue(json, clazz));
        } catch (JsonProcessingException e) {
            // Log the error
            return Optional.empty();
        }
    }

    @Override
    public <T> Optional<T> getClaimAs(ClaimKey key, TypeReference<T> typeReference) {
        Object claim = jwt.getClaim(key.getKey());
        if (claim == null) {
            return Optional.empty();
        }
        try {
            String json = objectMapper.writeValueAsString(claim);
            return Optional.of(objectMapper.readValue(json, typeReference));
        } catch (JsonProcessingException e) {
            // Log the error
            return Optional.empty();
        }
    }

    @Override
    public Optional<Map<String, Object>> getMapClaim(ClaimKey key) {
        return Optional.ofNullable(jwt.getClaimAsMap(key.getKey()));
    }

    @Override
    public Optional<List<String>> getStringListClaim(ClaimKey key) {
        return Optional.ofNullable(jwt.getClaimAsStringList(key.getKey()));
    }

    @Override
    public URI getPicture() {
        return getStringClaim(StandardClaimKeys.ID_IMAGE).map(URI::create).orElse(null);
    }

    @Override
    public String getSubject() {
        return jwt.getClaimAsString(StandardClaimKeys.SUBJECT.getKey());
    }
}