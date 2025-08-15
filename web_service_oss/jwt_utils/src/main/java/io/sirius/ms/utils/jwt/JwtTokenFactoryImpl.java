package io.sirius.ms.utils.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.sirius.ms.utils.jwt.auth.AuthServiceJwtToken;
import io.sirius.ms.utils.jwt.auth0.Auth0JwtToken;
import io.sirius.ms.utils.jwt.spring.SpringJwtToken;
import org.apache.commons.lang3.ClassUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JwtTokenFactoryImpl implements JwtTokenFactory {

    // Store the class names as constants
    private static final String SPRING_JWT_CLASS = "org.springframework.security.oauth2.core.ClaimAccessor";
    private static final String AUTH0_DECODED_JWT_CLASS = "com.auth0.jwt.interfaces.DecodedJWT";
    private static final String AUTH_SERVICE_TOKEN = "de.unijena.bioinf.auth.AuthService$Token";

    // Define the check order. More specific classes should be listed before general interfaces.
    private static final List<String> SUPPORTED_TYPES = List.of(
            SPRING_JWT_CLASS,
            AUTH_SERVICE_TOKEN,
            AUTH0_DECODED_JWT_CLASS
    );

    private final ObjectMapper objectMapper;

    public JwtTokenFactoryImpl() {
        this(new ObjectMapper());
    }

    public JwtTokenFactoryImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public @NotNull TokenWrapper create(@NotNull Object jwt, TokenType tokenType) {
        String matchingType = findMatchingType(jwt.getClass());

        if (matchingType == null)
            throw new IllegalArgumentException("Unsupported JWT type: " + jwt.getClass().getName());

        return switch (matchingType) {
            case SPRING_JWT_CLASS -> new SpringJwtToken(jwt, objectMapper);
            case AUTH_SERVICE_TOKEN -> new AuthServiceJwtToken(jwt, tokenType);
            case AUTH0_DECODED_JWT_CLASS -> new Auth0JwtToken(jwt);
            default -> throw new IllegalStateException("Unknown matching type found: " + matchingType);
        };
    }

    /**
     * Finds the best-matching supported type name for a given class by searching its hierarchy.
     * The search is prioritized based on the order defined in SUPPORTED_TYPES_IN_ORDER.
     *
     * @param clazz The class to check.
     * @return The string name of the highest-priority matching type, or null if no match is found.
     */
    @Nullable
    private String findMatchingType(Class<?> clazz) {
        // Phase 1: Quick check on the initial class's name. This is a fast path for common cases.
        for (String typeName : SUPPORTED_TYPES) {
            if (clazz.getName().equals(typeName)) {
                return typeName;
            }
        }

        // Phase 2: If no direct match, traverse the entire class hierarchy and search again.
        Set<Class<?>> hierarchy = Stream.concat(
                        ClassUtils.getAllSuperclasses(clazz).stream(),
                        ClassUtils.getAllInterfaces(clazz).stream())
                .collect(Collectors.toSet());

        for (String typeName : SUPPORTED_TYPES) {
            for (Class<?> classInHierarchy : hierarchy) {
                if (classInHierarchy.getName().equals(typeName)) {
                    return typeName;
                }
            }
        }

        return null;
    }
}