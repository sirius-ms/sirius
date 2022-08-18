package de.unijena.bioinf.ms.frontend.utils;

import de.unijena.bioinf.fingerid.utils.FingerIDProperties;
import de.unijena.bioinf.ms.frontend.DefaultParameter;
import de.unijena.bioinf.ms.frontend.subtools.CLIRootOptions;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.frontend.utils.Progressbar.ProgressVisualizer;
import de.unijena.bioinf.ms.frontend.utils.Progressbar.ProgressbarDefaultCalculator;
import de.unijena.bioinf.ms.frontend.utils.Progressbar.ProgressbarDefaultVisualizer;
import de.unijena.bioinf.ms.frontend.workflow.SimpleInstanceBuffer;
import de.unijena.bioinf.ms.frontend.workflow.WorkflowBuilder;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.ProjectSpaceManagerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import picocli.AutoComplete;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.Callable;


@Command(name = "install-autocompletion", description = "<INSTALL> generates and installs an Autocompletion-Script with all subcommands.",
        mixinStandardHelpOptions = true)
public class AutoCompletionScript implements Callable<Integer> {

    /**
     * type of installation of the Autocompletion Script
     */
    @ArgGroup()
    public Installationtype install = new Installationtype();
    @Option(names = {"--location", "-l",}, description = "Target directory to store the script file. DEFAULT is the current working directory. Directory must exist.")
    public void setScriptFile(Path scriptDir) {
        this.scriptFile = scriptDir.resolve(NAME);
    }

    public Path scriptFile = Path.of(System.getProperty("user.dir")).resolve(NAME); //current working dir



    /**
     *  type of the current OS
     */
    @Option(names = {"--OStype", "-o"}, description = "Overrides specification of the SystemOS. (Detected automatically per Default) Possibilities: {Linux, Mac, Solaris}")
    public String OS;
    private boolean firstsirius = true;
    private final HashSet<String> aliases = new HashSet<>();
    private final HashSet<Integer> removedDefinitions = new HashSet<>();
    private static final String NAME = "SiriusLinux_completion";
    private CommandLine commandline;
    private boolean validDeclaration;
    private ProgressVisualizer progressbar;
    private boolean subvalidDeclaration;
    /**
     * generates a CompletionScript for the sirius Commandline instance.
     * @return returns 1 if execution was successful
     */
    public Integer call() throws IOException, UnknownOSException {
        System.setProperty("de.unijena.bioinf.ms.propertyLocations", "sirius_frontend.build.properties");
        FingerIDProperties.sirius_guiVersion();
        final DefaultParameterConfigLoader configOptionLoader = new DefaultParameterConfigLoader(PropertyManager.DEFAULTS);
        WorkflowBuilder<?> builder = new WorkflowBuilder<>(new CLIRootOptions<>(configOptionLoader, new ProjectSpaceManagerFactory.Default()), configOptionLoader, new SimpleInstanceBuffer.Factory());
        builder.initRootSpec();
        if (install.toInstall() && this.OS == null) this.OS = detectOS();
        if (install.toInstall()) System.out.println("Detected OS as " + OS);
        commandline = new CommandLine(builder.getRootSpec());
        commandline.setCaseInsensitiveEnumValuesAllowed(true);
        commandline.registerConverter(DefaultParameter.class, new DefaultParameter.Converter());
        System.out.println("Creating AutocompletionScript");
        findAliases(commandline);
        addAliasesEdgeCases();
        String s = AutoComplete.bash("sirius", commandline);
        Files.writeString(scriptFile, s);
        s = formatScript(scriptFile);
        Files.writeString(scriptFile, s);
        this.progressbar.stop();
        System.out.printf("AutocompletionScript created successfully at %s%n", scriptFile);
        if (install.toInstall()) installScript(scriptFile, OS);
        return 1;
    }

    /**
     * calls the necessary function for the Script installation on different OS
     *
     * @param script the Script to be installed
     * @param OS     the current Operating System - Possibilities: {"Linux", "Mac", "Windows", "Solaris"}
     */
    private void installScript(final Path script, final String OS) {
        switch (OS) {
            case "Linux":
                installScriptLinux(script);
                break;
            case "Mac":
                installScriptMac(script);
                break;
            case "Windows":
                installScriptWindows(script);
                break;
            case "Solaris":
                installScriptSolaris(script);
                break;
            default:
                throw new UnknownOSException(String.format("OS %s is not supported!", OS));
        }
    }

