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
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.ExecutionException;

//todo set active subscription
@RestController
@RequestMapping(value = "/api/account")
@Tag(name = "Login and Account", description = "Perform signIn, signOut and signUp. Get tokens and account information.")
public class LoginController {

    /**
     * Login into SIRIUS web services.
     *
     * @param credentials      used to log in.
     * @param failWhenLoggedIn if true request fails if an active login already exists.
     * @param includeSubs      include available and active subscriptions in {@link AccountInfo}.
     * @return Basic information about the account that has been logged in and its subscriptions.
     */
    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
    public AccountInfo login(@RequestBody AccountCredentials credentials,
                             @RequestParam boolean acceptTerms,
                             @RequestParam(required = false, defaultValue = "false") boolean failWhenLoggedIn,
                             @RequestParam(required = false, defaultValue = "false") boolean includeSubs
    ) throws IOException, ExecutionException, InterruptedException {
        AuthService as = ApplicationCore.WEB_API.getAuthService();
        if (!as.needsLogin()) {
            if (failWhenLoggedIn)
                throw new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED, "Already logged in. PLeas logout first or use 'failWhenLoggedIn=false'.");
            else
                as.logout();
        }
        as.login(credentials.getUsername(), credentials.getPassword());
        if (acceptTerms)
            ApplicationCore.WEB_API.acceptTermsAndRefreshToken();
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
     *
     * @param includeSubs include available and active subscriptions in {@link AccountInfo}.
     * @return Basic information about the account that has been logged in and its subscriptions.
     */
    @GetMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
    public AccountInfo getAccountInfo(@RequestParam(required = false, defaultValue = "false") boolean includeSubs) {
        return ApplicationCore.WEB_API.getAuthService()
                .getToken().map(t -> AccountInfo.of(t, ApplicationCore.WEB_API.getActiveSubscription(), includeSubs))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not Logged in. Please log in to retrieve account information."));
    }

    /**
     * Check if a user is logged in.
     *
     * @return true if the user is logged in
     */
    @GetMapping(value = "/isLoggedIn", produces = MediaType.APPLICATION_JSON_VALUE)
    public boolean isLoggedIn() {
        return ApplicationCore.WEB_API.getAuthService().isLoggedIn();
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
     * Get SignUp URL (For signUp via web browser)
     */
    @GetMapping(value = "/signUpURL", produces = MediaType.TEXT_PLAIN_VALUE)
    public String getSignUpURL() throws URISyntaxException {
        return ApplicationCore.WEB_API.getSignUpURL().toString();
    }


    /**
     * Open SignUp window in system browser and return signUp link.
     */
    @GetMapping(value = "/signUp", produces = MediaType.TEXT_PLAIN_VALUE)
    public String signUp() throws URISyntaxException {
        String path = getSignUpURL();
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(path));
            } else {
                String message = "Could not detect system browser to open URL. Try visit Page Manually: " + path;
                LoggerFactory.getLogger(getClass()).error("Desktop NOT supported: " + message);

                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message);
            }
        } catch (IOException e) {
            String message = "Could not Open URL in System Browser. Try visit Page Manually: " + path;
            LoggerFactory.getLogger(getClass()).error(message, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message);
        }
        return path.toString();
    }
}
