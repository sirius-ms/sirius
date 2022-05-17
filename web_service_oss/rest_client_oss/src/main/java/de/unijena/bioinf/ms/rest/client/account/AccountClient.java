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
import de.unijena.bioinf.auth.LoginException;
import de.unijena.bioinf.ms.rest.client.AbstractClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Supplier;

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


    public URI getSignUpRedirectURL() {
        try {
            return getBaseURI("/account/signUp").build();
        } catch (URISyntaxException x) {
            throw new IllegalArgumentException(x.getMessage(), x);
        }
    }

    public URI getSignUpURL() {
        return authService.signUpURL(getSignUpRedirectURL());
    }

    public boolean deleteAccount(@NotNull CloseableHttpClient client) {
        try {
            execute(client, () -> {
                HttpDelete delete = new HttpDelete(getBaseURI("/account/delete").build());
                final int timeoutInSeconds = 8000;
                delete.setConfig(RequestConfig.custom().setConnectTimeout(timeoutInSeconds).setSocketTimeout(timeoutInSeconds).build());
                return delete;
            });
            return true;
        } catch (IOException e) {
            LoggerFactory.getLogger(getClass()).warn("Error when deleting user account: " + e.getMessage());
            return false;
        }
    }

    public boolean acceptTerms(@NotNull CloseableHttpClient client) {
        try {
            execute(client, () -> {
                HttpPost post = new HttpPost(getBaseURI("/account/accept-terms").build());
                final int timeoutInSeconds = 8000;
                post.setConfig(RequestConfig.custom().setConnectTimeout(timeoutInSeconds).setSocketTimeout(timeoutInSeconds).build());
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
