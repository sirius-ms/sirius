package io.sirius.ms.utils.jwt;

import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public abstract class Tokens {

    protected final JwtTokenFactory tokenFactory;

    protected Tokens() {
        this(new JwtTokenFactoryImpl());
    }
    protected Tokens(JwtTokenFactory tokenFactory) {
        this.tokenFactory = tokenFactory;
    }

    protected abstract Optional<TokenWrapper> createToken(@Nullable Object jwt);

    protected Optional<TokenWrapper> createToken(@Nullable Object jwt, TokenType tokenType) {
        if (jwt == null)
            return Optional.empty();
        return Optional.of(tokenFactory.create(jwt, tokenType));
    }
}
