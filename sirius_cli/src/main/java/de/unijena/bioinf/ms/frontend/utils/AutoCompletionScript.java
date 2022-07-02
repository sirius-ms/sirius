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
import org.jetbrains.annotations.Nullable;
import picocli.AutoComplete;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

@CommandLine.Command(name = "generateAutocompletion", description = " [WIP] <STANDALONE> generates an Autocompletion-Script with the given depth of subcommands",
    mixinStandardHelpOptions = true)
public class AutoCompletionScript implements Callable<Integer> {

    @CommandLine.Parameters(index = "0",description = "Maximum depth of subcommands" ,defaultValue = "5")
    private static int depth;
    private final HashSet<String> aliases = new HashSet<>();
    private final HashSet<Integer> removedDefinitions = new HashSet<>();
    private static final String NAME = "SiriusLinuxCompletionScript";
    private static final Path PATH = Path.of(String.format("./sirius_cli/scripts/%s",NAME));
    private CommandLine commandline;

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
        commandline = new CommandLine(builder.getRootSpec());
        commandline.setCaseInsensitiveEnumValuesAllowed(true);
        commandline.registerConverter(DefaultParameter.class, new DefaultParameter.Converter());
        System.out.printf("Creating AutocompletionScript of length %d%n", depth);
        findAliases(commandline);
        String s = AutoComplete.bash("sirius", commandline);
        System.out.printf("AutocompletionScript created successfully at %s%n", PATH);
        Files.writeString(PATH, s);
        s = formatScript();
        Files.writeString(PATH, s);
        System.out.printf("Please install the Script temporarily by typing the following into the Terminal: "+ (char)27 + "[1m. %s%n", NAME);
        return 0;
    }

    private void findAliases(@NotNull CommandLine currentCommandline) {
        CommandLine.Model.CommandSpec subcommandsSpec = currentCommandline.getCommandSpec();
        if(subcommandsSpec.subcommands().isEmpty()) return;
        Map<String, CommandLine> commands = new HashMap<>(subcommandsSpec.subcommands());

        // add command aliases from this depth to Set
        aliases.addAll(Arrays.asList(subcommandsSpec.aliases()));

        // add subcommand aliases from this depth to Set
        commands.forEach((name, subcommand)  -> aliases.addAll(Arrays.asList(subcommand.getCommandSpec().aliases())));


        // go through further depths
        subcommandsSpec.subcommands().forEach((name, command) -> findAliases(command));
    }


    private @NotNull String formatScript() throws IOException {

        StringBuilder output = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(String.valueOf(PATH)));
        String line;
        HashSet<Integer> removed = new HashSet<>();
        String functionstatus = null;
        while ((line = reader.readLine()) != null) {
            String[] words = line.split(" ");

            // update functionstatus
            functionstatus = getFunctionstatus(line, functionstatus, words);


            // Check functionstatus and format line
            line = formatLine(line, functionstatus, words);

            if(line != null) output.append(line).append("\n");
        }
        return output.toString();
    }

    @Nullable
    private String getFunctionstatus(String line, String functionstatus, String[] words) {
        if (functionstatus == null && words.length > 1 && Objects.equals(words[0], "function") && words[1].equals("_complete_sirius()"))
            functionstatus = "CompletionScriptFunction";
        else if (Objects.equals(functionstatus, "CompletionScriptFunction") && line.equals("  # Find the longest sequence of subcommands and call the bash function for that subcommand."))
            functionstatus = "LocalCommandDef";
        else if(Objects.equals(functionstatus, "LocalCommandDef") && words.length >= 3 && !Objects.equals(words[2], "local"))
            functionstatus = "CompWords";
        else if(Objects.equals(functionstatus, "CompWords") && line.equals("  # No subcommands were specified; generate completions for the top-level command."))
            functionstatus = "EOF";
        return functionstatus;
    }

    private String formatLine(String line, String functionstatus, String[] words) {
        if (functionstatus != null && !functionstatus.equals("EOF")) {
            switch (functionstatus) {
                case "CompletionScriptFunction": {
                    line = formatCompletionFunction(line, words);
                    break;
                }
                case "LocalCommandDef": {
                    line = formatCommandDefinitions(line, words);
                    break;
                }
                case "CompWords": {
                    line = removeCompWords(line, words);
                    break;
                }
            }
        }
        return line;
    }

    private String formatCompletionFunction(@NotNull String line, @NotNull String[] words) {
        if (Arrays.stream(words).anyMatch(word -> aliases.stream().anyMatch(alias -> alias.equals(word)))) line = null;
        if (Arrays.stream(words).anyMatch(word -> aliases.stream().anyMatch(alias -> (alias+"\"").equals(word)))) line = null;
        return line;
    }

    private String formatCommandDefinitions(@NotNull String line, @NotNull String[] words) {
        if (words.length < 4) return line;
        if (!words[2].equals("local")) return line;

        Integer number = Integer.valueOf((words[3].split("=")[0].substring(4)));
        for(String word : words) {
            String[] subwords = word.split("\\p{Punct}");
            for (String subword : subwords) {
                if (aliases.stream().anyMatch(alias -> alias.equals(subword))) {
                    line = null;
                    removedDefinitions.add(number);
                }
            }
        }
        return line;
    }

    private String removeCompWords(@NotNull String line, @NotNull String[] words) {
        System.out.println(line);
        if (words.length < 5) return line;
        String valueHolder = words[4].split("@")[0];
        Integer val = Integer.valueOf(valueHolder.substring(7, valueHolder.length() - 1));
        if (removedDefinitions.contains(val)) line = null;
        return line;
    }






    public static void main(String... args) throws IOException {
        int exitCode = new CommandLine(new AutoCompletionScript()).execute(args);
        System.exit(exitCode);
    }
}
