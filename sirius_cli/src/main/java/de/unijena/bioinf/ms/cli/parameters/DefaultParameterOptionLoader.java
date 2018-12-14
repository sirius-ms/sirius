package de.unijena.bioinf.ms.cli.parameters;

import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.sirius.core.ApplicationCore;
import picocli.CommandLine;

import java.io.IOException;
import java.io.InputStream;
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
        Properties defaults = new Properties();
        try (InputStream stream = ApplicationCore.class.getResourceAsStream("/custom.profile")) {
            defaults.load(stream);
        }
        return defaults.entrySet().stream().map((entry) -> {
            CommandLine.Model.OptionSpec.Builder pSpec = CommandLine.Model.OptionSpec
                    .builder((String) "--" + entry.getKey())
                    .hasInitialValue(false)
                    .defaultValue((String) entry.getValue())
                    .hidden(true); //todo hidden or subtool???

            if (((String) entry.getValue()).contains(",")) {
                pSpec.type(List.class)
                        .splitRegex("\\s+,\\s+")
                        .setter(new CommandLine.Model.ISetter() {
                            @Override
                            public <T> T set(T value) throws Exception {
                                return (T) parsedDefaults.setProperty((String) entry.getKey(),
                                        String.join(",", (List<String>) value));
                            }
                        });

            } else {
                pSpec.type(String.class)
                        .setter(new CommandLine.Model.ISetter() {
                            @Override
                            public <T> T set(T value) throws Exception {
                                return (T) parsedDefaults.setProperty((String) entry.getKey(), (String) value);
                            }
                        });
            }
            return pSpec.build();
        }).collect(Collectors.toList());
    }

    public void overrideDefaults() {
        PropertyManager.PROPERTIES.putAll(parsedDefaults);
    }

    public Properties getParsedDefaults() {
        return parsedDefaults;
    }

    public List<CommandLine.Model.OptionSpec> getOptions() {
        return options;
    }

    public CommandLine.Model.CommandSpec asCommandSpec() {
        if (commandSpec == null) {
            final CommandLine.Model.CommandSpec spec = CommandLine.Model.CommandSpec.forAnnotatedObject(new DefaultParameterOptions());
            for (CommandLine.Model.OptionSpec option : options) {
                spec.addOption(option);
            }
            commandSpec = spec;
        }
        return commandSpec;
    }

    @CommandLine.Command(name = "config", description = "This allows you to set all configuration from the profile files from the command line.")
    private class DefaultParameterOptions {
    }
}
