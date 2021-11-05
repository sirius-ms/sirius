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

import com.auth0.jwt.interfaces.DecodedJWT;
import de.unijena.bioinf.auth.auth0.Auth0Api;
import de.unijena.bioinf.babelms.utils.Base64;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class AuthServices {
    private AuthServices() {
    }

    public static AuthService createDefault(Path refreshTokenFile, @Nullable CloseableHttpAsyncClient client) throws MalformedURLException {
        String rToken = null;
        try {
            if (Files.exists(refreshTokenFile))
                rToken = readRefreshToken(refreshTokenFile);
        } catch (IOException e) {
            LoggerFactory.getLogger(AuthServices.class).warn("Could not read refresh token from file! (re)login might be needed!", e);
        }

        Auth0Api api = new Auth0Api(PropertyManager.getProperty("de.unijena.bioinf.sirius.security.authServer"));
        return new AuthService(rToken, api, client);
    }

    public static AuthService createDefaultFromCredentialsFile(@Nullable CloseableHttpAsyncClient client) throws MalformedURLException {
        String rToken = PropertyManager.getProperty("de.unijena.bioinf.sirius.security.rToken");
        Auth0Api api = new Auth0Api(PropertyManager.getProperty("de.unijena.bioinf.sirius.security.authServer"));
        return new AuthService(rToken, api, client);
    }

    public static void writeRefreshToken(@NotNull AuthService service, @NotNull Path refreshTokenFile) throws IOException {
        if (service.needsLogin()) {
            writeRefreshToken(service.getRefreshToken(), refreshTokenFile);
        } else {
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

    @Nullable
    public static DecodedJWT getIDToken(AuthService service)  {
        try {
            return service.refreshIfNeeded().getDecodedIdToken();
        } catch (LoginException e) {
            LoggerFactory.getLogger(AuthServices.class).warn("No login Found: " + e.getMessage());
            return null;
        }
    }
}
