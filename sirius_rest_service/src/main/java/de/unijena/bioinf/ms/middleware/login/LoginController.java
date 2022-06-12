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

package de.unijena.bioinf.ms.middleware.login;

import de.unijena.bioinf.auth.AuthService;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.middleware.login.model.AccountCredentials;
import de.unijena.bioinf.ms.middleware.login.model.AccountInfo;
import de.unijena.bioinf.ms.rest.model.license.Subscription;
import de.unijena.bioinf.webapi.Tokens;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping(value = "/api/account")
@Tag(name = "Login and Account", description = "Perform signIn, signOut and signUp. Get tokens and account information.")
public class LoginController {

    /**
     * Login into SIRIUS web services.
     * @param credentials used to log in.
     * @param failIfLoggedIn if true request fails if an active login already exists.
     * @param includeSubs  include available and active subscriptions in {@link AccountInfo}.
     * @return Basic information about the account that has been logged in and its subscriptions.
     */
    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
    public AccountInfo login(@RequestBody AccountCredentials credentials,
                             @RequestParam(required = false, defaultValue = "false") boolean failIfLoggedIn,
                             @RequestParam(required = false, defaultValue = "false") boolean includeSubs
    ) throws IOException, ExecutionException, InterruptedException {
        AuthService as = ApplicationCore.WEB_API.getAuthService();
        if (!as.needsLogin()) {
            if (failIfLoggedIn)
                throw new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED, "Already logged in. PLeas logout first or use 'failIfExists=false'.");
            else
                as.logout();
        }
        as.login(credentials.getUsername(), credentials.getPassword());
        return getAccountInfo(includeSubs);
    }

    /**
     * Logout from SIRIUS web services.
     */
    @PostMapping(value = "/logout", produces = MediaType.APPLICATION_JSON_VALUE)
    public void logout() {
        ApplicationCore.WEB_API.getAuthService().logout();
    }

    /**
     * Get information about the account currently logged in. Fails if not logged in.
     * @param includeSubs include available and active subscriptions in {@link AccountInfo}.
     * @return Basic information about the account that has been logged in and its subscriptions.
     */
    @GetMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
    public AccountInfo getAccountInfo(@RequestParam(required = false, defaultValue = "false") boolean includeSubs) {
        return ApplicationCore.WEB_API.getAuthService()
                .getToken().map(t -> AccountInfo.of(t, ApplicationCore.WEB_API.getActiveSubscription()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not Logged in. Please log in to retrieve account information."));
    }

    /**
     * Get available subscriptions of the account currently logged in. Fails if not logged in.
     */
    @GetMapping(value = "/subscriptions", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Subscription> getSubscriptions() {
        return ApplicationCore.WEB_API.getAuthService()
                .getToken().map(Tokens::getSubscriptions)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not Logged in. Please log in to retrieve subscriptions."));
    }


    /**
     * Request a long-lived refresh token for the account currently logged in (keep it save).
     * @return refresh_token in JWT bearer format.
     */
    @Deprecated
    @PostMapping(value = "/request-token", produces = MediaType.APPLICATION_JSON_VALUE)
    public String requestRefreshToken(){
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "NOT YET IMPLEMENTED");
    }

    /**
     * Invalidate a long-lived refresh token for the account currently logged in.
     */
    @Deprecated
    @PostMapping(value = "/invalidate-token", produces = MediaType.APPLICATION_JSON_VALUE)
    public void invalidateRefreshToken(){
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "NOT YET IMPLEMENTED");
    }

}