    /**
     * installs the Script for any bash-based terminals
     * @param script the Script to be installed
     * @param installationConfig Absolute Path to the installation config File (e.g. "~/.bashrc")
     */
    private void UnixinstallScript(Path script, Path installationConfig) {
        if (this.install.permInstall()) {
            List<String> content = Arrays.asList( // check if file exists to not crash bashrc is sirius is deleted
                    "# SIRIUS autocompletion support",
                    "if [ -f \"" + script.toAbsolutePath() + "\" ]; then",
                    ". \"" + script.toAbsolutePath() + "\"" ,
                    "fi"
            );

            try {
                Files.write(installationConfig, content, StandardOpenOption.APPEND);
                System.out.println("Script installed. Pleases restart the terminal if the Autocompletion does not work");
            } catch (IOException e) {
                throw new RuntimeException("Unable to install CompletionScript", e);
            }

        }
        //run this also for permanent installation to make terminal restart obsolete
        AutoCompletionScript.executeBashCommand(". " + scriptFile.toAbsolutePath());
        //todo @lukas this does not work. It sources to the newly created bash an not to the one the user is currently using...
    }

    /**
     * installs the given Script on a typical Linux machine
     */
    private void installScriptLinux(Path script) {
        UnixinstallScript(script,  Path.of(System.getProperty("user.home")).resolve(".bashrc"));
    }

    /**
     * installs the given Script on a typical Windows machine (not supported!)
     */
    private void installScriptWindows(Path script) {
        //TODO Windows installation Script
        throw new RuntimeException("Autocompletion under Windows is not supported by default");
    }

    /**
     * installs the given Script in a macOS machine
     */
    private void installScriptMac(Path script) {
        UnixinstallScript(script, Path.of(System.getProperty("user.home")).resolve(".zshrc"));
    }

    /**
     * installs the given Script on a typical Solaris machine
     */
    private void installScriptSolaris(Path script) {
        // same as Linux
        UnixinstallScript(script,  Path.of(System.getProperty("user.home")).resolve(".bashrc"));
    }

    /**
     * detects the currently running SystemOS
     *
     * @return One of the following Strings: {"Windows", "Mac", "Linux", "Solaris"}
     * @throws UnknownOSException if the OS does not fall into the 4 categories of the output
     */
    public static @NotNull String detectOS() throws UnknownOSException {
        final String OSName = System.getProperty("os.name").toLowerCase();

        if (OSName.contains("win")) return "Windows";
        else if (OSName.contains("mac")) return "Mac";
        else if (OSName.contains("nux") || OSName.contains("nix") || OSName.contains("aix")) return "Linux";
        else if (OSName.contains("sunos")) return "Solaris";
        else throw new UnknownOSException("Could not detect OS");
    }

    /**
     * Edge cases that causes mismatches with the current used RegEx
     */
    private void addAliasesEdgeCases() {
        aliases.add("rerank"); // for rerank-formulas
        aliases.add("search"); // for search-structure-db
        aliases.add("compound"); // for compound-classes
    }

    /**
     * recursive function for the detection of different aliases on all Commandline depths
     * @param currentCommandline the Commandlineinstance for the different depths
     */
    private void findAliases(@NotNull CommandLine currentCommandline) {
        CommandLine.Model.CommandSpec subcommandsSpec = currentCommandline.getCommandSpec();
        if (subcommandsSpec.subcommands().isEmpty()) return;
        Map<String, CommandLine> commands = new HashMap<>(subcommandsSpec.subcommands());

        // add command aliases from this depth to Set
        aliases.addAll(Arrays.asList(subcommandsSpec.aliases()));

        // add subcommand aliases from this depth to Set
        commands.forEach((name, subcommand) -> aliases.addAll(Arrays.asList(subcommand.getCommandSpec().aliases())));


        // go through further depths
        subcommandsSpec.subcommands().forEach((name, command) -> findAliases(command));
    }

