package io.sirius.ms.utils.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unijena.bioinf.auth.AuthService;
import io.sirius.ms.utils.jwt.auth.AuthServiceJwtToken;
import io.sirius.ms.utils.jwt.auth0.Auth0JwtToken;
import io.sirius.ms.utils.jwt.spring.SpringJwtToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtTokenFactoryImpl Tests")
class JwtTokenFactoryImplTest {

    // A minimal but valid JWT structure (header.payload.signature)
    private static final String DUMMY_JWT_STRING = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0In0.x";

    @Mock
    private Jwt springJwt;

    // Do NOT mock DecodedJWT because the factory checks its concrete class name.
    // Instead, we will create a real instance by decoding a dummy token string.

    @Mock
    private AuthService.Token authServiceToken;

    private JwtTokenFactoryImpl factory;

    @BeforeEach
    void setUp() {
        factory = new JwtTokenFactoryImpl(new ObjectMapper());
    }

    @Test
    @DisplayName("Should create SpringJwtToken for Spring's Jwt")
    void create_whenSpringJwt_shouldReturnSpringJwtToken() {
        // Act
        TokenWrapper wrapper = factory.create(springJwt, TokenType.ACCESS_TOKEN);

        // Assert
        assertNotNull(wrapper);
        assertInstanceOf(SpringJwtToken.class, wrapper, "Wrapper should be an instance of SpringJwtToken");
    }

    @Test
    @DisplayName("Should create Auth0JwtToken for Auth0's DecodedJWT")
    void create_whenAuth0Jwt_shouldReturnAuth0JwtToken() {
        // Arrange: Create a REAL DecodedJWT instance.
        DecodedJWT realAuth0Jwt = JWT.decode(DUMMY_JWT_STRING);

        // Act
        TokenWrapper wrapper = factory.create((DecodedJWT)realAuth0Jwt, TokenType.ACCESS_TOKEN);

        // Assert
        assertNotNull(wrapper);
        assertInstanceOf(Auth0JwtToken.class, wrapper, "Wrapper should be an instance of Auth0JwtToken");
    }

    @Test
    @DisplayName("Should create AuthServiceJwtToken for AuthService.Token")
    void create_whenAuthServiceToken_shouldReturnAuthServiceJwtToken() {
        // Arrange
        DecodedJWT realAuth0Jwt = JWT.decode(DUMMY_JWT_STRING);
        when(authServiceToken.getDecodedAccessToken()).thenReturn(realAuth0Jwt);

        // Act
        TokenWrapper wrapper = factory.create(authServiceToken, TokenType.ACCESS_TOKEN);

        // Assert
        assertNotNull(wrapper);
        assertInstanceOf(AuthServiceJwtToken.class, wrapper, "Wrapper should be an instance of AuthServiceJwtToken");
    }

    @Test
    @DisplayName("Should create AuthServiceJwtToken for ID Token")
    void create_whenAuthServiceTokenAsIdToken_shouldUseIdToken() {
        // Arrange
        DecodedJWT realIdToken = JWT.decode(DUMMY_JWT_STRING);
        when(authServiceToken.getDecodedIdToken()).thenReturn(realIdToken);

        // Act
        TokenWrapper wrapper = factory.create(authServiceToken, TokenType.ID_TOKEN);

        // Assert
        assertNotNull(wrapper);
        assertInstanceOf(AuthServiceJwtToken.class, wrapper);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for unsupported JWT type")
    void create_whenUnsupportedJwt_shouldThrowException() {
        // Arrange
        Object unsupportedJwt = new Object();

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            factory.create(unsupportedJwt, TokenType.ACCESS_TOKEN);
        });

        assertEquals("Unsupported JWT type: java.lang.Object", exception.getMessage());
    }

    @Test
    @DisplayName("Default create method should assume ACCESS_TOKEN")
    void create_defaultMethod_shouldUseAccessToken() {
        // Act
        TokenWrapper wrapper = factory.create(springJwt);

        // Assert
        assertNotNull(wrapper);
        assertInstanceOf(SpringJwtToken.class, wrapper);
    }
}