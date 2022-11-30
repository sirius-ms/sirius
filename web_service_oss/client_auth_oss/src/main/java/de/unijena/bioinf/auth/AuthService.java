/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.github.scribejava.apis.auth0.Auth0Service;
import com.github.scribejava.apis.openid.OpenIdOAuth2AccessToken;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.github.scribejava.core.revoke.TokenTypeHint;
import com.github.scribejava.httpclient.apache.ApacheHttpClient;
import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.core5.annotation.Contract;
import org.apache.hc.core5.annotation.ThreadingBehavior;
import org.apache.hc.core5.http.Header;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class AuthService implements IOFunctions.IOConsumer<HttpUriRequest>, Closeable {
    private OAuth20Service service;

    @Nullable
    private String refreshToken;
    @Nullable
    private Token token;


    protected final ReadWriteLock tokenLock = new ReentrantReadWriteLock();

    private int minLifetime = 900000;


    public AuthService(@NotNull DefaultApi20 authAPI, @NotNull String clientID) {
        this(authAPI, clientID, null);
    }

    public AuthService(@NotNull DefaultApi20 authAPI, @NotNull String clientID, @Nullable CloseableHttpAsyncClient client) {
        this(authAPI, clientID, null, null, client);
    }

    public AuthService(@NotNull DefaultApi20 authAPI, @NotNull String clientID, @Nullable String clientSecret, @Nullable String refreshToken) {
        this(authAPI, clientID, clientSecret, refreshToken, null);
    }

    public AuthService(@NotNull DefaultApi20 authAPI, @NotNull String clientID, @Nullable String clientSecret, @Nullable String refreshToken, @Nullable CloseableHttpAsyncClient client) {
        this(buildService(authAPI, clientID, clientSecret, client), refreshToken);
    }

    public AuthService(@NotNull OAuth20Service service, @Nullable String refreshToken) {
        this.service = service;
        tokenLock.writeLock().lock();
        try {
            if (refreshToken != null)
                setTokens(requestAccessTokenRefreshFlow(refreshToken));
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("Error when using given refresh_token. Not logged in. Re-login with PW Flow might be necessary", e);
            logout();
        } finally {
            tokenLock.writeLock().unlock();
        }
    }


    private void setTokens(@Nullable Token token) {
        if (token != null) {
            if (token.getSource().getRefreshToken() != null)
                this.refreshToken = token.getSource().getRefreshToken(); //replace with fresh token if available
            this.token = token;
        }
    }

    private static OAuth20Service buildService(@NotNull DefaultApi20 authAPI, @NotNull String clientID, @Nullable String clientSecret, @Nullable CloseableHttpAsyncClient client) {
        ServiceBuilder b = new ServiceBuilder(clientID);
        if (client != null)
            b.httpClient(new ApacheHttpClient(client));
        if (clientSecret != null)
            b.apiSecret(clientSecret);
        return b.build(authAPI);
    }

    public void reconnectService(@Nullable CloseableHttpAsyncClient client) {
        reconnectService(null, client);
    }

    public void reconnectService(@Nullable DefaultApi20 authAPI, @Nullable CloseableHttpAsyncClient client) {
        reconnectService(buildService(authAPI == null ? service.getApi() : authAPI, service.getApiKey(), service.getApiSecret(), client));
    }

    public void reconnectService(@NotNull OAuth20Service service) {
        tokenLock.writeLock().lock();
        try {
            this.service = service;
        } finally {
            tokenLock.writeLock().unlock();
        }
    }

    protected Token requestAccessTokenClientFlow() throws IOException, ExecutionException, InterruptedException {
        return new Token((OpenIdOAuth2AccessToken) service.getAccessTokenClientCredentialsGrant());
    }

    protected Token requestAccessTokenRefreshFlow() throws IOException, ExecutionException, InterruptedException {
        return new Token((OpenIdOAuth2AccessToken) service.refreshAccessToken(refreshToken));
    }

    private Token requestAccessTokenRefreshFlow(@NotNull String refreshToken) throws IOException, ExecutionException, InterruptedException {
        return new Token((OpenIdOAuth2AccessToken) service.refreshAccessToken(refreshToken));
    }

    protected Token requestAccessTokenPasswordFlow(String username, String password) throws IOException, ExecutionException, InterruptedException {
        return new Token((OpenIdOAuth2AccessToken) service.getAccessTokenPasswordGrant(username, password, "offline_access openid license:thin license:full")); // license:full request token and new refresh token
    }

    /**
     * Check whether a refresh_token is available and valid.
     * Requests Authentication service
     *
     * @return true if the refresh token is not NULL and valid.
     */
    protected boolean isRefreshTokenValid() {
        if (refreshToken == null || refreshToken.isBlank())
            return false;

        tokenLock.writeLock().lock();
        try {
            setTokens(requestAccessTokenRefreshFlow());
        } catch (IOException | InterruptedException | ExecutionException e) {
            LoggerFactory.getLogger(getClass()).warn("Error when refreshing access_token with current refresh_token.", e);
            return false;
        } finally {
            tokenLock.writeLock().unlock();
        }
        return true;
    }

    /**
     * @return true if a client secret is configured for this service
     */
    protected boolean hasClientSecret() {
        return service.getApiSecret() != null;
    }

    public boolean needsLogin() {
        //check access token
        if (!needsRefresh())
            return false;
        //check if client secret flow is configured
        if (hasClientSecret())
            return false;
        //check if refresh token flow is working
        return !isRefreshTokenValid();
    }

    public boolean isLoggedIn() {
        return !needsLogin();
    }


    public boolean needsRefresh() {
        tokenLock.readLock().lock();
        try {
            return needsRefreshRaw();
        } finally {
            tokenLock.readLock().unlock();
        }
    }

    protected boolean needsRefreshRaw() {
        return token == null || token.isExpired();
    }

    public Token refreshIfNeeded() throws LoginException {
        return refreshIfNeeded(false);
    }

    public Token refreshIfNeeded(boolean force) throws LoginException {
        if (force || needsRefresh()) {
            tokenLock.writeLock().lock();
            try {
                if (force || needsRefreshRaw()) {
                    if (hasClientSecret()) {
                        setTokens(requestAccessTokenClientFlow());
                    } else {
                        if (refreshToken == null || refreshToken.isBlank())
                            throw new LoginException(new NullPointerException("Refresh token is null or empty!"));
                        setTokens(requestAccessTokenRefreshFlow());
                    }
                }
            } catch (IOException | InterruptedException | ExecutionException e) {
                throw new LoginException(e);
            } finally {
                tokenLock.writeLock().unlock();
            }
        }
        return token;
    }

    public void login(String username, String password) throws IOException, ExecutionException, InterruptedException {
        tokenLock.writeLock().lock();
        try {
            setTokens(requestAccessTokenPasswordFlow(username, password));
        } finally {
            tokenLock.writeLock().unlock();
        }
    }

    public void login(String refreshToken) throws IOException, ExecutionException, InterruptedException {
        tokenLock.writeLock().lock();
        try {
            setTokens(requestAccessTokenRefreshFlow(refreshToken));
        } finally {
            tokenLock.writeLock().unlock();
        }
    }

    @Override
    public void accept(HttpUriRequest httpUriRequest) {
        if (isLoggedIn())
            httpUriRequest.setHeader(new TokenHeader(() -> refreshIfNeeded().getAccessToken()));
    }

    @Contract(threading = ThreadingBehavior.IMMUTABLE)
    private static class TokenHeader implements Header {
        private final IOFunctions.IOSupplier<String> tokenSupplier;

        private TokenHeader(IOFunctions.IOSupplier<String> tokenSupplier) {
            this.tokenSupplier = tokenSupplier;
        }

        @Override
        public String getName() {
            return "Authorization";
        }

        @Override
        public String getValue() {
            try {
                return "Bearer " + tokenSupplier.get();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public boolean isSensitive() {
            return true;
        }
    }

    public void logout() {
        tokenLock.writeLock().lock();
        try {

            if (refreshToken != null) {
                try {
                    service.revokeToken(refreshToken, TokenTypeHint.REFRESH_TOKEN);
                } catch (Throwable e) {
                    LoggerFactory.getLogger(getClass()).warn("Error when revoking refresh token!", e);
                }
            }
            if (token != null) {
                try {
                    service.revokeToken(token.getAccessToken(), TokenTypeHint.ACCESS_TOKEN);
                } catch (Throwable e) {
                    LoggerFactory.getLogger(getClass()).warn("Error when revoking access token!", e);
                }
            }

            token = null;
            refreshToken = null;
        } finally {
            tokenLock.writeLock().unlock();
        }
    }

    public int getMinLifetime() {
        return minLifetime;
    }

    public void setMinLifetime(int minLifetime) {
        this.minLifetime = minLifetime;
    }

    protected String getRefreshToken() {
        return refreshToken;
    }

    public String getRefreshTokenForQuickReuse() throws IOException, ExecutionException, InterruptedException {
        tokenLock.writeLock().lock();
        try {
            final String r = getRefreshToken();
            if (r == null)
                return null;
            setTokens(requestAccessTokenRefreshFlow(r)); //get a fat one
            return r;
        } finally {
            tokenLock.writeLock().unlock();
        }
    }

    public URI signUpURL(URI redirectUrl) {
        return signUpURL(redirectUrl.toString());
    }

    public URI signUpURL(String redirectUrl) {
        return URI.create(service.createAuthorizationUrlBuilder().additionalParams(Map.of(
                "screen_hint", "signup",
                "prompt", "login",
                "redirect_uri", redirectUrl)).build());
    }

    public void sendPasswordReset(String email) throws IOException, ExecutionException, InterruptedException {
        Response resp = ((Auth0Service) service).sendPasswordResetRequest(email);
        if (!resp.isSuccessful())
            throw new IOException("Could not initiate Password reset. Cause: " + resp.getMessage() + " | Body: " + resp.getBody());
    }

    public Optional<Token> getToken() {
        try {
            return Optional.of(refreshIfNeeded());
        } catch (LoginException e) {
            LoggerFactory.getLogger(AuthServices.class).warn("No login Found: " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void close() throws IOException {
        service.close();
    }

    public class Token {
        private final OpenIdOAuth2AccessToken source;
        private final Date expTime;

        private Token(OpenIdOAuth2AccessToken source) {
            this.source = source;
            expTime = JWT.decode(source.getAccessToken()).getExpiresAt();
        }


        public boolean isExpired() {
            return expTime.getTime() - System.currentTimeMillis() < minLifetime;
        }

        public String getAccessToken() {
            return source.getAccessToken();
        }

        public String getOpenIdToken() {
            return source.getOpenIdToken();
        }

        public DecodedJWT getDecodedIdToken() {
            return JWT.decode(getOpenIdToken());
        }

        public DecodedJWT getDecodedAccessToken() {
            return JWT.decode(getAccessToken());
        }

        public OpenIdOAuth2AccessToken getSource() {
            return source;
        }
    }
}
