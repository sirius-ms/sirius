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

package com.github.scribejava.apis.auth0;

import com.github.scribejava.apis.Auth0Api;
import com.github.scribejava.core.httpclient.HttpClient;
import com.github.scribejava.core.httpclient.HttpClientConfig;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.github.scribejava.core.revoke.TokenTypeHint;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;

public class Auth0Service extends OAuth20Service {

    public Auth0Service(Auth0Api api, String apiKey, String apiSecret, String callback, String defaultScope, String responseType, OutputStream debugStream, String userAgent, HttpClientConfig httpClientConfig, HttpClient httpClient) {
        super(api, apiKey, apiSecret, callback, defaultScope, responseType, debugStream, userAgent, httpClientConfig, httpClient);
    }

    @Override
    protected OAuthRequest createAccessTokenPasswordGrantRequest(String username, String password, String scope) {
        return withAudience(withClientID(super.createAccessTokenPasswordGrantRequest(username, password, scope)));
    }

    @Override
    protected OAuthRequest createRefreshTokenRequest(String refreshToken, String scope) {
        // we want this to be audience independent so that we can request access tokens for multiple audiences
        return withAudience(withClientID(super.createRefreshTokenRequest(refreshToken, scope)));
    }

    @Override
    protected OAuthRequest createAccessTokenClientCredentialsGrantRequest(String scope) {
        return withAudience(super.createAccessTokenClientCredentialsGrantRequest(scope));
    }

    @Override
    protected OAuthRequest createRevokeTokenRequest(String tokenToRevoke, TokenTypeHint tokenTypeHint) {
        return withClientID(super.createRevokeTokenRequest(tokenToRevoke, tokenTypeHint));
    }

    protected OAuthRequest withClientID(@NotNull final OAuthRequest request){
        request.addParameter("client_id", getApiKey());
        return request;
    }

    protected OAuthRequest withAudience(@NotNull final OAuthRequest request){
        request.addParameter("audience", ((Auth0Api)getApi()).getAudience());
        return request;
    }

    public OAuthRequest createPasswordResetRequest(String email){
        final OAuthRequest request = new OAuthRequest(Verb.POST, ((Auth0Api)getApi()).getAuthProviderURL() + "/dbconnections/change_password");
        request.addHeader("content-type", "application/json");
        request.setPayload("{\"client_id\": \"" + getApiKey() +
                "\",\"email\": \"" + email +
                "\",\"connection\": \"Username-Password-Authentication\"}");
        return request;
    }

    public Response sendPasswordResetRequest(String email) throws IOException, ExecutionException, InterruptedException {
        return execute(createPasswordResetRequest(email));
    }
}
