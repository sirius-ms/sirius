package io.sirius.ms.utils.jwt;

import de.unijena.bioinf.ms.rest.model.license.Subscription;
import de.unijena.bioinf.ms.rest.model.license.SubscriptionData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.sirius.ms.utils.jwt.claims.StandardClaimKeys.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
@DisplayName("AccessTokens Utility Tests")
class AccessTokensTest {

    @Mock
    private JwtTokenFactory mockFactory;

    @Mock
    private TokenWrapper mockTokenWrapper;
    
    private AccessTokens accessTokens;
    private final Object mockJwt = new Object(); // Dummy JWT object

    @BeforeEach
    void setUp() {
        accessTokens = new AccessTokens(mockFactory);
        // Lenient stubbing because not all tests use the factory directly
        lenient().when(mockFactory.create(any(Object.class), eq(TokenType.ACCESS_TOKEN))).thenReturn(mockTokenWrapper);
        lenient().when(mockFactory.create(any(Object.class))).thenReturn(mockTokenWrapper);
    }
    
    @Test
    @DisplayName("Should create token with ACCESS_TOKEN type")
    void createToken_shouldUseAccessTokenType() {
        Optional<TokenWrapper> wrapper = accessTokens.createToken(mockJwt);
        assertTrue(wrapper.isPresent());
        assertEquals(mockTokenWrapper, wrapper.get());
    }

    @Nested
    @DisplayName("Core User Attribute Extraction")
    class UserAttributeTests {

        @Test
        @DisplayName("getUserId should return subject")
        void getUserId_shouldReturnSubject() {
            when(mockTokenWrapper.getSubject()).thenReturn("user-456");
            assertEquals(Optional.of("user-456"), accessTokens.getUserId(mockJwt));
        }

        @Test
        @DisplayName("getUserEmail should return email claim")
        void getUserEmail_shouldReturnEmail() {
            when(mockTokenWrapper.getStringClaim(USER_EMAIL)).thenReturn(Optional.of("access@example.com"));
            assertEquals(Optional.of("access@example.com"), accessTokens.getUserEmail(mockJwt));
        }

        @Test
        @DisplayName("isUserEmailVerified should return true when claim is true")
        void isUserEmailVerified_whenTrue_shouldReturnTrue() {
            when(mockTokenWrapper.getBooleanClaim(USER_EMAIL_VERIFIED)).thenReturn(Optional.of(true));
            assertTrue(accessTokens.isUserEmailVerified(mockJwt));
        }

        @Test
        @DisplayName("isUserEmailVerified should return false when claim is missing")
        void isUserEmailVerified_whenMissing_shouldReturnFalse() {
            when(mockTokenWrapper.getBooleanClaim(USER_EMAIL_VERIFIED)).thenReturn(Optional.empty());
            assertFalse(accessTokens.isUserEmailVerified(mockJwt));
        }
    }

    @Nested
    @DisplayName("Metadata Extraction")
    class MetadataTests {

        @Test
        @DisplayName("getAppMetadata should return map claim")
        void getAppMetadata_shouldReturnMap() {
            Map<String, Object> metadata = Map.of("key", "value");
            when(mockTokenWrapper.getMapClaim(APP_METADATA)).thenReturn(Optional.of(metadata));
            assertEquals(Optional.of(metadata), accessTokens.getAppMetadata(mockJwt));
        }

        @Test
        @DisplayName("getUserMetadata should return map claim")
        void getUserMetadata_shouldReturnMap() {
            Map<String, Object> metadata = Map.of("setting", "enabled");
            when(mockTokenWrapper.getMapClaim(USER_METADATA)).thenReturn(Optional.of(metadata));
            assertEquals(Optional.of(metadata), accessTokens.getUserMetadata(mockJwt));
        }
        
        @Test
        @DisplayName("isUserAccountProtected should return true from app metadata")
        void isUserAccountProtected_whenTrue_shouldReturnTrue() {
            Map<String, Object> metadata = Map.of(APP_META_DATA__PROTECTED.getKey(), true);
            when(mockTokenWrapper.getMapClaim(APP_METADATA)).thenReturn(Optional.of(metadata));
            assertEquals(Optional.of(true), accessTokens.isUserAccountProtected(mockJwt));
        }

        @Test
        @DisplayName("isUserAccountProtected should return false when missing from app metadata")
        void isUserAccountProtected_whenMissing_shouldReturnFalse() {
             Map<String, Object> metadata = Map.of("other_key", "value");
            when(mockTokenWrapper.getMapClaim(APP_METADATA)).thenReturn(Optional.of(metadata));
            assertEquals(Optional.of(false), accessTokens.isUserAccountProtected(mockJwt));
        }
        
        @Test
        @DisplayName("isUserAccountProtected should return empty when app metadata is missing")
        void isUserAccountProtected_whenMetadataMissing_shouldReturnEmpty() {
            when(mockTokenWrapper.getMapClaim(APP_METADATA)).thenReturn(Optional.empty());
            assertTrue(accessTokens.isUserAccountProtected(mockJwt).isEmpty());
        }
    }

    @Nested
    @DisplayName("Subscription and License Logic")
    class SubscriptionTests {
        private Subscription sub1, sub2, sub3;

        @BeforeEach
        void setUp() {
            sub1 = Subscription.builder().sid("sub-1").build();
            sub2 = Subscription.builder().sid("sub-2").build();
            sub3 = Subscription.builder().sid("sub-3").build();
        }
        
