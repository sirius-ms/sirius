package de.unijena.bioinf.ms.frontend.utils;

import de.unijena.bioinf.fingerid.utils.FingerIDProperties;
import de.unijena.bioinf.ms.frontend.DefaultParameter;
import de.unijena.bioinf.ms.frontend.subtools.CLIRootOptions;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.frontend.workflow.SimpleInstanceBuffer;
import de.unijena.bioinf.ms.frontend.workflow.WorkflowBuilder;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.projectspace.ProjectSpaceManagerFactory;
import picocli.AutoComplete;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@CommandLine.Command(name = "generateAutocompletion", description = "<STANDALONE> generates an Autocompletion-Script with the given depth of subcommands",
    mixinStandardHelpOptions = true)
public class AutoCompletionScript implements Callable<Integer> {

    @CommandLine.Parameters(index = "0",description = "Maximum depth of subcommands" ,defaultValue = "5")
    private int depth;

    private static final String NAME = "SiriusLinuxCompletionScript";
    private static final Path PATH = Path.of(String.format("./sirius_cli/scripts/%s",NAME));


    /**
     * Pass this CommandLine instance and the name of the script to the picocli.AutoComplete::bash method.
     * The method will return the source code of a completion script. Save the source code to a file and install it.
     * For the installation of the completion Script, please see the following: <a href="https://picocli.info/autocomplete.html#_install_completion_script">...</a>
     */
    public Integer call() throws IOException{
        //TODO generate completion Script during build: See https://picocli.info/autocomplete.html#_generating_completion_scripts_during_the_build
        System.setProperty("de.unijena.bioinf.ms.propertyLocations", "sirius_frontend.build.properties");
        FingerIDProperties.sirius_guiVersion();
        final DefaultParameterConfigLoader configOptionLoader = new DefaultParameterConfigLoader();
        WorkflowBuilder<CLIRootOptions<ProjectSpaceManager>> builder = new WorkflowBuilder<>(new CLIRootOptions<>(configOptionLoader, new ProjectSpaceManagerFactory.Default()), configOptionLoader, new SimpleInstanceBuffer.Factory());
        builder.initRootSpec();
        CommandLine commandline = new CommandLine(builder.getRootSpec());
        commandline.setCaseInsensitiveEnumValuesAllowed(true);
        commandline.registerConverter(DefaultParameter.class, new DefaultParameter.Converter());
        System.out.printf("Creating AutocompletionScript of length %d%n", depth);
        setRecursionDepthLimit(commandline, depth);
        String s = AutoComplete.bash("sirius", commandline);
        System.out.printf("AutocompletionScript created successfully at %s%n", PATH);
        Files.writeString(PATH, s);
        System.out.printf("Please install the Script temporarily by typing the following into the Terminal: "+ (char)27 + "[1m. %s%n", NAME);
        return 0;
    }

    public static void main(String... args) throws IOException {
        int exitCode = new CommandLine(new AutoCompletionScript()).execute(args);
        System.exit(exitCode);
    }

    private static void setRecursionDepthLimit(CommandLine commandline, int remaining_depth) {
        CommandLine.Model.CommandSpec subcommandsSpec = commandline.getCommandSpec();
        if(subcommandsSpec.subcommands().isEmpty()) return;

        //remove Autocompletion Command
        commandline.getCommandSpec().removeSubcommand("generateAutocompletion");

        //remove aliases
        HashSet<String> nonAliases = new HashSet<>();
        commandline.getCommandSpec().subcommands().forEach((name, subcommand) -> nonAliases.add(subcommand.getCommandSpec().name()));
        HashSet<String> keyset = new HashSet<>(commandline.getCommandSpec().subcommands().keySet());
        Set<String> aliases = keyset.stream().filter(name -> !(nonAliases.contains(name))).collect(Collectors.toSet());
        //TODO remove the alias but not the underlying command
        //aliases.forEach(alias -> commandline.getCommandSpec().removeSubcommand(alias));

        if(remaining_depth < 1) {
            Set<String> commands = subcommandsSpec.subcommands().keySet();
            commands = new HashSet<>(commands);
            commands.forEach(subcommand  -> subcommandsSpec.removeSubcommand(subcommand));
        }
        else {
            subcommandsSpec.subcommands().forEach((name, command) -> setRecursionDepthLimit(command, remaining_depth - 1));
        }
    }
}
