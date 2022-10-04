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

package de.unijena.bioinf.ms.frontend.subtools.login;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Multimap;
import de.unijena.bioinf.auth.AuthService;
import de.unijena.bioinf.auth.AuthServices;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.frontend.subtools.StandaloneTool;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.ms.rest.model.info.LicenseInfo;
import de.unijena.bioinf.ms.rest.model.info.Term;
import de.unijena.bioinf.ms.rest.model.license.Subscription;
import de.unijena.bioinf.ms.rest.model.license.SubscriptionConsumables;
import de.unijena.bioinf.webapi.Tokens;
import de.unijena.bioinf.webapi.WebAPI;
import de.unijena.bioinf.rest.ConnectionError;
import de.unijena.bioinf.rest.ProxyManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@CommandLine.Command(name = "login", description = "<STANDALONE> Allows a user to login for SIRIUS Webservices (e.g. CSI:FingerID or CANOPUS) and securely store a personal access token.", versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, showDefaultValues = true)
public class LoginOptions implements StandaloneTool<LoginOptions.LoginWorkflow> {

    // RESET Account password
    @CommandLine.Option(names = "--reset-password",
            description = {"Delete stored refresh/access token (re-login required to use webservices)"})
    protected String emailToReset;

    // DELETE Account
    @CommandLine.Option(names = {"--logout", "--clear"},
            description = {"Logout. Deletes stored refresh and access token (re-login required to use webservices again)."})
    protected boolean clearLogin;


    //SHOW Account info
    @CommandLine.Option(names = "--show",
            description = {"Show profile information about the profile you are logged in with."})
    protected boolean showProfile;

    //SHOW License info
    @CommandLine.Option(names = {"--license-info", "--limits"},
            description = {"Show license information and compound limits."})
    protected boolean showLicense;

    @CommandLine.Option(names = {"--select-license", "--select-subscription"},
            description = {"Specify active subscription (sid) if multiple licenses are available at your account. Available subscriptions can be listed with '--show'"})
//    protected int sidIndex = -1;
    protected String sid = null;

    @CommandLine.Option(names = {"--request-token-only"},
            description = {"Requests and prints a new SECRET refresh token but does not store the token as login.", "This can be used to request a token to be used in third party applications that wish to call SIRIUS Web Services using your account.", "Do never store your username and password in third party apps.", "Do not store the output of this command in any log. We recommend redirecting the output into a file."})
    protected boolean tokenRequestOnly = false;

    //todo token invalidation command.

    //SET Account
    //only one of the login methods can be used.
    @CommandLine.ArgGroup(exclusive = true)
    LoginOpts login;

    private static class LoginOpts {
        @CommandLine.ArgGroup(exclusive = false)
        private InteractiveLoginOpts interactiveLogin = null;
        @CommandLine.ArgGroup(exclusive = false)
        private TokenLoginOpts tokenLogin = null;
        @CommandLine.ArgGroup(exclusive = false)
        private BatchLoginOpts batchLogin = null;

        public boolean isTokenAuth() {
            return tokenLogin != null;
        }

        public String getRefreshToken() {
            if (!isTokenAuth())
                return null;
            return tokenLogin.token;
        }

        public String getUsername() {
            if (interactiveLogin != null)
                return interactiveLogin.username;
            if (batchLogin != null)
                return batchLogin.username;
            return null;
        }

        public String getPassword() {
            if (interactiveLogin != null)
                return interactiveLogin.password;
            if (batchLogin != null)
                return batchLogin.password;
            return null;
        }


    }

    private static class InteractiveLoginOpts {
        @CommandLine.Option(names = {"--user", "--email", "-u"}, required = true,
                description = {"Login username/email"})
        protected String username;

        @CommandLine.Option(names = {"--password", "--pwd", "-p"}, required = true,
                description = {"Console password input."},
                interactive = true)
        protected String password;
    }

    private static class TokenLoginOpts {
        @CommandLine.Option(names = {"--token"}, required = true,
                description = {"Refresh token to use as login."})
        protected String token;
    }

    private static class BatchLoginOpts {
        @CommandLine.Option(names = {"--user-env"}, required = true,
                description = {"Environment variable with login username."})
        private void setUsername(String envUsername) {
            this.username = System.getenv(envUsername);
        }

        protected String username;


        @CommandLine.Option(names = {"--password-env"}, required = true,
                description = {"Environment variable with login password."})
        private void setPassword(String envPassword) {
            this.password = System.getenv(envPassword);
        }

        protected String password;
    }


