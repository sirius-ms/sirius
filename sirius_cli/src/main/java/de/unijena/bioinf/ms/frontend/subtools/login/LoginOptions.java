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
import de.unijena.bioinf.webapi.rest.ConnectionError;
import de.unijena.bioinf.webapi.rest.ProxyManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
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
    @CommandLine.Option(names = "--clear",
            description = {"Delete stored refresh/access token (re-login required to use webservices)"})
    protected boolean clearLogin;


    //SHOW Account info
    @CommandLine.Option(names = "--show",
            description = {"Show profile information about the profile you are logged in with."})
    protected boolean showProfile;

    //SHOW License info
    @CommandLine.Option(names = {"--license-info", "--limits"},
            description = {"Show license information and compound limits."})
    protected boolean showLicense;

    @CommandLine.Option(names = {"--select-license", "--select-subscription"}, required = false,
            description = {"Specify active subscription (sid) if multiple licenses are available at your account. Available subscriptions can be listed with '--show'"})
    protected String sid = null;

    //SET Account
    @CommandLine.ArgGroup(exclusive = false)
    LoginOpts login;

    private static class LoginOpts {
        @CommandLine.Option(names = {"--user", "--email", "-u"}, required = true,
                description = {"Compute fragmentation tree alignments between all compounds in the dataset, incorporating the given fragmentation tree library. The similarity is not the raw alignment score, but the correlation of the scores."})
        protected String username;

        @CommandLine.Option(names = {"--password", "--pwd", "-p"}, required = true,
                description = {"Console password input."},
                interactive = true)
        protected String password;
    }


    @Override
    public LoginWorkflow makeWorkflow(RootOptions<?, ?, ?> rootOptions, ParameterConfig config) {
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


            if (login != null && login.username != null && login.password != null) {
                AuthService service = ApplicationCore.WEB_API.getAuthService();
                try {
                    service.login(login.username, login.password);
                    AuthServices.writeRefreshToken(service, ApplicationCore.TOKEN_FILE);
                    final AuthService.Token token = service.getToken().orElse(null);
                    if (showProfile)
                        showProfile(token);


                    {
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
                    }


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
                            return;
                        }
                        System.out.println();

                    }

                    if (errors.isEmpty()) {
                        Subscription subUsed = ApplicationCore.WEB_API.getActiveSubscription();
                        if (sid != null && sid.equals(subUsed.getSid())) { //make host change persistent because connection was successful
                            SiriusProperties.setAndStoreInBackground(Tokens.ACTIVE_SUBSCRIPTION_KEY, sid);
                        }
                        System.out.println("Login successful! Active License is: '" + subUsed.getSid() + " - " + subUsed.getName() +"'.");
                    }
                } catch (ExecutionException | InterruptedException | IOException e) {
                    LoggerFactory.getLogger(getClass()).error("Could not login to Authentication Server!", e);
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
                System.out.println();
                System.out.println("---- Available Subscriptions ----");
                @NotNull List<Subscription> subs = Tokens.getSubscriptions(token);
                if (subs.isEmpty()) {
                    System.out.println("<NO SUBSCRIPTIONS/LICENSES AVAILABLE>");
                }else{
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
    }
}