        @Test
        @DisplayName("getSubscriptions should extract subscriptions from SubscriptionData")
        void getSubscriptions_shouldReturnListOfSubscriptions() {
            SubscriptionData data = new SubscriptionData();
            data.setSubscriptions(List.of(sub1, sub2));
            when(mockTokenWrapper.getClaimAs(LICENSE_DATA, SubscriptionData.class)).thenReturn(Optional.of(data));

            List<Subscription> subs = accessTokens.getSubscriptions(mockJwt);
            assertEquals(2, subs.size());
            assertEquals("sub-1", subs.get(0).getSid());
        }

        @Test
        @DisplayName("getSubscriptions should return empty list if data is missing")
        void getSubscriptions_whenDataMissing_shouldReturnEmptyList() {
            when(mockTokenWrapper.getClaimAs(LICENSE_DATA, SubscriptionData.class)).thenReturn(Optional.empty());
            assertTrue(accessTokens.getSubscriptions(mockJwt).isEmpty());
        }

        @Test
        @DisplayName("hasSubscriptions should return true when subscriptions exist")
        void hasSubscriptions_whenPresent_shouldReturnTrue() {
            SubscriptionData data = new SubscriptionData();
            data.setSubscriptions(List.of(sub1));
            when(mockTokenWrapper.getClaimAs(LICENSE_DATA, SubscriptionData.class)).thenReturn(Optional.of(data));
            assertTrue(accessTokens.hasSubscriptions(mockJwt));
        }

        @Test
        @DisplayName("hasSubscriptions should return false when no subscriptions exist")
        void hasSubscriptions_whenEmpty_shouldReturnFalse() {
             when(mockTokenWrapper.getClaimAs(LICENSE_DATA, SubscriptionData.class)).thenReturn(Optional.empty());
             assertFalse(accessTokens.hasSubscriptions(mockJwt));
        }
        
        @Test
        @DisplayName("getDefaultSubscriptionId should get ID from user metadata")
        void getDefaultSubscriptionId_shouldReturnId() {
            Map<String, Object> metadata = Map.of(USER_METADATA__DEFAULT_SUBSCRIPTION.getKey(), "default-sub");
            when(mockTokenWrapper.getMapClaim(USER_METADATA)).thenReturn(Optional.of(metadata));
            assertEquals("default-sub", accessTokens.getDefaultSubscriptionId(mockJwt));
        }

        @Test
        @DisplayName("getActiveSubscription should find by SID first")
        void getActiveSubscription_shouldFindBySid() {
            Subscription activeSub = accessTokens.getActiveSubscription(List.of(sub1, sub2, sub3), "sub-2", "sub-3", true);
            assertEquals(sub2, activeSub);
        }

        @Test
        @DisplayName("getActiveSubscription should find by default SID if SID is not found")
        void getActiveSubscription_shouldFindByDefaultSid() {
            Subscription activeSub = accessTokens.getActiveSubscription(List.of(sub1, sub2, sub3), "sub-nonexistent", "sub-3", true);
            assertEquals(sub3, activeSub);
        }

        @Test
        @DisplayName("getActiveSubscription should find by fallback if SID and default are not found")
        void getActiveSubscription_shouldUseFallback() {
            Subscription activeSub = accessTokens.getActiveSubscription(List.of(sub1, sub2, sub3), null, null, true);
            assertEquals(sub1, activeSub);
        }

        @Test
        @DisplayName("getActiveSubscription should return null if fallback is disabled and no match")
        void getActiveSubscription_shouldReturnNullWithNoFallback() {
             Subscription activeSub = accessTokens.getActiveSubscription(List.of(sub1, sub2, sub3), "sub-x", "sub-y", false);
            assertNull(activeSub);
        }

        @Test
        @DisplayName("getActiveSubscription should return null for empty subscription list")
        void getActiveSubscription_withEmptyList_shouldReturnNull() {
            Subscription activeSub = accessTokens.getActiveSubscription(Collections.emptyList(), "sub-x", "sub-y", true);
            assertNull(activeSub);
        }
    }

    @Nested
    @DisplayName("Token State Logic")
    class TokenStateTests {
        
        @Test
        @DisplayName("isExpired should return true for past expiration date")
        void isExpired_whenPastDate_shouldReturnTrue() {
            Instant expiredTime = Instant.now().minus(1, ChronoUnit.HOURS);
            when(mockTokenWrapper.getInstantClaim(EXPIRES_AT)).thenReturn(Optional.of(expiredTime));
            assertTrue(accessTokens.isExpired(mockJwt));
        }

        @Test
        @DisplayName("isExpired should return false for future expiration date")
        void isExpired_whenFutureDate_shouldReturnFalse() {
            Instant futureTime = Instant.now().plus(1, ChronoUnit.HOURS);
            when(mockTokenWrapper.getInstantClaim(EXPIRES_AT)).thenReturn(Optional.of(futureTime));
            assertFalse(accessTokens.isExpired(mockJwt));
        }

        @Test
        @DisplayName("isExpired should return true if expiration claim is missing")
        void isExpired_whenClaimMissing_shouldReturnTrue() {
            when(mockTokenWrapper.getInstantClaim(EXPIRES_AT)).thenReturn(Optional.empty());
            assertTrue(accessTokens.isExpired(mockJwt));
        }
        
        @Test
        @DisplayName("isExpired should respect the buffer")
        void isExpired_shouldRespectBuffer() {
            // Token expires in 5 seconds
            Instant futureTime = Instant.now().plus(5, ChronoUnit.SECONDS);
            when(mockTokenWrapper.getInstantClaim(EXPIRES_AT)).thenReturn(Optional.of(futureTime));
            
            // With a 10 second buffer, it should be considered expired
            assertTrue(accessTokens.isExpired(mockJwt, 10, ChronoUnit.SECONDS));
            
            // With a 2 second buffer, it should not be considered expired
            assertFalse(accessTokens.isExpired(mockJwt, 2, ChronoUnit.SECONDS));
        }
    }
}