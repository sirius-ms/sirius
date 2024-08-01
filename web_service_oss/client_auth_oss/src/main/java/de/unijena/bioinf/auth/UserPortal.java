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

import de.unijena.bioinf.ms.properties.PropertyManager;
import okhttp3.HttpUrl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class UserPortal {
    public boolean isConfigured() {
        try {
            URI.create(PropertyManager.getProperty("de.unijena.bioinf.sirius.web.portal"));
            return true;
        } catch (Exception e) {
            LoggerFactory.getLogger(UserPortal.class).warn("Error when creating portal URL: " + e.getMessage());
            return false;
        }
    }

    public static HttpUrl.Builder baseURLBuilder() {
        try {
            return HttpUrl.parse(PropertyManager.getProperty("de.unijena.bioinf.sirius.web.portal"))
                    .newBuilder();
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not part User portal URL: " + PropertyManager.getProperty("de.unijena.bioinf.sirius.web.portal"));
        }
    }

    public static URI baseURL() {
        return baseURLBuilder().build().uri();
    }


    public static URI withPath(@Nullable final String path) {
        if (path == null || path.isBlank())
            return baseURL();
        return baseURLBuilder().addPathSegments(path).build().uri();
    }

    public static URI signUpURL() {
        return withPath(signUpPath());
    }

    public static URI signInURL() {
        return signInURL(null);
    }

    public static URI signInURL(@Nullable String username) {
        HttpUrl.Builder b = baseURLBuilder().addPathSegments(signInPath());
        if (username != null && !username.isBlank())
            b.addQueryParameter("username", username);
        return b.build().uri();
    }

    public static URI openWithTokenURL(@NotNull String quickReuseToken) {
        return withPath(signInPath() + URLEncoder.encode(quickReuseToken, StandardCharsets.UTF_8));
    }

    public static URI pwResetURL() {
        return withPath(pwResetPath());
    }

    public static URI explorerLicURL(String licenseInfo) {
        return baseURLBuilder().addPathSegments(explorerLicPath())
                .addEncodedQueryParameter("key", URLEncoder.encode(licenseInfo, StandardCharsets.UTF_8))
                .build().uri();
    }

    public static String signUpPath() {
        return "auth/register/";
    }

    public static String pwResetPath() {
        return "auth/reset-password/";
    }

    public static String signInPath() {
        return "auth/login/";
    }

    public static String explorerLicPath() {
        return "register-explorer-license/";
    }
}
