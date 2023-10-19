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
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.function.Supplier;

/**
 * @deprecated user Browser based {@link de.unijena.bioinf.auth.UserPortal} for Account management instead
 * Still needed to accept terms and conditions within SIRIUS
 */
@Deprecated
public class AccountClient extends AbstractClient {
    private final AuthService authService;

    @SafeVarargs
    public AccountClient(@Nullable URI serverUrl, @Nullable String versionSuffix, @NotNull AuthService authService, IOFunctions.@NotNull IOConsumer<Request.Builder>... requestDecorators) {
        this(() -> serverUrl, () -> versionSuffix, authService, requestDecorators);
    }

    @SafeVarargs
    public AccountClient(@NotNull Supplier<URI> serverUrl, @NotNull Supplier<String> contextPath, @NotNull AuthService authService, IOFunctions.@NotNull IOConsumer<Request.Builder>... requestDecorators) {
        super(serverUrl, contextPath, requestDecorators);
        this.authService = authService;
    }

    public boolean acceptTerms(@NotNull OkHttpClient client) {
        try {
            execute(client, () -> new Request.Builder()
                    .url(getBaseURI("account/accept-terms").build())
                    .post(RequestBody.create(new byte[]{})));
            return true;
        } catch (IOException e) {
            LoggerFactory.getLogger(getClass()).warn("Error when accepting terms: " + e.getMessage());
            return false;
        }
    }
}
