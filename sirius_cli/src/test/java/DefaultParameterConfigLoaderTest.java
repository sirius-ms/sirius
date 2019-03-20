import de.unijena.bioinf.ms.cli.parameters.RootOptionsCLI;
import de.unijena.bioinf.ms.cli.parameters.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.sirius.core.ApplicationCore;
import org.apache.commons.configuration2.CombinedConfiguration;
import org.junit.Test;
import picocli.CommandLine;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class DefaultParameterConfigLoaderTest {
    public static final String SINGLE_VALUE = "tw_blub_bla";
    public static final String LIST_VALUE = "l1, l 2 , l3 ll";

    @Test
    public void singleValueTest() throws IOException {
        {
            String c = ApplicationCore.CITATION;
            CombinedConfiguration p = PropertyManager.PROPERTIES;
        }
        final DefaultParameterConfigLoader builder = new DefaultParameterConfigLoader();
        final List<CommandLine.Model.OptionSpec> options = new ArrayList<>(builder.getOptions().values());
        final CommandLine.Model.CommandSpec spec = CommandLine.Model.CommandSpec.forAnnotatedObject(new RootOptionsCLI());
        for (CommandLine.Model.OptionSpec option : options) {
            spec.addOption(option);
        }



        Properties defaults = new Properties();
        try (InputStream stream = ApplicationCore.class.getResourceAsStream("/custom.config")) {
            defaults.load(stream);
        }

        final List<String> argList = new ArrayList<>();

        List<String> singleKeys = new ArrayList<>();
        List<String> listKeys = new ArrayList<>();
        defaults.entrySet().stream().forEach(e -> {
            argList.add("--" + e.getKey());
            if (((String) e.getValue()).contains(",")) {
                listKeys.add((String) e.getKey());
                argList.add(LIST_VALUE);
            } else {
                singleKeys.add((String) e.getKey());
                argList.add(SINGLE_VALUE);
            }
        });
//        argList.add("/fantasy/path/tmp");

        final String[] args = argList.toArray(new String[0]);

//        final CommandLine cm  = CommandLine. spec.commandLine();
        final CommandLine cm = new CommandLine(spec);
        cm.parseArgs(args);
//        builder.overrideDefaults();


        singleKeys.stream().forEach(key -> {
            assertEquals(SINGLE_VALUE, PropertyManager.PROPERTIES.getProperty(key));
        });
        listKeys.stream().forEach(key -> {
            assertEquals(
                    LIST_VALUE.replaceAll("\\s+", ""),
                    PropertyManager.PROPERTIES.getString(key).replaceAll("\\s+", ""));
        });
    }

}
