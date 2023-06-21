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

import com.github.scribejava.apis.Auth0Api;
import de.unijena.bioinf.babelms.utils.Base64;
import de.unijena.bioinf.ms.properties.PropertyManager;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

public class AuthServices {
    private AuthServices() {
    }

    public static AuthService createDefault(@NotNull URI audienceURI, Path refreshTokenFile, @Nullable OkHttpClient client) {
        return createDefault(audienceFromURI(audienceURI), refreshTokenFile, client);
    }

    public static AuthService createDefault(@NotNull String audience, Path refreshTokenFile, @Nullable OkHttpClient client) {
        String rToken = null;
        try {
            if (Files.exists(refreshTokenFile))
                rToken = readRefreshToken(refreshTokenFile);
        } catch (IOException e) {
            LoggerFactory.getLogger(AuthServices.class).warn("Could not read refresh token from file! (re)login might be needed!", e);
        }

        return createDefault(audience, rToken, client, as -> {
            if (as.hasRefreshToken()) {
                try {
                    writeRefreshToken(as, refreshTokenFile);
                } catch (IOException e) {
                    LoggerFactory.getLogger(AuthServices.class).warn("Error when writing refresh token. Login might not be persistent. You probably have to re-login next time", e);
                }
            } else {
                try {
                    clearRefreshToken(refreshTokenFile);
                } catch (IOException e) {
                    LoggerFactory.getLogger(AuthServices.class).warn("Error when try to remove refresh token. Token might still be present at '" + refreshTokenFile + "'. Please remove the file manually to ensure that your are logged out!", e);
                }
            }
        });
    }

    public static AuthService createDefault(@NotNull URI audienceURI, @Nullable OkHttpClient client) {
        return createDefault(audienceFromURI(audienceURI), client);
    }

    public static AuthService createDefault(@NotNull String audience, @Nullable OkHttpClient client) {
        String rToken = PropertyManager.getProperty("de.unijena.bioinf.sirius.security.rToken");
        return createDefault(audience, rToken, client);
    }

    protected static AuthService createDefault(@NotNull URI audienceURI, @Nullable String rToken, @Nullable OkHttpClient client) {
        return createDefault(audienceFromURI(audienceURI), rToken, client);
    }

    @SafeVarargs
    protected static AuthService createDefault(@NotNull String audience, @Nullable String rToken, @Nullable OkHttpClient client, Consumer<AuthService>... postRefreshHooks) {
        return new AuthService(createDefaultApi(audience),
                PropertyManager.getProperty("de.unijena.bioinf.sirius.security.clientID"),
                PropertyManager.getProperty("de.unijena.bioinf.sirius.security.clientSecret"),
                rToken, client, postRefreshHooks);
    }

    public static Auth0Api createDefaultApi(@NotNull URI audienceURI) {
        return createDefaultApi(audienceFromURI(audienceURI));
    }

    public static Auth0Api createDefaultApi(@NotNull String audience) {
        return Auth0Api.instance(PropertyManager.getProperty("de.unijena.bioinf.sirius.security.authServer"), audience);
    }

    protected static String audienceFromURI(@NotNull URI audienceURI) {
        return audienceURI.getScheme() + "://" + audienceURI.getHost() + "/";
    }

    public static void writeRefreshToken(@NotNull AuthService service, @NotNull Path refreshTokenFile) throws IOException {
        writeRefreshToken(service, refreshTokenFile, false);
    }

    public static void writeRefreshToken(@NotNull AuthService service, @NotNull Path refreshTokenFile, final boolean ignoreMissing) throws IOException {
        if (!service.needsLogin()) {
            if (service.getRefreshToken() != null) {
                writeRefreshToken(service.getRefreshToken(), refreshTokenFile);
            } else {
                LoggerFactory.getLogger(AuthServices.class).warn("Cannot write refresh token. Given Service seems to rely on client credentials flow, so  refresh token is obsolete. Skipping!");
            }
        } else {
            if (!ignoreMissing)
                throw new LoginException(new IllegalStateException("Cannot save refresh token if user is not logged in."));
        }
    }

    public static void writeRefreshToken(String refreshToken, Path refreshTokenFile) throws IOException {
        Files.write(refreshTokenFile, Base64.encodeBytesToBytes(refreshToken.getBytes(StandardCharsets.UTF_8)));
    }

    public static String readRefreshToken(Path refreshTokenFile) throws IOException {
        return new String(Base64.decode(Files.readAllBytes(refreshTokenFile)), StandardCharsets.UTF_8);
    }

    public static boolean clearRefreshToken(Path refreshTokenFile) throws IOException {
        return clearRefreshToken(null, refreshTokenFile);
    }

    public static boolean clearRefreshToken(@Nullable AuthService toClear, @NotNull Path refreshTokenFile) throws IOException {
        if (toClear != null)
            toClear.logout();
        return Files.deleteIfExists(refreshTokenFile);
    }
}
