package io.sirius.ms.utils.jwt;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface JwtTokenFactory {

    /**
     * Creates a Token wrapper for a given JWT object.
     * This method safely checks for available JWT implementations without causing a NoClassDefFoundError.
     *
     * @param jwt The JWT object from a specific library (e.g., Spring Security or Auth0).
     * @return A Token instance.
     * @throws IllegalArgumentException if the JWT type is not supported or its library is not on the classpath.
     */
    @NotNull
    TokenWrapper create(@NotNull Object jwt, TokenType tokenType);

    default TokenWrapper create(@NotNull Object jwt) {
        return create(jwt, TokenType.ACCESS_TOKEN);
    }
}
