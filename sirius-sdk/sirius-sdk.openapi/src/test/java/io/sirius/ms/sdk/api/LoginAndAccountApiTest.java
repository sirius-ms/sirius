package io.sirius.ms.sdk.api;

import io.sirius.ms.sdk.model.AccountCredentials;
import io.sirius.ms.sdk.model.AccountInfo;
import io.sirius.ms.sdk.model.Subscription;
import org.junit.FixMethodOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.runners.MethodSorters;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LoginAndAccountApiTest {

    private LoginAndAccountApi instance;

    @BeforeEach
    public void setUp() {
        instance = TestSetup.getInstance().getSiriusClient().account();
        TestSetup.getInstance().loginIfNeeded();
    }


    @Test
    public void instanceTest() {
        assertNotNull(instance);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void getAccountInfoTest(boolean includeSubs) {
        TestSetup.getInstance().loginIfNeeded();
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
        TestSetup.getInstance().loginIfNeeded();
        List<Subscription> response = instance.getSubscriptions();
        assertNotNull(response);
        assertFalse(response.isEmpty());
    }

    @Test
    public void isLoggedInTest() {
        TestSetup.getInstance().loginIfNeeded();
        boolean response = instance.isLoggedIn();
        assertTrue(response);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false})
    public void loginTest( boolean includeSubs) {
        boolean acceptTerms = true;
        AccountCredentials accountCredentials = new AccountCredentials().username(TestSetup.getInstance().getSIRIUS_USER_ENV()).password(TestSetup.getInstance().getSIRIUS_PW_ENV());
        boolean failWhenLoggedIn = false;

        try {
            instance.logout();
            AccountInfo response = instance.login(acceptTerms, accountCredentials, failWhenLoggedIn, includeSubs);
            assertNotNull(response);
        } finally {
            TestSetup.getInstance().loginIfNeeded();
        }
    }

    @Test
    public void logoutTest() {
        try {
            TestSetup.getInstance().loginIfNeeded();
            assertTrue(instance.isLoggedIn());
            instance.logout();
            assertFalse(instance.isLoggedIn());
        } finally {
            TestSetup.getInstance().loginIfNeeded();
        }
    }

    @Test
    public void selectSubscriptionTest() {
            TestSetup.getInstance().loginIfNeeded();

            assertTrue(instance.isLoggedIn());
            AccountInfo response = instance.getAccountInfo(true);
            System.out.println("Subscription before: " + response.getActiveSubscriptionId());

            String sid = TestSetup.getInstance().getSIRIUS_ACTIVE_SUB();
            System.out.println("Subscription to change to: " + sid);
            instance.selectSubscription(sid);

            response = instance.getAccountInfo(true);
            System.out.println("Subscription after: " + response.getActiveSubscriptionId());

            assertEquals(sid, response.getActiveSubscriptionId());
    }
}
