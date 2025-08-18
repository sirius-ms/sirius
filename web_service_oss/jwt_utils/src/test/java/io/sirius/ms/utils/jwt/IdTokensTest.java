package io.sirius.ms.utils.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.Optional;

import static io.sirius.ms.utils.jwt.claims.StandardClaimKeys.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("IdTokens Utility Tests")
class IdTokensTest {

    @Mock
    private JwtTokenFactory mockFactory;

    @Mock
    private TokenWrapper mockTokenWrapper;

    private IdTokens idTokens;
    private final Object mockJwt = new Object(); // Dummy JWT object

    @BeforeEach
    void setUp() {
        idTokens = new IdTokens(mockFactory);
        // Use lenient stubbing as not every single test path will invoke the factory,
        // especially the one testing for a null JWT.
        lenient().when(mockFactory.create(any(Object.class), eq(TokenType.ID_TOKEN))).thenReturn(mockTokenWrapper);
    }

    @Test
    @DisplayName("Should create token with ID_TOKEN type")
    void createToken_shouldUseIdTokenType() {
        // This test verifies the abstract method implementation
        Optional<TokenWrapper> wrapper = idTokens.createToken(mockJwt);
        assertTrue(wrapper.isPresent());
        assertEquals(mockTokenWrapper, wrapper.get());
    }

    @Nested
    @DisplayName("Claim Extraction Tests")
    class ClaimExtractionTests {

        @Test
        @DisplayName("getUserId should return subject from token")
        void getUserId_shouldReturnSubject() {
            when(mockTokenWrapper.getSubject()).thenReturn("user-123");
            Optional<String> userId = idTokens.getUserId(mockJwt);
            assertTrue(userId.isPresent());
            assertEquals("user-123", userId.get());
        }

        @Test
        @DisplayName("getUserEmail should return email claim")
        void getUserEmail_shouldReturnEmail() {
            when(mockTokenWrapper.getStringClaim(ID_USER_EMAIL)).thenReturn(Optional.of("test@example.com"));
            Optional<String> email = idTokens.getUserEmail(mockJwt);
            assertTrue(email.isPresent());
            assertEquals("test@example.com", email.get());
        }

        @Test
        @DisplayName("getUsername should return name claim")
        void getUsername_shouldReturnName() {
            when(mockTokenWrapper.getStringClaim(ID_USER_NAME)).thenReturn(Optional.of("Test User"));
            Optional<String> username = idTokens.getUsername(mockJwt);
            assertTrue(username.isPresent());
            assertEquals("Test User", username.get());
        }

        @Test
        @DisplayName("getUserImage should return picture claim as URI")
        void getUserImage_shouldReturnPictureUri() {
            String imageUrl = "https://example.com/image.png";
            when(mockTokenWrapper.getStringClaim(ID_IMAGE)).thenReturn(Optional.of(imageUrl));
            Optional<URI> userImage = idTokens.getUserImage(mockJwt);
            assertTrue(userImage.isPresent());
            assertEquals(URI.create(imageUrl), userImage.get());
        }

        @Test
        @DisplayName("isUserEmailVerified should return true when claim is true")
        void isUserEmailVerified_whenTrue_shouldReturnTrue() {
            when(mockTokenWrapper.getBooleanClaim(ID_USER_EMAIL_VERIFIED)).thenReturn(Optional.of(true));
            assertTrue(idTokens.isUserEmailVerified(mockJwt));
        }

        @Test
        @DisplayName("isUserEmailVerified should return false when claim is false")
        void isUserEmailVerified_whenFalse_shouldReturnFalse() {
            when(mockTokenWrapper.getBooleanClaim(ID_USER_EMAIL_VERIFIED)).thenReturn(Optional.of(false));
            assertFalse(idTokens.isUserEmailVerified(mockJwt));
        }
    }

    @Nested
    @DisplayName("Edge Case and Empty Claim Tests")
    class EdgeCaseTests {
        
        @Test
        @DisplayName("All methods should return empty Optional or false for a null JWT")
        void methods_whenJwtIsNull_shouldReturnEmpty() {
            // Arrange: No arrangement needed, we are testing the null-handling logic.
            // DO NOT call thenCallRealMethod on a mock interface.
            
            // Act & Assert
            assertTrue(idTokens.getUserId(null).isEmpty(), "getUserId should be empty");
            assertTrue(idTokens.getUserEmail(null).isEmpty(), "getUserEmail should be empty");
            assertTrue(idTokens.getUsername(null).isEmpty(), "getUsername should be empty");
            assertTrue(idTokens.getUserImage(null).isEmpty(), "getUserImage should be empty");
            assertFalse(idTokens.isUserEmailVerified(null), "isUserEmailVerified should be false");
        }

        @Test
        @DisplayName("Getters should return empty Optional when claims are missing")
        void getters_whenClaimsMissing_shouldReturnEmpty() {
            // Arrange: Stub only the specific methods needed for this test.
            when(mockTokenWrapper.getSubject()).thenReturn(null);
            when(mockTokenWrapper.getStringClaim(ID_USER_EMAIL)).thenReturn(Optional.empty());
            when(mockTokenWrapper.getStringClaim(ID_USER_NAME)).thenReturn(Optional.empty());
            when(mockTokenWrapper.getStringClaim(ID_IMAGE)).thenReturn(Optional.empty());

            // Act & Assert
            assertTrue(idTokens.getUserId(mockJwt).isEmpty());
            assertTrue(idTokens.getUserEmail(mockJwt).isEmpty());
            assertTrue(idTokens.getUsername(mockJwt).isEmpty());
            assertTrue(idTokens.getUserImage(mockJwt).isEmpty());
        }

        @Test
        @DisplayName("isUserEmailVerified should return false when claim is missing")
        void isUserEmailVerified_whenClaimMissing_shouldReturnFalse() {
            // Arrange: Stub only the specific method needed for this test.
            when(mockTokenWrapper.getBooleanClaim(ID_USER_EMAIL_VERIFIED)).thenReturn(Optional.empty());
            
            // Act & Assert
            assertFalse(idTokens.isUserEmailVerified(mockJwt));
        }
    }
}