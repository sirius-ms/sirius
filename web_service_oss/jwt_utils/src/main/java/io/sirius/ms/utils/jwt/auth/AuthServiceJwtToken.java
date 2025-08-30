package io.sirius.ms.utils.jwt.auth;

import de.unijena.bioinf.auth.AuthService;
import io.sirius.ms.utils.jwt.TokenType;
import io.sirius.ms.utils.jwt.auth0.Auth0JwtToken;

public class AuthServiceJwtToken extends Auth0JwtToken {
    public AuthServiceJwtToken(Object token, TokenType tokenType) {
        this((AuthService.Token) token, tokenType);
    }

    public AuthServiceJwtToken(AuthService.Token token, TokenType tokenType) {
        super(tokenType == TokenType.ID_TOKEN ? token.getDecodedIdToken() : token.getDecodedAccessToken());
    }
}