    /**
     * main function for reading the bash-autocompletion Script and calling different functions
     * for the different parts of the script
     * @return the modified Script
     * @throws IOException if the File is unreadable
     */
    private @NotNull String formatScript(Path scriptFile) throws IOException {
        this.progressbar = new ProgressbarDefaultVisualizer(System.out, new ProgressbarDefaultCalculator(5));
        this.progressbar.start();
        StringBuilder output = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(String.valueOf(scriptFile)));
        String line;
        HashSet<Integer> removed = new HashSet<>();
        String functionstatus = null;
        while ((line = reader.readLine()) != null) {
            String[] words = line.split(" ");

            // update functionstatus
            functionstatus = getFunctionstatus(line, functionstatus, words);


            // Check functionstatus and format line
            line = formatLine(line, functionstatus, words);

            if (line != null) output.append(line).append("\n");
        }
        progressbar.getCalculator().increaseProgress();
        return output.toString();
    }

    /**
     * Detects the current part of the autocompletion Script
     * @param line the line of the Script
     * @param functionstatus the last detected part of the Script
     * @param words the line split for all words
     * @return the current functionstatus
     */
    @Nullable
    private String getFunctionstatus(String line, String functionstatus, String[] words) {
        if (functionstatus == null && words.length > 1 && Objects.equals(words[0], "function") && words[1].equals("_complete_sirius()")) {
            functionstatus = "CompletionScriptFunction";
            progressbar.getCalculator().increaseProgress();
        } else if (Objects.equals(functionstatus, "CompletionScriptFunction") && line.equals("  # Find the longest sequence of subcommands and call the bash function for that subcommand.")) {
            functionstatus = "LocalCommandDef";
            progressbar.getCalculator().increaseProgress();
        } else if (Objects.equals(functionstatus, "LocalCommandDef") && words.length >= 3 && !Objects.equals(words[2], "local")) {
            functionstatus = "CompWords";
            progressbar.getCalculator().increaseProgress();
        } else if (Objects.equals(functionstatus, "CompWords") && line.equals("  # No subcommands were specified; generate completions for the top-level command.")) {
            functionstatus = "Subcommandfunction";
            validDeclaration = true;
            progressbar.getCalculator().increaseProgress();
        }
        return functionstatus;
    }

    /**
     * modifies the current line by calling the necessary function for the current functionstatus
     * @param line the currently read line
     * @param functionstatus the current functionstatus
     * @param words the line split for the needed words
     * @return the modified line (Null if line should be empty)
     */
    private String formatLine(String line, String functionstatus, String[] words) {
        if (functionstatus != null) {
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
                case "Subcommandfunction": {
                    line = formatSubcommandFunction(line, words);
                    break;
                }
            }
        }
        return line;
    }

    /**
     * Modifies the first part of the AutoCompletion Script
     * @param line the currently read line
     * @param words the currently read line split for the different words
     * @return the modified line
     */
    private String formatCompletionFunction(@NotNull String line, @NotNull String[] words) {
        if (Arrays.stream(words).anyMatch(word -> aliases.stream().anyMatch(alias -> alias.equals(word)))) line = null;
        if (Arrays.stream(words).anyMatch(word -> aliases.stream().anyMatch(alias -> (alias + "\"").equals(word))))
            line = null;
        return line;
    }

    /**
     * Modifies the second part of the AutoCompletion Script
     * @param line the currently read line
     * @param words the currently read line split for the different words
     * @return the modified line
     */
    private String formatCommandDefinitions(@NotNull String line, @NotNull String[] words) {
        if (words.length < 4) return line;
        if (!words[2].equals("local")) return line;

        Integer number = Integer.valueOf((words[3].split("=")[0].substring(4)));
        for (String word : words) {
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

    /**
     * Modifies the third part of the AutoCompletion Script
     * @param line the currently read line
     * @param words the currently read line split for the different words
     * @return the modified line
     */
    private String removeCompWords(@NotNull String line, @NotNull String[] words) {
        if (words.length < 5) return line;
        String valueHolder = words[4].split("@")[0];
        Integer val = Integer.valueOf(valueHolder.substring(7, valueHolder.length() - 1));
        if (removedDefinitions.contains(val)) line = null;
        return line;
    }

    /**
     * Definition of removed functionnames in fourth part of the AutocompletionScript.
     * @param alias the currently detected alias
     * @return if the function for the alias should be removed
     */
    private boolean isvalidsubalias(String alias) {
        return false;

        //TODO Ambiguous - non Deterministic?
        /*
        return (
                alias.equals("A") || alias.equals("PS") //|| alias.equals("C")
                  || alias.equals("EPR")  || alias.equals("F")
                  || alias.equals("tree") || alias.equals("MGF")|| alias.equals("compound") || alias.equals("search-structure-db")
                || alias.equals("P")|| alias.equals("sirius")
                || alias.equals("rerank")
    //                        || alias.equals("search")
      //                      || alias.equals("S")
            //    || alias.equals("T")
                        //    || alias.equals("W")
                        //   || alias.equals("Z")
                         ||  alias.equals("rerank-formulas")
  //              || alias.equals("compound-classes") || alias.equals("DB")
        );

         */
    }

    /**
     * Modifies the fourth part of the AutoCompletion Script
     * @param line the currently read line
     * @param words the currently read line split for the different words
     * @return the modified line
     */
    private String formatSubcommandFunction(String line, String[] words) {
        HashSet<String> subaliases = new HashSet(aliases);
        aliases.forEach(alias -> {
            if (!isvalidsubalias(alias)) subaliases.remove(alias);
        });
        String[] DECLARATIONINDICATOR = {"#", "Generates", "completions", "for", "the", "options", "and", "subcommands", "of", "the"};
        boolean declaration = words.length >= 10;
        if (words.length >= 10) {
            for (int i = 0; i < 9; i++) {
                if (!(words[i].equals(DECLARATIONINDICATOR[i]))) {
                    declaration = false;
                    break;
                }
            }
        }

        // Check if function is valid
        if (declaration && words.length >= 11) {
            String word = words[10].replaceAll("\\p{Punct}", "");
            validDeclaration = !aliases.contains(word);
            subvalidDeclaration = !subaliases.contains(word);
        }

        // Second check for function validity
        if (line.contains("function _picocli_sirius_")) {
            final String functionName = words[1].substring(13);
            if (aliases.stream().anyMatch(alias -> functionName.contains("_" + alias + "_"))) validDeclaration = false;
            if (subaliases.stream().anyMatch(alias -> functionName.contains("_" + alias + "_"))) subvalidDeclaration = false;
        }

        //Allow first declaration of sirius command
        if (firstsirius && declaration && !validDeclaration) {
            firstsirius = false;
            validDeclaration = true;
        }

        // remove invalid functions
        if(!validDeclaration && !subvalidDeclaration) line = null;

        // remove invalid subcommands from valid functions
        if (validDeclaration) {
            StringBuilder newline = new StringBuilder();
            for (String word : words) {
                if (!(aliases.contains(word.replaceAll("\"", "")))) newline.append(word).append(" ");
                else if (word.endsWith("\"")) newline.append("\"");
            }
            line = newline.toString();
        }
        return line;
    }


    /**
     * executes the given String as a Bash Command
     * @return true if execution was successful
     */
    public static boolean executeBashCommand(String command) {
        boolean success = false;
        Runtime r = Runtime.getRuntime();
        String[] commands = {"bash", "-c", command};
        try {
            Process p = r.exec(commands);
            p.waitFor();
            success = true;
        } catch (Exception e) {
            System.err.println("Failed to execute bash with command: " + command);
            e.printStackTrace();
        }
        return success;
    }

    /**
     * Exception for detection of an Unknown OS
     */
    public static class UnknownOSException extends RuntimeException {
        public UnknownOSException(String could_not_detect_os) {
            super(could_not_detect_os);
        }
    }
}

/**
 * class for determining the type of installation for the AutocompletionScript
 */
class Installationtype {
    @Option(names = {"--temporary", "-t"}, defaultValue = "false",
            description = "[Exclusive to -p] installs the Completionscript temporary")  private boolean temp;
    @Option(names = {"--permanent", "-p"}, defaultValue = "false",
            description = "[Exclusive to -t] installs the Completionscript permanently")  private boolean perm;

    /**
     * returns true if any installation is required
     */
    public boolean toInstall() {return (temp || perm);}

    /**
     * returns true if a permanent installation is required
     */
    public boolean permInstall() {return perm;}

    /**
     * Changes the installationtype to the given Parameter
     *
     * @param installationtype valid are: {null, temporary, permanent}
     * @return successfull change of the installationtype
     */
    public boolean setInstallationtype(@Nullable String installationtype) {
        if (installationtype == null) {
            this.temp = false;
            this.perm = false;
            return true;
        }
        if (installationtype.equals("temporary")) {
            this.temp = true;
            this.perm = false;
            return true;
        }
        if (installationtype.equals("permanent")) {
            this.temp = false;
            this.perm = true;
            return true;
        }
        return false;
    }
}