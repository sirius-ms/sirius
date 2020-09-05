/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.frontend.subtools.config;

import de.unijena.bioinf.ms.frontend.DefaultParameter;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class DefaultParameterConfigLoader {
    public static final String CLI_CONFIG_NAME = "CLI_CONFIG";
    private static final Logger LOG = LoggerFactory.getLogger(DefaultParameterConfigLoader.class);
    private final Map<String, CommandLine.Model.OptionSpec> options;
    private CommandLine.Model.CommandSpec commandSpec = null;
    public final ParameterConfig config;


    public DefaultParameterConfigLoader() throws IOException {
        this(PropertyManager.DEFAULTS);
    }

    public DefaultParameterConfigLoader(ParameterConfig baseConfig) throws IOException {
        this.config = baseConfig.newIndependentInstance(CLI_CONFIG_NAME);
        options = loadDefaultParameterOptions();
    }


    private Map<String, CommandLine.Model.OptionSpec> loadDefaultParameterOptions() throws IOException {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(config.getConfigKeys(), Spliterator.ORDERED), false)
                .map((key) -> {
                    final String shortKey = key.replace(config.configRoot + ".", "");
                    final String[] descr = config.getConfigDescription(key).orElse(new String[]{});
                    CommandLine.Model.OptionSpec.Builder pSpec = CommandLine.Model.OptionSpec
                            .builder("--" + shortKey)
                            .description(descr)
                            .paramLabel(PropertyManager.DEFAULTS.getConfigValue(shortKey))
                            .hasInitialValue(false);

                    pSpec.type(String.class)
                            .setter(new CommandLine.Model.ISetter() {
                                @Override
                                public <T> T set(T value) throws Exception {
                                    LOG.debug("Changing DEFAULT:" + key + " -> " + value);
                                    config.changeConfig(key, String.valueOf(value).replaceAll("\\s", ""));
                                    return value;
                                }
                            });
            return pSpec.build();
        }).collect(Collectors.toMap(it -> it.names()[0].replaceFirst("--", ""), it -> it));
    }

    public Map<String, CommandLine.Model.OptionSpec> getOptions() {
        return options;
    }

    public CommandLine.Model.CommandSpec asCommandSpec() {
        if (commandSpec == null) {
            CommandLine.Model.CommandSpec spec = CommandLine.Model.CommandSpec.forAnnotatedObject(new ConfigOptions());
            for (CommandLine.Model.OptionSpec option : options.values()) {
                spec.addOption(option);
            }
            commandSpec = spec;
        }
        return commandSpec;
    }

    public void changeOption(String optionName, DefaultParameter para) throws Exception {
        changeOption(optionName, para.value);
    }

    public void changeOption(String optionName, boolean value) throws Exception {
        changeOption(optionName, String.valueOf(value));
    }

    public void changeOption(String optionName, String value) throws Exception {
        options.get(optionName).setter().set(value);
    }

    public void changeOption(String optionName, List<String> value) throws Exception {
        options.get(optionName).setter().set(value.stream().collect(Collectors.joining(",")));
    }

    @CommandLine.Command(name = "config", description = "<CONFIGURATION> Override all possible default configurations of this toolbox from the command line.", versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true)
    public final class ConfigOptions {
    }
}
