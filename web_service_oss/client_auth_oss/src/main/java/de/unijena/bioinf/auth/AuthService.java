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
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.github.scribejava.core.revoke.TokenTypeHint;
import com.github.scribejava.httpclient.okhttp.OkHttpHttpClient;
import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;


public class AuthService implements IOFunctions.IOConsumer<Request.Builder>, Closeable {
    private OAuth20Service service;

    @Nullable
    private String refreshToken;
    @Nullable
    private Token token;

    // we could add even more such hooks in the future
    private final LinkedHashSet<Consumer<AuthService>> postRefreshHooks = new LinkedHashSet<>();

    public LinkedHashSet<Consumer<AuthService>> postRefreshHooks() {
        return postRefreshHooks;
    }


    protected final ReadWriteLock tokenLock = new ReentrantReadWriteLock();

    private int minLifetime = 900000;


    @SafeVarargs
    public AuthService(@NotNull DefaultApi20 authAPI, @NotNull String clientID, Consumer<AuthService>... postRefreshHooks) {
        this(authAPI, clientID, null, postRefreshHooks);
    }

    @SafeVarargs
    public AuthService(@NotNull DefaultApi20 authAPI, @NotNull String clientID, @Nullable OkHttpClient client, Consumer<AuthService>... postRefreshHooks) {
        this(authAPI, clientID, null, null, client, postRefreshHooks);
    }

    @SafeVarargs
    public AuthService(@NotNull DefaultApi20 authAPI, @NotNull String clientID, @Nullable String clientSecret, @Nullable String refreshToken, Consumer<AuthService>... postRefreshHooks) {
        this(authAPI, clientID, clientSecret, refreshToken, null, postRefreshHooks);
    }

    @SafeVarargs
    public AuthService(@NotNull DefaultApi20 authAPI, @NotNull String clientID, @Nullable String clientSecret, @Nullable String refreshToken, @Nullable OkHttpClient client, Consumer<AuthService>... postRefreshHooks) {
        this(buildService(authAPI, clientID, clientSecret, client), refreshToken, postRefreshHooks);
    }

    @SafeVarargs
    public AuthService(@NotNull OAuth20Service service, @Nullable String refreshToken, Consumer<AuthService>... postRefreshHooks) {
        this.service = service;
        if (postRefreshHooks != null && postRefreshHooks.length > 0)
            this.postRefreshHooks.addAll(List.of(postRefreshHooks));
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
            this.token = token;
            if (token.getSource().getRefreshToken() != null) {
                this.refreshToken = token.getSource().getRefreshToken(); //replace with fresh token if available
                postRefreshHooks.forEach(it -> it.accept(this));
            }

        } else {
            this.token = null;
            this.refreshToken = null;
            postRefreshHooks.forEach(it -> it.accept(this));
        }
    }

    private static OAuth20Service buildService(@NotNull DefaultApi20 authAPI, @NotNull String clientID, @Nullable String clientSecret, @Nullable OkHttpClient client) {
        ServiceBuilder b = new ServiceBuilder(clientID);
        if (client != null)
            b.httpClient(new OkHttpHttpClient(client));
        if (clientSecret != null)
            b.apiSecret(clientSecret);
        return b.build(authAPI);
    }

    public void reconnectService(@Nullable OkHttpClient client) {
        reconnectService(null, client);
    }

    public void reconnectService(@Nullable DefaultApi20 authAPI, @Nullable OkHttpClient client) {
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
        return new Token((OpenIdOAuth2AccessToken) service.refreshAccessToken(refreshToken, "offline_access openid profile email license:v3 license:full"));
    }

    private Token requestAccessTokenRefreshFlow(@NotNull String refreshToken) throws IOException, ExecutionException, InterruptedException {
        return new Token((OpenIdOAuth2AccessToken) service.refreshAccessToken(refreshToken, "offline_access openid profile email license:v3 license:full"));
    }

    protected Token requestAccessTokenPasswordFlow(String username, String password) throws IOException, ExecutionException, InterruptedException {
        return new Token((OpenIdOAuth2AccessToken) service.getAccessTokenPasswordGrant(username, password, "offline_access openid profile email license:v3 license:full")); // license:full request token and new refresh token
    }

    /**
     * Check whether a refresh_token is available and valid.
     * Requests Authentication service
     *
     * @return true if the refresh token is not NULL and valid.
     */
    protected boolean isRefreshTokenValid() {
        if (!hasRefreshToken())
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

    protected boolean hasRefreshToken() {
        return refreshToken != null && !refreshToken.isBlank();
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
                        if (!hasRefreshToken())
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
    public void accept(Request.Builder httpUriRequest) {
        if (isLoggedIn()) {
            try {
                httpUriRequest.addHeader("Authorization",  "Bearer " + refreshIfNeeded().getAccessToken());
            } catch (LoginException e) {
                throw new RuntimeException(e);
            }
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

            setTokens(null);
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
