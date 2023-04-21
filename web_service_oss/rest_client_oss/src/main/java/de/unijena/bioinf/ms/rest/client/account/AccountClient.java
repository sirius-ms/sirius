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

package de.unijena.bioinf.ms.rest.client.account;

import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import de.unijena.bioinf.auth.AuthService;
import de.unijena.bioinf.ms.rest.client.AbstractClient;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * @deprecated user Browser based {@link de.unijena.bioinf.auth.UserPortal} for Account management instead
 * Still needed to accept terms and conditions within SIRIUS
 */
@Deprecated
public class AccountClient extends AbstractClient {
    private final AuthService authService;
    private final String versionSuffix;

    @SafeVarargs
    public AccountClient(@Nullable URI serverUrl, @Nullable String versionSuffix, @NotNull AuthService authService, IOFunctions.@NotNull IOConsumer<HttpUriRequest>... requestDecorators) {
        this(() -> serverUrl, versionSuffix, authService, requestDecorators);

    }

    @SafeVarargs
    public AccountClient(@NotNull Supplier<URI> serverUrl, @Nullable String versionSuffix, @NotNull AuthService authService, IOFunctions.@NotNull IOConsumer<HttpUriRequest>... requestDecorators) {
        super(serverUrl, requestDecorators);
        this.authService = authService;
        this.versionSuffix = versionSuffix;
    }


    /**
     * Redirect URI for native Auth0 signup (no user portal involved)
     * @return The redirect URI
     */
    public URI getSignUpRedirectURL() {
        try {
            return getBaseURI("/account/signUp").build();
        } catch (URISyntaxException x) {
            throw new IllegalArgumentException(x.getMessage(), x);
        }
    }

    /**
     * URI for Native Auth0 signup (no user portal involved)
     * @return The signup URI with parameters
     */
    public URI getSignUpURL() {
        return authService.signUpURL(getSignUpRedirectURL());
    }

    public boolean deleteAccount(@NotNull HttpClient client) {
        try {
            execute(client, () -> {
                HttpDelete delete = new HttpDelete(getBaseURI("/account/delete").build());
                final int timeoutInSeconds = 8000;
                delete.setConfig(RequestConfig.custom().setConnectTimeout(timeoutInSeconds, TimeUnit.SECONDS)/*.setSocketTimeout(timeoutInSeconds)*/.build());
                return delete;
            });
            return true;
        } catch (IOException e) {
            LoggerFactory.getLogger(getClass()).warn("Error when deleting user account: " + e.getMessage());
            return false;
        }
    }

    public boolean acceptTerms(@NotNull HttpClient client) {
        try {
            execute(client, () -> {
                HttpPost post = new HttpPost(getBaseURI("/account/accept-terms").build());
                post.setEntity(new StringEntity(""));
                final int timeoutInSeconds = 8000;
                post.setConfig(RequestConfig.custom().setConnectTimeout(timeoutInSeconds, TimeUnit.SECONDS)
                        /*.setSocketTimeout(timeoutInSeconds)*/.build());
                return post;
            });
            return true;
        } catch (IOException e) {
            LoggerFactory.getLogger(getClass()).warn("Error when accepting terms: " + e.getMessage());
            return false;
        }
    }

    @Override
    protected String makeVersionContext() {
        if (versionSuffix == null || versionSuffix.isBlank())
            return "";
        return "/" + versionSuffix;
    }
}
