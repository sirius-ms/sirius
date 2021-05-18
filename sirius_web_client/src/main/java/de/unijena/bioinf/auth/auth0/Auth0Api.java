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

package de.unijena.bioinf.auth.auth0;

import com.github.scribejava.apis.openid.OpenIdJsonTokenExtractor;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.extractors.TokenExtractor;
import com.github.scribejava.core.model.OAuth2AccessToken;

import java.net.MalformedURLException;
import java.net.URL;

public class Auth0Api extends DefaultApi20 {
    private URL localAuthProviderURL;
    public Auth0Api() {}

    public Auth0Api(String localAuthProviderURL) throws MalformedURLException {
        this(new URL(localAuthProviderURL));
    }

    public Auth0Api(URL localAuthProviderURL) {
        this.localAuthProviderURL = localAuthProviderURL;
    }

    private static class InstanceHolder {
        private static final Auth0Api INSTANCE = new Auth0Api();
    }

    public static Auth0Api instance() {
        return InstanceHolder.INSTANCE;
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
    public TokenExtractor<OAuth2AccessToken> getAccessTokenExtractor() {
        return OpenIdJsonTokenExtractor.instance();
    }
}