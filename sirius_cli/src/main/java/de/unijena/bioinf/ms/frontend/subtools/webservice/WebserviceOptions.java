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

package de.unijena.bioinf.ms.frontend.subtools.webservice;

import com.auth0.jwt.interfaces.DecodedJWT;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.frontend.subtools.StandaloneTool;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import org.jetbrains.annotations.Nullable;
import picocli.CommandLine;


@CommandLine.Command(name = "webservice", description = "<STANDALONE> Show info about the web service like available workers, pending jobs and personal usage stats.", versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, showDefaultValues = true)
public class WebserviceOptions implements StandaloneTool<WebserviceOptions.WF> {


    @CommandLine.Option(names = {"-w", "--workers"},
            description = {"Show available workers."})
    protected boolean workerInfo;

    @CommandLine.Option(names = "--user-stats",
            description = {"Show profile information about the profile you are logged in with."})
    protected boolean userStats;

    @CommandLine.Option(names = {"--pending-jobs"},
            description = {"Return  number of pending jobs in th Cloud"})
    protected boolean jobs;


    @Override
    public WF makeWorkflow(RootOptions<?, ?, ?, ?> rootOptions, ParameterConfig config) {
        return new WF();
    }

    public class WF implements Workflow {
        @Override
        public void run() {
            /*if (clearLogin) {
                try {
                    ApplicationCore.WEB_API
                } catch (IOException e) {
                    LoggerFactory.getLogger(getClass()).error("Error when clearing refresh token.", e);
                }
                return;
            }


            if (username != null && password != null) {
                try {
                    AuthService service = AuthServices.createDefault(ApplicationCore.TOKEN_FILE);
                    try {
                        service.login(username, password);
                        AuthServices.writeRefreshToken(service, ApplicationCore.TOKEN_FILE);
                        if (showProfile)
                            showProfile(AuthServices.getIDToken(service));
                    } catch (ExecutionException | InterruptedException | IOException e) {
                        LoggerFactory.getLogger(getClass()).error("Could not login to Authentication Server!", e);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            } else if (showProfile) {
                try {
                    AuthService service = AuthServices.createDefault(ApplicationCore.TOKEN_FILE);
                    showProfile(AuthServices.getIDToken(service));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }*/
        }

        private void showProfile(@Nullable DecodedJWT decoded) {

            System.out.println("####################### Login Info #######################");
            if (decoded != null) {
                System.out.println("Logged in as: " + decoded.getClaim("name"));
                System.out.println("Token expires at: " + decoded.getExpiresAt().toString());
            } else {
                System.out.println("Not logged in.");
            }
            System.out.println("##########################################################");
        }
    }
}

