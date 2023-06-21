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

package com.github.scribejava.apis;

import com.github.scribejava.apis.openid.OpenIdJsonTokenExtractor;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.extractors.TokenExtractor;
import com.github.scribejava.core.httpclient.HttpClient;
import com.github.scribejava.core.httpclient.HttpClientConfig;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.apis.auth0.Auth0Service;
import org.jetbrains.annotations.NotNull;

import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Auth0Api extends DefaultApi20 {
    private static final ConcurrentMap<String, Auth0Api> INSTANCES = new ConcurrentHashMap<>();

    @NotNull
    private final URL localAuthProviderURL;

    public URL getLocalAuthProviderURL() {
        return localAuthProviderURL;
    }

    @NotNull
    private String audience;

    public String getAudience() {
        return audience;
    }

    public Auth0Api(@NotNull URL localAuthProviderURL, @NotNull String audience) {
        super();
        this.localAuthProviderURL = localAuthProviderURL;
        this.audience = audience;
    }

    public static Auth0Api instance(@NotNull String localAuthProviderURL, @NotNull String audience) {
        try {
            final String defaultBaseUrlWithRealm = composeBaseUrlWithAudience(localAuthProviderURL, audience);
            final URL url = new URL(localAuthProviderURL);
            return  INSTANCES.computeIfAbsent(defaultBaseUrlWithRealm, k -> new Auth0Api(url, audience));
        } catch (MalformedURLException e) {
            e.printStackTrace();
            throw new IllegalArgumentException(e);
        }


    }

    protected static String composeBaseUrlWithAudience(String baseUrl, String audience) {
        return baseUrl + (baseUrl.endsWith("/") ? "" : "/")  + audience;
    }

    @Override
    public String getAccessTokenEndpoint() {
        return localAuthProviderURL.toString() + "/oauth/token";
    }

    @Override
    protected String getAuthorizationBaseUrl() {
        return localAuthProviderURL.toString() + "/authorize";
    }

    @Override
    public String getRevokeTokenEndpoint() {
        return localAuthProviderURL.toString() + "/oauth/revoke";
    }


    public String getAuthProviderURL(){
        return localAuthProviderURL.toString();
    }

    @Override
    public TokenExtractor<OAuth2AccessToken> getAccessTokenExtractor() {
        return OpenIdJsonTokenExtractor.instance();
    }

    @Override
    public Auth0Service createService(String apiKey, String apiSecret, String callback, String defaultScope,
                                      String responseType, OutputStream debugStream, String userAgent, HttpClientConfig httpClientConfig,
                                      HttpClient httpClient) {
        return new Auth0Service(this, apiKey, apiSecret, callback, defaultScope, responseType, debugStream,
                userAgent, httpClientConfig, httpClient);
    }
}