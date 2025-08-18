// StandardClaimKeys.java
package io.sirius.ms.utils.jwt.claims;

/**
 * Defines standard and custom JWT claim keys used across the application.
 */
public enum StandardClaimKeys implements ClaimKey {
    // Standard Claims
    SUBJECT("sub"),
    ISSUER("iss"),
    EXPIRES_AT("exp"),

    // Custom Namespaced Claims
    USER_NAME("https://bright-giant.com/name"),
    USER_EMAIL("https://bright-giant.com/email"),
    USER_EMAIL_VERIFIED("https://bright-giant.com/email_verified"),
    USER_ROLES("https://bright-giant.com/roles"),

    APP_METADATA("https://bright-giant.com/app_metadata"),
    USER_METADATA("https://bright-giant.com/user_metadata"),
    LICENSE_DATA("https://bright-giant.com/licenseData"),

    // Nested Keys within Custom Claims
    USER_METADATA__DEFAULT_SUBSCRIPTION("defaultSubscription"),
    APP_METADATA__ACCEPTED_TERMS("acceptedTerms"),
    APP_META_DATA__PROTECTED("protected"),
    LICENSE_DATA__SUBSCRIPTIONS("subscriptions"),

    // ID token claims
    ID_USER_NAME("name"),
    ID_USER_EMAIL("email"),
    ID_USER_EMAIL_VERIFIED("email_verified"),
    ID_GIVEN_NAME("given_name"),
    ID_FAMILY_NAME("family_name"),
    ID_IMAGE("picture");

    private final String key;

    StandardClaimKeys(String key) {
        this.key = key;
    }

    @Override
    public String getKey() {
        return this.key;
    }
}