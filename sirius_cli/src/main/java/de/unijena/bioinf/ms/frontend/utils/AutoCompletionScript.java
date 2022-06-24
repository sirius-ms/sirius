package de.unijena.bioinf.ms.frontend.utils;

import de.unijena.bioinf.fingerid.utils.FingerIDProperties;
import de.unijena.bioinf.ms.frontend.DefaultParameter;
import de.unijena.bioinf.ms.frontend.subtools.CLIRootOptions;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.frontend.workflow.SimpleInstanceBuffer;
import de.unijena.bioinf.ms.frontend.workflow.WorkflowBuilder;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.projectspace.ProjectSpaceManagerFactory;
import org.jetbrains.annotations.NotNull;
import picocli.AutoComplete;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "generateAutocompletion", description = "<STANDALONE> generates an Autocompletion-Script with the given depth of subcommands",
    mixinStandardHelpOptions = true)
public class AutoCompletionScript implements Callable<Integer> {

    @CommandLine.Parameters(index = "0",description = "Maximum depth of subcommands" ,defaultValue = "5")
    private static int depth;
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
        //setRecursionDepthLimit(commandline, depth);
        String s = AutoComplete.bash("sirius", commandline);
        System.out.printf("AutocompletionScript created successfully at %s%n", PATH);
        Files.writeString(PATH, s);
        s = formatScript();
        Files.writeString(PATH, s);
        System.out.printf("Please install the Script temporarily by typing the following into the Terminal: "+ (char)27 + "[1m. %s%n", NAME);
        return 0;
    }
    private @NotNull String formatScript() throws IOException {
        StringBuilder output = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(String.valueOf(PATH)));
        String line;
        while ((line = reader.readLine()) != null) {
            String[] words = line.split(" ");

            // find completion_script function
            if (words.length > 1 && Objects.equals(words[0], "function") && words[1].equals("_complete_sirius()")) {
                output.append(line).append("\n");
                int defaultlength = 16;
                while ((line = reader.readLine()) != null) {
                    words = line.split(" ");

                    if (line.equals("  # Find the longest sequence of subcommands and call the bash function for that subcommand.")) {
                        // end of completion_script function
                        output.append(formatLocalCommandDef(reader));
                        break;
                    }

                    if (words.length <= defaultlength+depth) {
                        // line small enough
                        output.append(line).append("\n");
                    }
                }
            }



            // line uninteresting
            output.append(line).append("\n");
        }
        return output.toString();
    }

    private @NotNull String formatLocalCommandDef(@NotNull BufferedReader reader) throws IOException {
        String[] words;
        HashSet<Integer> removed = new HashSet<>();
        StringBuilder currentOutput = new StringBuilder();
        String line;
        int defaultlength = 3;
        while ((line = reader.readLine()) != null) {
            words = line.split(" ");
            if(words.length >= 3 && !Objects.equals(words[2], "local")) {
                currentOutput.append(removeCompWords(reader, removed));
                return currentOutput.toString();
            }

            if (words.length <= defaultlength+depth) {
                // line small enough
                currentOutput.append(line).append("\n");
            }
            else {
                removed.add(Integer.valueOf((words[3].split("="))[0].substring(4)));
            }
        }
        return currentOutput.toString();
    }

    private @NotNull String removeCompWords(@NotNull BufferedReader reader, HashSet<Integer> removed) throws IOException {
        String[] words;
        StringBuilder currentOutput = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            words = line.split(" ");

            if (line.equals("# No subcommands were specified; generate completions for the top-level command.") || words.length < 5) {
                return currentOutput.toString();
            }
            String valueHolder = words[4].split("@")[0];
            Integer val = Integer.valueOf(valueHolder.substring(7, valueHolder.length()-1));
            if (!(removed.contains(val))) {
                // don't remove current line
                currentOutput.append(line).append("\n");
            }
        }
        return currentOutput.toString();
    }

    public static void main(String... args) throws IOException {
        int exitCode = new CommandLine(new AutoCompletionScript()).execute(args);
        System.exit(exitCode);
    }
/*
//maybe useful later
    private static Map<String, CommandLine> setRecursionDepthLimit(CommandLine commandline, int remaining_depth) {
        CommandLine.Model.CommandSpec subcommandsSpec = commandline.getCommandSpec();
        if(subcommandsSpec.subcommands().isEmpty()) return new HashMap<>(subcommandsSpec.subcommands());

        //remove Autocompletion Command
        commandline.getCommandSpec().removeSubcommand("generateAutocompletion");

        if(remaining_depth >= depth-1) {
            counter++;
            StringBuilder progress = new StringBuilder();
            for(int i=0; i<counter; i++) progress.append("=");
            for(int i=0; i<commandline.getCommandSpec().subcommands().size() - counter; i++) progress.append(" ");
            System.out.println("|"+progress+"|\r");
        }

        if(remaining_depth < 1) {
            Map<String, CommandLine> commands = new HashMap<>(subcommandsSpec.subcommands());
            commands.forEach((name, subcommand)  -> subcommandsSpec.removeSubcommand(name));
        }
        else {
            subcommandsSpec.subcommands().forEach((name, command) -> {
                Map<String, CommandLine> commands = setRecursionDepthLimit(command, remaining_depth - 1);
                commands.forEach((subname, subcommand) -> {
                    try {
                        if (commandline.getParent() != null) commandline.getParent().getCommandSpec().addSubcommand(subname, subcommand);
                    }
                    catch (CommandLine.DuplicateNameException ignored){};
                });
            });
        }
        return new HashMap<>(subcommandsSpec.subcommands());
    }

 */
}
