package io.sirius.ms.sdk.api;

import io.sirius.ms.sdk.model.AccountInfo;
import io.sirius.ms.sdk.model.Subscription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Read-only account tests. Tests that mutate the account state (login, logout, subscription switching)
 * live in {@link AccountStateApiTest} and run against a dedicated, isolated SIRIUS instance.
 */
public class LoginAndAccountApiTest {

    private LoginAndAccountApi instance;

    @BeforeEach
    public void setUp() {
        instance = TestSetup.getInstance().getSiriusClient().account();
    }

    @Test
    public void instanceTest() {
        assertNotNull(instance);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void getAccountInfoTest(boolean includeSubs) {
        AccountInfo response = instance.getAccountInfo(includeSubs);
        assertNotNull(response);
        assertEquals(response.getSubscriptions() != null, includeSubs);
    }

    @Test
    public void getSignUpURLTest() {
        String response = instance.getSignUpURL();
        assertNotNull(response);
    }

    @Test
    public void getSubscriptionsTest() {
        List<Subscription> response = instance.getSubscriptions();
        assertNotNull(response);
        assertFalse(response.isEmpty());
    }

    @Test
    public void isLoggedInTest() {
        boolean response = instance.isLoggedIn();
        assertTrue(response);
    }
}