    @Override
    public LoginWorkflow makeWorkflow(RootOptions<?, ?, ?, ?> rootOptions, ParameterConfig config) {
        return new LoginWorkflow();
    }

    public class LoginWorkflow implements Workflow {
        @Override
        public void run() {
            PropertyManager.DEFAULTS.changeConfig("PrintCitations", "FALSE");
            if (clearLogin) {
                try {
                    AuthServices.clearRefreshToken(ApplicationCore.TOKEN_FILE);
                    System.out.println("Token successfully removed. You are now logged out!");
                } catch (IOException e) {
                    LoggerFactory.getLogger(getClass()).error("Error when clearing refresh token.", e);
                }
                return;
            }

            if (emailToReset != null) {
                try {
                    if (!emailToReset.contains("@"))
                        throw new IllegalArgumentException("'" + emailToReset + "' id not a valid email address! No password reset request sent.");
                    ApplicationCore.WEB_API.getAuthService().sendPasswordReset(emailToReset);
                    System.out.println("Password reset request sent to '" + emailToReset + "'.");
                } catch (IOException | ExecutionException | InterruptedException e) {
                    LoggerFactory.getLogger(getClass()).error("Error when sending password reset request.", e);
                }
                return;
            }


            if (login != null) {
                AuthService service = ApplicationCore.WEB_API.getAuthService();
                try {
                    if (login.isTokenAuth())
                        service.login(login.getRefreshToken());
                    else
                        service.login(login.getUsername(), login.getPassword());

                    if (tokenRequestOnly) {
                        String rToken = service.getToken().map(t -> t.getSource().getRefreshToken()).orElseThrow(() -> new IOException("Could not extract refresh token after successful login!"));
                        System.out.println("###################### Refresh token ######################");
                        System.out.println(rToken);
                        System.out.println("###########################################################");
                    }
                    AuthServices.writeRefreshToken(service, ApplicationCore.TOKEN_FILE);
                    final AuthService.Token token = service.getToken().orElse(null);
                    if (showProfile)
                        showProfile(token);

                    Multimap<ConnectionError.Klass, ConnectionError> errors = determineAndCheckActiveSubscription(token);
                    if (errors.isEmpty())
                        System.out.println("Login successful!");

                } catch (ExecutionException | InterruptedException | IOException e) {
                    LoggerFactory.getLogger(getClass()).error("Could not login to Authentication Server!", e);
                }
            } else if (sid != null) {
                try {
                    AuthService service = AuthServices.createDefault(
                            URI.create(SiriusProperties.getProperty("de.unijena.bioinf.sirius.security.audience")),
                            ApplicationCore.TOKEN_FILE,
                            ProxyManager.getSirirusHttpAsyncClient());
                    final AuthService.Token token = service.getToken().orElseThrow(() -> new IllegalStateException("Not logged in! Please log in to select a license!"));
                    determineAndCheckActiveSubscription(token);
                    if (showProfile)
                        showProfile(service.getToken().orElse(null));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else if (showProfile) {
                try {
                    AuthService service = AuthServices.createDefault(
                            URI.create(SiriusProperties.getProperty("de.unijena.bioinf.sirius.security.audience")),
                            ApplicationCore.TOKEN_FILE,
                            ProxyManager.getSirirusHttpAsyncClient());
                    showProfile(service.getToken().orElse(null));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            if (showLicense)
                try {
                    showLicense();
                } catch (IOException e) {
                    throw new RuntimeException("Error when requesting license information.", e);
                }
        }

        private void showProfile(@Nullable AuthService.Token token) {
            System.out.println();
            System.out.println("####################### Login Info #######################");
            if (token != null) {
                DecodedJWT decodedId = token.getDecodedIdToken();
                System.out.println("Logged in as: " + decodedId.getClaim("name").asString());
                System.out.println("User ID: " + decodedId.getClaim("sub").asString());
                System.out.println("Token expires at: " + decodedId.getExpiresAt().toString());
                System.out.println("Active subscription: " + Optional.ofNullable(Tokens.getActiveSubscription(token)).map(Subscription::getSid).orElse("NONE"));
                System.out.println();
                System.out.println("---- Available Subscriptions ----");
                @NotNull List<Subscription> subs = Tokens.getSubscriptions(token);
                if (subs.isEmpty()) {
                    System.out.println("<NO SUBSCRIPTIONS/LICENSES AVAILABLE>");
                } else {
                    try {
                        System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(subs));
                    } catch (JsonProcessingException e) {
                        LoggerFactory.getLogger(getClass()).error("Error when printing available licenses!", e);
                    }
                }
            } else {
                System.out.println("Not logged in.");
            }
            System.out.println("##########################################################");
            System.out.println();
        }

        private void showLicense() throws IOException {
            WebAPI<?> api = ApplicationCore.WEB_API;
            @Nullable Subscription subs = Tokens.getActiveSubscription(api.getAuthService().getToken().orElse(null));

            System.out.println();
            System.out.println("###################### License Info ######################");
            if (subs != null) {
                final LicenseInfo licenseInfo = new LicenseInfo();
                licenseInfo.setSubscription(subs);
                System.out.println("Licensed to: " + subs.getSubscriberName() + " (" + subs.getDescription() + ")");
                System.out.println("Expires at: " + (subs.hasExpirationTime() ? subs.getExpirationDate().toString() : "NEVER"));
                if (subs.getCountQueries()) {
                    if (subs.hasCompoundLimit()) {
                        licenseInfo.setConsumables(api.getConsumables(false));
                        System.out.println("Compounds Computed (Yearly): " + licenseInfo.consumables()
                                .map(SubscriptionConsumables::getCountedCompounds)
                                .map(String::valueOf).orElse("?") + " of " + subs.getCompoundLimit());
                    } else {
                        licenseInfo.setConsumables(api.getConsumables(true));
                        System.out.println("Compounds Computed (Monthly): " + licenseInfo.consumables().map(SubscriptionConsumables::getCountedCompounds)
                                .map(String::valueOf).orElse("?"));
                    }
                }
            } else {
                System.out.println("Not License information found.");
            }
            System.out.println("##########################################################");
            System.out.println();
        }

        private Multimap<ConnectionError.Klass, ConnectionError> determineAndCheckActiveSubscription(AuthService.Token token) throws IOException {
            Subscription sub = null;
            @NotNull List<Subscription> subs = Tokens.getSubscriptions(token);
            if (sid != null)
                sub = Tokens.getActiveSubscription(subs, sid, false);
            if (sub == null) {
                if (sid != null)
                    LoggerFactory.getLogger(getClass()).debug("Could not find subscription with sid '"
                            + sid + "'. Trying to find fallback");
                sub = Tokens.getActiveSubscription(subs);
            }
            ApplicationCore.WEB_API.changeActiveSubscription(sub);

            //check connection
            Multimap<ConnectionError.Klass, ConnectionError> errors = ApplicationCore.WEB_API.checkConnection();
            LoggerFactory.getLogger(getClass()).debug("Connection check after login returned errors: " +
                    errors.values().stream().sorted(Comparator.comparing(ConnectionError::getSiriusErrorCode))
                            .map(ConnectionError::toString).collect(Collectors.joining(",\n")));

            if (errors.containsKey(ConnectionError.Klass.TERMS)) {
                List<Term> terms = Tokens.getActiveSubscriptionTerms(token);

                System.out.println();
                System.out.println("###################### Accept Terms ######################");
                System.out.println("I agree to the ");
                System.out.println(Term.toText(terms));
                System.out.print("Y(es)|No:  ");
                Scanner scanner = new Scanner(System.in);
                String answer = scanner.next();
                System.out.println("##########################################################");
                if (answer.equalsIgnoreCase("Y") || answer.equalsIgnoreCase("YES")) {
                    ApplicationCore.WEB_API.acceptTermsAndRefreshToken();
                    System.out.println("Terms accepted! Checking web service permissions...");
                    errors = ApplicationCore.WEB_API.checkConnection();
                } else { //not accepted clear account data
                    System.out.println("Terms NOT Accepted! Removing login information. Please re-login and accept terms to use web service based features.");
                    AuthServices.clearRefreshToken(ApplicationCore.TOKEN_FILE);
                    throw new IllegalArgumentException("Terms not Accepted!");
                }
                System.out.println();

            }

            if (errors.isEmpty()) {
                Subscription subUsed = ApplicationCore.WEB_API.getActiveSubscription();
                System.out.println();
                if (sid != null && sid.equals(subUsed.getSid())) { //make host change persistent because connection was successful
                    String old = SiriusProperties.getProperty(Tokens.ACTIVE_SUBSCRIPTION_KEY);
                    SiriusProperties.setAndStoreInBackground(Tokens.ACTIVE_SUBSCRIPTION_KEY, sid);
                    System.out.println("Active Subscription changed from '" + old + "' to '" + sid + "'.");
                } else {
                    System.out.println("Active Subscription is: '" + subUsed.getSid() + " - " + subUsed.getName() + "'.");
                }
            }

            return errors;
        }
    }
}