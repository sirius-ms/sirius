package de.unijena.bioinf.ms.cli.parameters;

import de.unijena.bioinf.ms.properties.PropertyManager;
import picocli.CommandLine;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class DefaultParameterOptionLoader {
    private final Properties parsedDefaults = new Properties();
    private final List<CommandLine.Model.OptionSpec> options;
    private CommandLine.Model.CommandSpec commandSpec = null;


    public DefaultParameterOptionLoader() throws IOException {
        options = loadDefaultParameterOptions();
    }

    private List<CommandLine.Model.OptionSpec> loadDefaultParameterOptions() throws IOException {

        return PropertyManager.DEFAULTS.getDefaultPropertyKeys().stream().map((key) -> {
            final String value = PropertyManager.getStringProperty(key);
            final String descr = PropertyManager.DEFAULTS.getDefaultPropertyDescription(key);
            CommandLine.Model.OptionSpec.Builder pSpec = CommandLine.Model.OptionSpec
                    .builder("--" + key.replace(PropertyManager.DEFAULTS.configRoot + ".", ""))
                    .description((descr != null) ? descr.replaceAll(System.lineSeparator()," ").replaceAll("#\\s*","") : "")
                    .hasInitialValue(false)
                    .defaultValue(value);

            if (value.contains(",")) {
                pSpec.type(List.class)
                        .splitRegex(",")
                        .setter(new CommandLine.Model.ISetter() {
                            @Override
                            public <T> T set(T value) throws Exception {
                                PropertyManager.DEFAULTS.changeDefault(key, String.join(",", (List<String>) value));
                                return value;
                            }
                        });

            } else {
                pSpec.type(String.class)
                        .setter(new CommandLine.Model.ISetter() {
                            @Override
                            public <T> T set(T value) throws Exception {
                                PropertyManager.DEFAULTS.changeDefault(key, String.valueOf(value));
//                                return (T) parsedDefaults.setProperty(key, (String) value);
                                return value;
                            }
                        });
            }
            return pSpec.build();
        }).collect(Collectors.toList());
    }

    public void overrideDefaults() {
        PropertyManager.setProperties(parsedDefaults);
    }

    public Properties getParsedDefaults() {
        return parsedDefaults;
    }

    public List<CommandLine.Model.OptionSpec> getOptions() {
        return options;
    }

    public CommandLine.Model.CommandSpec asCommandSpec() {
        if (commandSpec == null) {
            CommandLine.Model.CommandSpec spec = CommandLine.Model.CommandSpec.create();
            spec.name("config");
            spec.versionProvider(new Provide.Versions());
            spec.defaultValueProvider(new Provide.Defaults());
            spec.mixinStandardHelpOptions(true); // usageHelp and versionHelp options
            for (CommandLine.Model.OptionSpec option : options) {
                spec.addOption(option);
            }
            commandSpec = spec;
        }
        return commandSpec;
    }

    /*@CommandLine.Command(name = "config", description = "This allows you to set all configuration from the profile files from the command line.", defaultValueProvider = Provide.Defaults.class, versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true)
    private class DefaultParameterOptions {

    }*/
}
