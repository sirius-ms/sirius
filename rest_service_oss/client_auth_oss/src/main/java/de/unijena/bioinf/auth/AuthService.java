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

import com.github.scribejava.apis.openid.OpenIdOAuth2AccessToken;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.github.scribejava.core.revoke.TokenTypeHint;
import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import de.unijena.bioinf.fingerid.utils.FingerIDProperties;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.apache.http.client.methods.HttpUriRequest;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AuthService implements IOFunctions.IOConsumer<HttpUriRequest> {

    protected final OAuth20Service service;

    @Nullable
    private String refreshToken;
    private OpenIdOAuth2AccessToken token;

    protected final ReadWriteLock tokenLock = new ReentrantReadWriteLock();

    private int minLifetime = 60;


    public AuthService(DefaultApi20 authAPI) {
        this(null, authAPI);
    }

    public AuthService(@Nullable String refreshToken, DefaultApi20 authAPI) {
        this(refreshToken, buildService(authAPI));
    }

    public AuthService(@Nullable String refreshToken, OAuth20Service service) {
        this.refreshToken = refreshToken;
        this.service = service;
    }


    private static OAuth20Service buildService(DefaultApi20 authAPI) {
        ServiceBuilder b = new ServiceBuilder(PropertyManager.getProperty("de.unijena.bioinf.sirius.security.clientID", null, null));
        String secret = PropertyManager.getProperty("de.unijena.bioinf.sirius.security.clientSecret");
        if (secret != null)
            b.apiSecret(secret);
//                .defaultScope("offline_access") // replace with desired scope
//              .callback("http://your.site.com/callback")
        return b.build(authAPI);
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
            token = (OpenIdOAuth2AccessToken) service.refreshAccessToken(refreshToken);
        } catch (IOException | InterruptedException | ExecutionException e) {
            LoggerFactory.getLogger(getClass()).warn("Error when refreshing access_token with current refresh_token.", e);
            return false;
        } finally {
            tokenLock.writeLock().lock();
        }
        return true;
    }

    public boolean needsLogin() {
        if (!needsRefresh())
            return true;
        return !isRefreshTokenValid();
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
        return token == null || token.getExpiresIn() < minLifetime;
    }

    public OpenIdOAuth2AccessToken refreshIfNeeded() throws LoginException {
        if (needsRefresh()) {
            tokenLock.writeLock().lock();
            try {
                if (needsRefreshRaw()) {
                    if (refreshToken == null || refreshToken.isBlank())
                        throw new LoginException(new NullPointerException("Refresh token is null or empty!"));
                    token = (OpenIdOAuth2AccessToken) service.refreshAccessToken(refreshToken);
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
            token = (OpenIdOAuth2AccessToken) service.getAccessTokenPasswordGrant(username, password, "offline_access"); //request token and new refresh token
            refreshToken = token.getRefreshToken();
        } finally {
            tokenLock.writeLock().unlock();
        }
    }

    public void login() throws IOException, ExecutionException, InterruptedException {
        tokenLock.writeLock().lock();
        try {
            token = (OpenIdOAuth2AccessToken) service.getAccessTokenClientCredentialsGrant("offline_access"); //request token and new refresh token
            refreshToken = token.getRefreshToken();
        } finally {
            tokenLock.writeLock().unlock();
        }
    }


    @Override
    public void accept(HttpUriRequest httpUriRequest) throws IOException {
        httpUriRequest.setHeader("Authorization", "Bearer " + refreshIfNeeded().getOpenIdToken());
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

    protected String getRefreshToken(){
        return refreshToken;
    }

    public String signUpURL(String redirectUrl){
        return service.createAuthorizationUrlBuilder().additionalParams(Map.of("screen_hint","signup","redirect_uri", redirectUrl)).build();
    }


}
