/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.middleware.controller;

import de.unijena.bioinf.auth.AuthService;
import de.unijena.bioinf.auth.UserPortal;
import de.unijena.bioinf.ms.middleware.model.login.AccountCredentials;
import de.unijena.bioinf.ms.middleware.model.login.AccountInfo;
import de.unijena.bioinf.ms.middleware.model.login.Subscription;
import de.unijena.bioinf.webapi.Tokens;
import de.unijena.bioinf.webapi.WebAPI;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@RestController
@RequestMapping(value = "/api/account")
@Tag(name = "Login and Account", description = "Perform signIn, signOut and signUp. Get tokens and account information.")
public class LoginController {
    private static final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    private final WebAPI<?> webAPI;

    public LoginController(WebAPI<?> webAPI) {
        this.webAPI = webAPI;
    }

    /**
     * Login into SIRIUS web services and activate default subscription if available.
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
        lock.writeLock().lock();
        try {
            AuthService as = webAPI.getAuthService();
            if (!as.needsLogin()) {
                if (failWhenLoggedIn)
                    throw new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED, "Already logged in. Please logout first or use 'failWhenLoggedIn=false'.");
                else
                    as.logout();
            }
            as.login(credentials.getUsername(), credentials.getPassword());

            // enable default subscription
            webAPI.changeActiveSubscription(Tokens.getActiveSubscription(as.getToken().orElseThrow()));

            // if there is no sub available accept-terms will fail
            if (acceptTerms && webAPI.getActiveSubscription() != null)
                webAPI.acceptTermsAndRefreshToken();
            return getAccountInfo(includeSubs);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Logout from SIRIUS web services.
     */
    @PostMapping(value = "/logout", produces = MediaType.APPLICATION_JSON_VALUE)
    public void logout() {
        lock.writeLock().lock();
        try {
            webAPI.getAuthService().logout();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Get information about the account currently logged in. Fails if not logged in.
     *
     * @param includeSubs include available and active subscriptions in {@link AccountInfo}.
     * @return Basic information about the account that has been logged in and its subscriptions.
     */
    @GetMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
    public AccountInfo getAccountInfo(@RequestParam(required = false, defaultValue = "false") boolean includeSubs) {
        lock.readLock().lock();
        try {
            return webAPI.getAuthService()
                    .getToken().map(t -> AccountInfo.of(t, Optional.ofNullable(webAPI.getActiveSubscription())
                            .map(Subscription::of).orElse(null), includeSubs))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not Logged in. Please log in to retrieve account information."));
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Check if a user is logged in.
     *
     * @return true if the user is logged in
     */
    @GetMapping(value = "/isLoggedIn", produces = MediaType.APPLICATION_JSON_VALUE)
    public boolean isLoggedIn() {
        lock.readLock().lock();
        try {
            return webAPI.getAuthService().isLoggedIn();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get available subscriptions of the account currently logged in. Fails if not logged in.
     */
    @GetMapping(value = "/subscriptions", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Subscription> getSubscriptions() {
        lock.readLock().lock();
        try {
            return webAPI.getAuthService()
                    .getToken().map(Tokens::getSubscriptions)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not Logged in. Please log in to retrieve subscriptions."))
                    .stream().map(Subscription::of).toList();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Select a subscription as active subscription to be used for computations.
     * @return Account information with updated active subscription
     */
    @PutMapping(value = "/subscriptions/select-active", produces = MediaType.APPLICATION_JSON_VALUE)
    public AccountInfo selectSubscription(@RequestParam @NotNull String sid) {
        lock.readLock().lock();
        try {
            de.unijena.bioinf.ms.rest.model.license.Subscription sub = webAPI.getAuthService().getToken()
                    .map(Tokens::getSubscriptions)
                    .flatMap(l -> l.stream().filter(s -> sid.equals(s.getSid())).findFirst())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "The subscription has not been found in your account and cannot be selected as active subscription"));

            webAPI.changeActiveSubscription(sub);
            return getAccountInfo(false);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get SignUp URL (For signUp via web browser)
     */
    @GetMapping(value = "/signUpURL", produces = MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8")
    //specify utf-8 because some generated clients have problems if not specified explicitly
    public String getSignUpURL() {
        return UserPortal.signUpURL().toString();
    }


    /**
     * Open SignUp window in system browser and return signUp link.
     */
    @GetMapping(value = "/signUp", produces = MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8")
    //specify utf-8 because some generated clients have problems if not specified explicitly
    public String signUp() {
        URI uri = UserPortal.signUpURL();
        openInBrowser(uri);
        return uri.toString();
    }

    /**
     * Open User portal in browser. If user is logged in SIRIUS tries to transfer the login state to the browser.
     */
    @GetMapping(value = "/openPortal")
    public void openPortal() {
        openInBrowser(webAPI.getAuthService().getToken()
                .flatMap(Tokens::getUsername)
                .map(UserPortal::signInURL).orElse(UserPortal.signInURL()));
    }

    private void openInBrowser(URI uri) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(uri);
            } else {
                String message = "Could not detect system browser to open URL. Try visit Page Manually: " + uri;
                LoggerFactory.getLogger(getClass()).error("Desktop NOT supported: " + message);

                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message);
            }
        } catch (IOException e) {
            String message = "Could not Open URL in System Browser. Try visit Page Manually: " + uri;
            LoggerFactory.getLogger(getClass()).error(message, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message);
        }
    }
}
