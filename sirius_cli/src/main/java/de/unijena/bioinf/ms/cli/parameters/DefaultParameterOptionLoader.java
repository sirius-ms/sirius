package de.unijena.bioinf.ms.cli.parameters;

import de.unijena.bioinf.ms.properties.PropertyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DefaultParameterOptionLoader {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultParameterOptionLoader.class);
    private final Map<String, CommandLine.Model.OptionSpec> options;
    private CommandLine.Model.CommandSpec commandSpec = null;


    public DefaultParameterOptionLoader() throws IOException {
        options = loadDefaultParameterOptions();
    }

    private Map<String, CommandLine.Model.OptionSpec> loadDefaultParameterOptions() throws IOException {

        return PropertyManager.DEFAULTS.getDefaultPropertyKeys().stream().map((key) -> {
            final String value = PropertyManager.getStringProperty(key);
            final String descr = PropertyManager.DEFAULTS.getDefaultPropertyDescription(key);
            CommandLine.Model.OptionSpec.Builder pSpec = CommandLine.Model.OptionSpec
                    .builder("--" + key.replace(PropertyManager.DEFAULTS.configRoot + ".", ""))
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
                                PropertyManager.DEFAULTS.changeDefault(key, v);
                                return value;
                            }
                        });

            } else {
                pSpec.type(String.class)
                        .setter(new CommandLine.Model.ISetter() {
                            @Override
                            public <T> T set(T value) throws Exception {
                                LOG.debug("Changing DEFAULT:" + key + " -> " + value);
                                PropertyManager.DEFAULTS.changeDefault(key, String.valueOf(value));
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
            CommandLine.Model.CommandSpec spec = CommandLine.Model.CommandSpec.create();
            spec.name("config");
            spec.versionProvider(new Provide.Versions());
            spec.defaultValueProvider(new Provide.Defaults());
            spec.mixinStandardHelpOptions(true); // usageHelp and versionHelp options
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
}
