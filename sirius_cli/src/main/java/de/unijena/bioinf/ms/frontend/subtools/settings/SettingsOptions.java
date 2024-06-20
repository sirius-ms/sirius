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

package de.unijena.bioinf.ms.frontend.subtools.settings;

import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.frontend.subtools.StandaloneTool;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import picocli.CommandLine;

import java.util.List;

@CommandLine.Command(name = "settings", description = "<STANDALONE> Configure persistent (technical) settings of SIRIUS (e.g. ProxySettings or ILP Solver). %n %n", versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, showDefaultValues = true)
public class SettingsOptions implements StandaloneTool<SettingsOptions.SettingsWorkflow> {

    @CommandLine.ArgGroup(exclusive = false, multiplicity = "0..*", heading = "@|bold %n Generic key-value pairs of properties. %n|@")
    private List<Property> properties;

    @CommandLine.ArgGroup(exclusive = false, heading = "@|bold %n Properties to configure Proxy server for SIRIUS. %n|@")
    ProxyProperties proxyProperties;

    private static class Property {
        @CommandLine.Option(names = {"--key", "-k"}, required = true,
                description = "Property Key",
                order = 201)
        String key;
        @CommandLine.Option(names = {"--value", "-v"}, required = true,
                description = "Property Value",
                order = 211)
        String value;
    }

    private static class ProxyProperties {
        @CommandLine.Option(names = "--proxy", required = true,
                description = "Define proxy settings.",
                order = 301)
        boolean useProxy;
        @CommandLine.Option(names = "--host", required = true,
                description = "Proxy domain.",
                order = 311)
        String hostname;
        @CommandLine.Option(names = "--port", required = true,
                description = "Proxy port.",
                order = 321)
        int port;

        @CommandLine.ArgGroup(exclusive = false)
        Credentials credentials;
    }

    private static class Credentials {
        @CommandLine.Option(names = "--user",
                description = "Proxy user (only if proxy server needs credentials.",
                required = true,
                order = 341)
        String user;

        @CommandLine.Option(names = {"--password", "--pw"},
                description = "Proxy password (only if proxy server needs credentials.",
                required = true,
                order = 351)
        String password;
    }

    @Override
    public SettingsWorkflow makeWorkflow(RootOptions<?> rootOptions, ParameterConfig config) {
        return new SettingsWorkflow();
    }

    public class SettingsWorkflow implements Workflow {
        @Override
        public void run() {
            if (properties != null && !properties.isEmpty()) {
                properties.forEach(p -> {
                    if (p.key != null && !p.key.isBlank())
                        SiriusProperties.SIRIUS_PROPERTIES_FILE().setProperty(p.key, p.value);
                });
            }

            if (proxyProperties != null) {
                if (proxyProperties.useProxy)
                    SiriusProperties.SIRIUS_PROPERTIES_FILE().setProperty("de.unijena.bioinf.sirius.proxy", "SIRIUS");
                else
                    SiriusProperties.SIRIUS_PROPERTIES_FILE().setProperty("de.unijena.bioinf.sirius.proxy", "NONE");

                SiriusProperties.SIRIUS_PROPERTIES_FILE().setProperty("de.unijena.bioinf.sirius.proxy.hostname", proxyProperties.hostname);
                SiriusProperties.SIRIUS_PROPERTIES_FILE().setProperty("de.unijena.bioinf.sirius.proxy.port", String.valueOf(proxyProperties.port));

                //credentials
                if (proxyProperties.credentials != null) {
                    SiriusProperties.SIRIUS_PROPERTIES_FILE().setProperty("de.unijena.bioinf.sirius.proxy.credentials", "true");
                    SiriusProperties.SIRIUS_PROPERTIES_FILE().setProperty("de.unijena.bioinf.sirius.proxy.user", proxyProperties.credentials.user);
                    SiriusProperties.SIRIUS_PROPERTIES_FILE().setProperty("de.unijena.bioinf.sirius.proxy.pw", proxyProperties.credentials.password);
                } else {
                    SiriusProperties.SIRIUS_PROPERTIES_FILE().setProperty("de.unijena.bioinf.sirius.proxy.credentials", "false");
                    SiriusProperties.SIRIUS_PROPERTIES_FILE().setProperty("de.unijena.bioinf.sirius.proxy.user", null);
                    SiriusProperties.SIRIUS_PROPERTIES_FILE().setProperty("de.unijena.bioinf.sirius.proxy.pw", null);
                }
            }

            SiriusProperties.SIRIUS_PROPERTIES_FILE().store();
        }
    }
}
