package io.sirius.ms.utils.jwt;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.sirius.ms.utils.jwt.auth0.Auth0JwtToken;
import io.sirius.ms.utils.jwt.claims.ClaimKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.sirius.ms.utils.jwt.claims.StandardClaimKeys.ID_IMAGE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Auth0JwtToken Wrapper Tests")
class Auth0JwtTokenTest {

    @Mock
    private DecodedJWT mockDecodedJWT;

    @Mock
    private Claim mockClaim;

    private Auth0JwtToken tokenWrapper;

    // Helper to create a ClaimKey for testing
    private ClaimKey testKey(String key) {
        return () -> key;
    }

    @BeforeEach
    void setUp() {
        tokenWrapper = new Auth0JwtToken(mockDecodedJWT);
        // The general stubbing that caused the error has been removed from here.
        // Stubbings are now defined within each test that needs them.
    }

    @Test
    @DisplayName("getSubject should return subject from DecodedJWT")
    void getSubject_shouldReturnSubject() {
        // Arrange: This test only needs the getSubject stub.
        when(mockDecodedJWT.getSubject()).thenReturn("test-subject");
        
        // Act & Assert
        assertEquals("test-subject", tokenWrapper.getSubject());
    }

    @Test
    @DisplayName("getStringClaim should return string from claim")
    void getStringClaim_shouldReturnString() {
        // Arrange
        when(mockDecodedJWT.getClaim("some_key")).thenReturn(mockClaim);
        when(mockClaim.isNull()).thenReturn(false);
        when(mockClaim.asString()).thenReturn("claim-value");
        
        // Act
        Optional<String> result = tokenWrapper.getStringClaim(testKey("some_key"));
        
        // Assert
        assertTrue(result.isPresent());
        assertEquals("claim-value", result.get());
    }

    @Test
    @DisplayName("getBooleanClaim should return boolean from claim")
    void getBooleanClaim_shouldReturnBoolean() {
        // Arrange
        when(mockDecodedJWT.getClaim("some_key")).thenReturn(mockClaim);
        when(mockClaim.isNull()).thenReturn(false);
        when(mockClaim.asBoolean()).thenReturn(true);

        // Act
        Optional<Boolean> result = tokenWrapper.getBooleanClaim(testKey("some_key"));
        
        // Assert
        assertTrue(result.isPresent());
        assertTrue(result.get());
    }

    @Test
    @DisplayName("getInstantClaim should return Instant from claim")
    void getInstantClaim_shouldReturnInstant() {
        // Arrange
        Instant now = Instant.now();
        when(mockDecodedJWT.getClaim("some_key")).thenReturn(mockClaim);
        when(mockClaim.isNull()).thenReturn(false);
        when(mockClaim.asDate()).thenReturn(Date.from(now));

        // Act
        Optional<Instant> result = tokenWrapper.getInstantClaim(testKey("some_key"));
        
        // Assert
        assertTrue(result.isPresent());
        assertEquals(now.getEpochSecond(), result.get().getEpochSecond());
    }

    @Test
    @DisplayName("getMapClaim should return Map from claim")
    void getMapClaim_shouldReturnMap() {
        // Arrange
        Map<String, Object> map = Map.of("key", "value");
        when(mockDecodedJWT.getClaim("some_key")).thenReturn(mockClaim);
        when(mockClaim.isNull()).thenReturn(false);
        when(mockClaim.asMap()).thenReturn(map);
        
        // Act
        Optional<Map<String, Object>> result = tokenWrapper.getMapClaim(testKey("some_key"));
        
        // Assert
        assertTrue(result.isPresent());
        assertEquals(map, result.get());
    }
    
    @Test
    @DisplayName("getStringListClaim should return List<String> from claim")
    void getStringListClaim_shouldReturnList() {
        // Arrange
        List<String> list = List.of("role1", "role2");
        when(mockDecodedJWT.getClaim("some_key")).thenReturn(mockClaim);
        when(mockClaim.isNull()).thenReturn(false);
        when(mockClaim.asList(String.class)).thenReturn(list);
        
        // Act
        Optional<List<String>> result = tokenWrapper.getStringListClaim(testKey("some_key"));
        
        // Assert
        assertTrue(result.isPresent());
        assertEquals(list, result.get());
    }

    @Test
    @DisplayName("getClaim should return empty Optional if claim does not exist")
    void getClaim_whenClaimIsNull_shouldReturnEmpty() {
        // Arrange: Simulate the claim not being present by returning null.
        when(mockDecodedJWT.getClaim("null_claim")).thenReturn(null);
        
        // Act
        Optional<String> result = tokenWrapper.getStringClaim(testKey("null_claim"));
        
        // Assert
        assertFalse(result.isPresent());
    }
    
    @Test
    @DisplayName("getClaim should return empty Optional if claim.isNull() is true")
    void getClaim_whenClaimIsExplicitlyNull_shouldReturnEmpty() {
        // Arrange: Simulate a claim that exists but its value is null.
        when(mockDecodedJWT.getClaim("is_null_claim")).thenReturn(mockClaim);
        when(mockClaim.isNull()).thenReturn(true);
        
        // Act
        Optional<String> result = tokenWrapper.getStringClaim(testKey("is_null_claim"));
        
        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("getPicture should call getStringClaim with ID_IMAGE key")
    void getPicture_shouldUseCorrectClaimKey() {
        // Arrange
        String imageUrl = "http://example.com/pic.jpg";
        when(mockDecodedJWT.getClaim(ID_IMAGE.getKey())).thenReturn(mockClaim);
        when(mockClaim.isNull()).thenReturn(false);
        when(mockClaim.asString()).thenReturn(imageUrl);
        
        // Act
        var pictureUri = tokenWrapper.getPicture();

        // Assert
        assertNotNull(pictureUri);
        assertEquals(imageUrl, pictureUri.toString());
    }
}