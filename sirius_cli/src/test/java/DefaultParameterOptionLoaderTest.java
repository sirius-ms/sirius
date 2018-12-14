import de.unijena.bioinf.ms.cli.parameters.BasicOptions;
import de.unijena.bioinf.ms.cli.parameters.DefaultParameterOptionLoader;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.sirius.core.ApplicationCore;
import org.junit.Test;
import picocli.CommandLine;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class DefaultParameterOptionLoaderTest {
    public static final String TEST_VALUE =  "tw_blub_bla";

    @Test
    public void inputVsOutputTest() throws IOException {
        {
            String c = ApplicationCore.CITATION;
            Properties p = PropertyManager.PROPERTIES;
        }
        final DefaultParameterOptionLoader builder = new DefaultParameterOptionLoader();
        final List<CommandLine.Model.OptionSpec> options = builder.getOptions();
        final CommandLine.Model.CommandSpec spec = CommandLine.Model.CommandSpec.forAnnotatedObject(new BasicOptions());
        for (CommandLine.Model.OptionSpec option : options) {
            spec.addOption(option);
        }



        Properties defaults = new Properties();
        try (InputStream stream = ApplicationCore.class.getResourceAsStream("/custom.profile")) {
            defaults.load(stream);
        }

        final List<String> argList = new ArrayList<>();

        defaults.keySet().stream().forEach(key -> {
            argList.add("--"+key);
            argList.add(TEST_VALUE);
        });
        argList.add("/fantasy/path/tmp");

        final String[] args = argList.toArray(new String[0]);

//        final CommandLine cm  = CommandLine. spec.commandLine();
        final CommandLine cm = new CommandLine(spec);
        cm.parseArgs(args);
        builder.overrideDefaults();


        defaults.keySet().stream().forEach(key -> {
            assertEquals(TEST_VALUE,PropertyManager.PROPERTIES.getProperty((String) key));
        });
    }

}
