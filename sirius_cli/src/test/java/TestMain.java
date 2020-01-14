import de.unijena.bioinf.ms.frontend.subtools.CLIRootOptions;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import picocli.CommandLine;

import java.io.IOException;

public class TestMain {
    public static void main(String[] args) throws IOException {
        String t = ApplicationCore.VERSION_STRING();
        CLIRootOptions basicOptions = new CLIRootOptions(new DefaultParameterConfigLoader());
        final CommandLine.Model.CommandSpec spec = CommandLine.Model.CommandSpec.forAnnotatedObject(basicOptions);
//        spec.addMixin("SIRIUS", CommandLine.Model.CommandSpec.forAnnotatedObject(siriusOptions));
//        spec.addMixin("Zodiac", CommandLine.Model.CommandSpec.forAnnotatedObject(zodiacOptions));
//        spec.addMixin("FingerID", CommandLine.Model.CommandSpec.forAnnotatedObject(fingeridOptions));
//        spec.addMixin("Canopus", CommandLine.Model.CommandSpec.forAnnotatedObject(canopusOptions));
        spec.usageMessage().footerHeading("Please cite the following publications when using our tool:");
        spec.usageMessage().footer(ApplicationCore.CITATION);
        final CommandLine.ParseResult parseResult = spec.commandLine().parseArgs(args);
        System.out.println(parseResult);
    }
}
