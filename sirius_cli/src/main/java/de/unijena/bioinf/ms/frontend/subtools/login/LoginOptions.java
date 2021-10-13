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
import de.unijena.bioinf.webapi.ProxyManager;
import de.unijena.bioinf.webapi.WebAPI;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

@CommandLine.Command(name = "login", description = "<STANDALONE> Allows a user to login for SIRIUS Webservices (e.g. CSI:FingerID or CANOPUS) and securely store a personal access token.", versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, showDefaultValues = true)
public class LoginOptions implements StandaloneTool<LoginOptions.LoginWorkflow> {

    // DELETE Account
    @CommandLine.Option(names = "--clear",
            description = {"Delete stored refresh/access token (re-login required to use webservices)"})
    protected boolean clearLogin;


    //SHOW Account info
    @CommandLine.Option(names = "--show",
            description = {"Show profile information about the profile you are logged in with."})
    protected boolean showProfile;

    //SHOW License info
    @CommandLine.Option(names = {"--license", "--limits"},
            description = {"Show license information and compound limits."})
    protected boolean showLicense;


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

        @CommandLine.Option(names = {"--url"}, required = false,
                description = {"Changes base URL of the webservice to be used with the given account."},
                interactive = true)
        protected URI webserviceURL = null;
    }


    @Override
    public LoginWorkflow makeWorkflow(RootOptions<?, ?, ?> rootOptions, ParameterConfig config) {
        return new LoginWorkflow();
    }

    public class LoginWorkflow implements Workflow {
        @Override
        public void run() {
            PropertyManager.DEFAULTS.changeConfig("PrintCitations","FALSE");
            if (clearLogin) {
                try {
                    AuthServices.clearRefreshToken(ApplicationCore.TOKEN_FILE);
                } catch (IOException e) {
                    LoggerFactory.getLogger(getClass()).error("Error when clearing refresh token.", e);
                }
                return;
            }


            if (login != null && login.username != null && login.password != null) {
                AuthService service = ApplicationCore.WEB_API.getAuthService();
                try {
                    service.login(login.username,  login.password);
                    AuthServices.writeRefreshToken(service, ApplicationCore.TOKEN_FILE);
                    if (showProfile)
                        showProfile(AuthServices.getIDToken(service));
                    if  (login.webserviceURL != null){
                        ApplicationCore.WEB_API.changeHost(login.webserviceURL);
                    }
                    //check connection
                    int i = ApplicationCore.WEB_API.checkConnection();
                    LoggerFactory.getLogger(getClass()).debug("Connection check after login returned error code: " + i);
                    if (i == 8){

                        List<Term> terms = ApplicationCore.WEB_API.getTerms();

                        System.out.println();
                        System.out.println("###################### Accept Terms ######################");
                        System.out.println("I agree to the ");
                        System.out.println(Term.toText(terms));
                        System.out.print("Y(es)|No:  ");
                        Scanner scanner = new Scanner(System.in);
                        String answer =  scanner.next();
                        System.out.println("##########################################################");
                        if (answer.equalsIgnoreCase("Y") || answer.equalsIgnoreCase("YES")){
                            ApplicationCore.WEB_API.acceptTermsAndRefreshToken();
                            System.out.println("Terms accepted! Checking web service permissions...");
                            i = ApplicationCore.WEB_API.checkConnection();
                        }else { //not accepted clear account data
                            System.out.println("Terms NOT Accepted! Removing login information. Please re-login and accept terms to use web service based features.");
                            AuthServices.clearRefreshToken(ApplicationCore.TOKEN_FILE);
                            return;
                        }
                        System.out.println();

                    }

                    if (i == 0){
                        if (login.webserviceURL != null){ //make host change persistent because connection was successful
                            SiriusProperties.setAndStoreInBackground("de.unijena.bioinf.fingerid.web.host", login.webserviceURL.toString());
                            System.out.println("Login successful!");
                        }
                    }
                } catch (ExecutionException | InterruptedException | IOException e) {
                    LoggerFactory.getLogger(getClass()).error("Could not login to Authentication Server!", e);
                }
            } else if (showProfile) {
                try {
                    AuthService service = AuthServices.createDefault(ApplicationCore.TOKEN_FILE, ProxyManager.getSirirusHttpAsyncClient());
                    showProfile(AuthServices.getIDToken(service));
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

        private void showProfile(@Nullable DecodedJWT decoded) {
            System.out.println();
            System.out.println("####################### Login Info #######################");
            if (decoded != null) {
                System.out.println("Logged in as: " + decoded.getClaim("name").asString());
                System.out.println("User ID: " + decoded.getClaim("sub").asString());
                System.out.println("Token expires at: " + decoded.getExpiresAt().toString());
            } else {
                System.out.println("Not logged in.");
            }
            System.out.println("##########################################################");
            System.out.println();
        }

        private void showLicense() throws IOException {
            WebAPI api = ApplicationCore.WEB_API;
            final LicenseInfo licenseInfo = api.getLicenseInfo();

            System.out.println();
            System.out.println("###################### License Info ######################");
            if (licenseInfo != null) {
                System.out.println("Licensed to: " + licenseInfo.getLicensee() + " (" + licenseInfo.getDescription() + ")");
                System.out.println("Expires at: " + (licenseInfo.hasExpirationTime() ? licenseInfo.getExpirationDate().toString() : "NEVER"));
                if (licenseInfo.isCountQueries()) {
                    if (licenseInfo.hasCompoundLimit()) {
                        int year = api.getCountedJobs(false);
                        System.out.println("Compounds Computed (Yearly): " + year + " of " + licenseInfo.getCompoundLimit());
                    } else {
                        int month = api.getCountedJobs(true);
                        System.out.println("Compounds Computed (Monthly): " + month);
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