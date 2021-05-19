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

import com.github.scribejava.apis.openid.OpenIdJsonTokenExtractor;
import com.github.scribejava.apis.openid.OpenIdOAuth2AccessToken;
import com.github.scribejava.core.model.OAuth2AccessToken;
import de.unijena.bioinf.auth.AuthService;
import de.unijena.bioinf.auth.AuthServices;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.frontend.subtools.StandaloneTool;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

@CommandLine.Command(name = "login", description = "<STANDALONE> Allows a user to login for SIRIUS Webservices (e.g. CSI:FingerID or CANOPUS) and securely store a personal access token.", versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, showDefaultValues = true)
public class LoginOptions implements StandaloneTool<LoginOptions.LoginWorkflow> {


    @CommandLine.Option(names = "--clear",
            description = {"Delete stored refresh/access token (re-login required t use webservices)"})
    protected boolean clearLogin;

    @CommandLine.Option(names = "--show",
            description = {"Show profile information about the profile you are logged in with."})
    protected boolean showProfile;

    @CommandLine.Option(names = {"--user", "--email", "-u"},
            description = {"Compute fragmentation tree alignments between all compounds in the dataset, incorporating the given fragmentation tree library. The similarity is not the raw alignment score, but the correlation of the scores."})
    protected String username;

    @CommandLine.Option(names = {"--password", "--pwd", "-p"},
            description = {"Console password input."},
            interactive = true)
    protected String password;


    @Override
    public LoginWorkflow makeWorkflow(RootOptions<?, ?, ?> rootOptions, ParameterConfig config) {
        return new LoginWorkflow();
    }

    public class LoginWorkflow implements Workflow {
        @Override
        public void run() {
            if (clearLogin) {
                try {
                    AuthServices.clearRefreshToken(ApplicationCore.TOKEN_FILE);
                } catch (IOException e) {
                    LoggerFactory.getLogger(getClass()).error("Error when clearing refresh token.", e);
                }
                return;
            }


            if (username != null && password != null) {
                try {
                    AuthService service = AuthServices.createDefault(ApplicationCore.TOKEN_FILE);
                    try{
                        service.login(username, password);
                        AuthServices.writeRefreshToken(service, ApplicationCore.TOKEN_FILE);
                        if (showProfile)
                            showProfile(service.refreshIfNeeded());
                    } catch (ExecutionException | InterruptedException | IOException e) {
                        LoggerFactory.getLogger(getClass()).error("Could not login to Authentication Server!", e);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            } else if (showProfile) {
                try {
                    AuthService service = AuthServices.createDefault(ApplicationCore.TOKEN_FILE);
                    showProfile(service.refreshIfNeeded());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        private void showProfile(OpenIdOAuth2AccessToken token) {
            System.out.println(token.getRawResponse());
            //todo show information from extracted token
        }
    }
}