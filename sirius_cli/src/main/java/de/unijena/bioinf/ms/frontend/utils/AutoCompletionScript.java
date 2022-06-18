package de.unijena.bioinf.ms.frontend.utils;

import de.unijena.bioinf.fingerid.utils.FingerIDProperties;
import de.unijena.bioinf.ms.frontend.DefaultParameter;
import de.unijena.bioinf.ms.frontend.subtools.CLIRootOptions;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.frontend.workflow.SimpleInstanceBuffer;
import de.unijena.bioinf.ms.frontend.workflow.WorkflowBuilder;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.projectspace.ProjectSpaceManagerFactory;
import picocli.AutoComplete;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "generateAutocompletion", description = "<STANDALONE> generates an Autocompletion-Script with the given depth of parameters",
    mixinStandardHelpOptions = true)
public class AutoCompletionScript implements Callable<Integer> {
    @CommandLine.Parameters(index = "0",description = "Maximum depth of parameters" ,defaultValue = "5")
    private int depth;

    /**
     * Pass this CommandLine instance and the name of the script to the picocli.AutoComplete::bash method.
     * The method will return the source code of a completion script. Save the source code to a file and install it.
     * For the installation of the completion Script, please see the following: <a href="https://picocli.info/autocomplete.html#_install_completion_script">...</a>
     */
    public Integer call() throws IOException{
        //TODO generate completion Script during build: See https://picocli.info/autocomplete.html#_generating_completion_scripts_during_the_build

        String NAME = "SiriusLinuxCompletionScript";
        Path PATH = Path.of(String.format("./sirius_cli/scripts/%s",NAME));
        System.setProperty("de.unijena.bioinf.ms.propertyLocations", "sirius_frontend.build.properties");
        FingerIDProperties.sirius_guiVersion();
        final DefaultParameterConfigLoader configOptionLoader = new DefaultParameterConfigLoader(PropertyManager.DEFAULTS);
        WorkflowBuilder<CLIRootOptions<ProjectSpaceManager>> builder = new WorkflowBuilder<>(new CLIRootOptions<>(configOptionLoader, new ProjectSpaceManagerFactory.Default()), configOptionLoader, new SimpleInstanceBuffer.Factory());
        builder.initRootSpec();
        CommandLine commandline = new CommandLine(builder.getRootSpec());
        commandline.setCaseInsensitiveEnumValuesAllowed(true);
        commandline.registerConverter(DefaultParameter.class, new DefaultParameter.Converter());
        System.out.println(String.format("Creating AutocompletionScript of length %d", depth));
        setRecursionDepthLimit(commandline, depth);
        String s = AutoComplete.bash("sirius", commandline);
        System.out.println(String.format("AutocompletionScript created successfull at %s", PATH));
        Files.writeString(PATH, s);
        System.out.println(String.format("Please install the Script temporarly by typing the following into the Terminal: "+ (char)27 + "[1m. %s", NAME));
        return 0;
    }

    public static void main(String... args) throws IOException {
        int exitCode = new CommandLine(new AutoCompletionScript()).execute(args);
        System.exit(exitCode);
    }

    private static void setRecursionDepthLimit(CommandLine commandline, int remaining_depth) {
        CommandLine.Model.CommandSpec subcommandsSpec = commandline.getCommandSpec();
        if(subcommandsSpec.subcommands().isEmpty()) return;
        if(remaining_depth < 1) {
            Set<String> commands = subcommandsSpec.subcommands().keySet();
            while(!commands.isEmpty()){
                subcommandsSpec.removeSubcommand(commands.stream().iterator().next());
                commands = subcommandsSpec.subcommands().keySet();
                }
            }
        else {
            subcommandsSpec.subcommands().forEach((name, command) -> setRecursionDepthLimit(command, remaining_depth - 1));
        }
    }
}
