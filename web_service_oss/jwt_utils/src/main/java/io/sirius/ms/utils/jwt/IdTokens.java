package io.sirius.ms.utils.jwt;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.Optional;

import static io.sirius.ms.utils.jwt.claims.StandardClaimKeys.*;

public class IdTokens extends Tokens {
    public static final IdTokens ID_TOKENS = new IdTokens();

    public IdTokens() {
        super();
    }

    public IdTokens(JwtTokenFactory tokenFactory) {
        super(tokenFactory);
    }

    @Override
    protected Optional<TokenWrapper> createToken(@Nullable Object jwt) {
        return createToken(jwt, TokenType.ID_TOKEN);
    }

    @NotNull
    public Optional<String> getUserId(@Nullable Object jwt) {
        return createToken(jwt).map(TokenWrapper::getSubject);
    }

    @NotNull
    public Optional<String> getUserEmail(@Nullable Object jwt) {
        return createToken(jwt).flatMap(t -> t.getStringClaim(ID_USER_EMAIL));
    }

    public boolean isUserEmailVerified(@Nullable Object jwt) {
        // Now uses the namespaced key from Tokens.java
        return createToken(jwt).flatMap(t -> t.getBooleanClaim(ID_USER_EMAIL_VERIFIED)).orElse(false);
    }

    @NotNull
    public Optional<String> getUsername(@Nullable Object jwt) {
        return createToken(jwt).flatMap(t -> t.getStringClaim(ID_USER_NAME));
    }

    @NotNull
    public Optional<URI> getUserImage(@Nullable Object jwt) {
        return createToken(jwt).flatMap(t -> t.getStringClaim(ID_IMAGE)).map(URI::create);
    }

}
