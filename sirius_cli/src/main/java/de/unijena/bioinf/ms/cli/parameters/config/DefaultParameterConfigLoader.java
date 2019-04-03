package de.unijena.bioinf.ms.cli.parameters.config;

import de.unijena.bioinf.ms.cli.parameters.Provide;
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
                    final String value = config.getConfigValue(key);
                    final String descr = config.getConfigDescription(key);
            CommandLine.Model.OptionSpec.Builder pSpec = CommandLine.Model.OptionSpec
                    .builder("--" + key.replace(config.configRoot + ".", ""))
                    .description((descr != null) ? descr.replaceAll(System.lineSeparator()," ").replaceAll("#\\s*","") : "")
                    .hasInitialValue(false);

            if (value.contains(",")) {
                pSpec.type(List.class)
                        .splitRegex(",")
                        .setter(new CommandLine.Model.ISetter() {
                            @Override
                            public <T> T set(T value) throws Exception {
                                final String v = String.join(",", (List<String>) value);
                                LOG.debug("Changing DEFAULT:" + key + " -> " + v);
                                config.changeConfig(key, v);
                                return value;
                            }
                        });

            } else {
                pSpec.type(String.class)
                        .setter(new CommandLine.Model.ISetter() {
                            @Override
                            public <T> T set(T value) throws Exception {
                                LOG.debug("Changing DEFAULT:" + key + " -> " + value);
                                config.changeConfig(key, String.valueOf(value));
                                return value;
                            }
                        });
            }
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

    public void changeOption(String optionName, String value) throws Exception {
        options.get(optionName).setter().set(value);
    }

    public void changeOption(String optionName, List<String> value) throws Exception {
        options.get(optionName).setter().set(value);
    }

    @CommandLine.Command(name = "config", description = "Override all possible default configurations of this toolbox from the command line.", defaultValueProvider = Provide.Defaults.class, versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true)
    public final class ConfigOptions{}
}
