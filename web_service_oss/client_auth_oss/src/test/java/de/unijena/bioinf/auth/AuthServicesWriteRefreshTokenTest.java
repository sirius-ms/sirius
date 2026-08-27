package de.unijena.bioinf.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link AuthServices#writeRefreshToken} is called from a {@code finally} block in the CLI login tool, which
 * also runs after a login has failed. Without {@code ignoreMissing} it then throws about a token it could
 * never have had, and that error replaces the real one - wrong credentials (#666).
 */
public class AuthServicesWriteRefreshTokenTest {

    @TempDir
    Path tmp;

    private static AuthService loggedOut() {
        AuthService service = mock(AuthService.class);
        when(service.needsLogin()).thenReturn(true);
        return service;
    }

    private static AuthService loggedIn(String refreshToken) {
        AuthService service = mock(AuthService.class);
        when(service.needsLogin()).thenReturn(false);
        when(service.getRefreshToken()).thenReturn(refreshToken);
        return service;
    }

    /** the trap #666 fell into: the default overload reports a missing token as a failure */
    @Test
    public void notLoggedInThrowsByDefault() {
        Path file = tmp.resolve("token");

        assertThrows(LoginException.class, () -> AuthServices.writeRefreshToken(loggedOut(), file));
        assertFalse(Files.exists(file));
    }

    @Test
    public void notLoggedInIsQuietWhenMissingIsIgnored() throws IOException {
        Path file = tmp.resolve("token");

        AuthServices.writeRefreshToken(loggedOut(), file, true);

        assertFalse(Files.exists(file), "nothing to write, so nothing should be written");
    }

    @Test
    public void aLoggedInServiceStillWritesItsToken() throws IOException {
        Path file = tmp.resolve("token");

        AuthServices.writeRefreshToken(loggedIn("a-refresh-token"), file, true);

        assertEquals("a-refresh-token", AuthServices.readRefreshToken(file),
                "ignoreMissing must not stop a real token from being stored");
    }

    /** the client credentials flow has no refresh token; that is not an error either */
    @Test
    public void aLoggedInServiceWithoutATokenIsQuiet() throws IOException {
        Path file = tmp.resolve("token");

        AuthServices.writeRefreshToken(loggedIn(null), file, false);

        assertFalse(Files.exists(file));
    }
}
