package io.sirius.ms.sdk.api;

import io.sirius.ms.sdk.model.AccountCredentials;
import io.sirius.ms.sdk.model.AccountInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that mutate the server-global account state (login, logout, subscription switching).
 * They run against a dedicated SIRIUS instance with an isolated workspace (see {@link AccountTestSetup})
 * and are excluded from the regular integration test suite via the 'account-state' tag, so they can
 * never corrupt the login state shared by all other API tests.
 * <p>
 * Each test establishes its own precondition and restores what it changed, so the tests are order-independent.
 */
@Tag("account-state")
public class AccountStateApiTest {

    private AccountTestSetup setup;
    private LoginAndAccountApi instance;

    @BeforeEach
    public void setUp() {
        setup = AccountTestSetup.getInstance(); // skips (assumption) if another SIRIUS instance is running
        instance = setup.getSiriusClient().account();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false})
    public void loginTest(boolean includeSubs) {
        boolean acceptTerms = true;
        AccountCredentials accountCredentials = new AccountCredentials()
                .username(setup.getSIRIUS_USER_ENV())
                .password(setup.getSIRIUS_PW_ENV());
        boolean failWhenLoggedIn = false;

        instance.logout();
        assertFalse(instance.isLoggedIn());

        AccountInfo response = instance.login(acceptTerms, accountCredentials, failWhenLoggedIn, includeSubs);
        assertNotNull(response);
        assertTrue(instance.isLoggedIn());
    }

    @Test
    public void logoutTest() {
        setup.ensureLoggedIn();
        assertTrue(instance.isLoggedIn());

        instance.logout();
        assertFalse(instance.isLoggedIn());
    }

    @Test
    public void selectSubscriptionTest() {
        setup.ensureLoggedIn();
        assertTrue(instance.isLoggedIn());

        String originalSid = instance.getAccountInfo(true).getActiveSubscriptionId();
        String sid = setup.getSIRIUS_ACTIVE_SUB();
        try {
            instance.selectSubscription(sid);
            assertEquals(sid, instance.getAccountInfo(true).getActiveSubscriptionId());
        } finally {
            if (originalSid != null && !originalSid.equals(sid))
                instance.selectSubscription(originalSid);
        }
    }
}
