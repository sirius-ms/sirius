package de.unijena.bioinf.ms.frontend.utils;

import de.unijena.bioinf.fingerid.utils.FingerIDProperties;
import de.unijena.bioinf.ms.frontend.DefaultParameter;
import de.unijena.bioinf.ms.frontend.subtools.CLIRootOptions;
import de.unijena.bioinf.ms.frontend.subtools.canopus.CanopusOptions;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.frontend.subtools.custom_db.CustomDBOptions;
import de.unijena.bioinf.ms.frontend.subtools.decomp.DecompOptions;
import de.unijena.bioinf.ms.frontend.subtools.export.mgf.MgfExporterOptions;
import de.unijena.bioinf.ms.frontend.subtools.export.tables.ExportPredictionsOptions;
import de.unijena.bioinf.ms.frontend.subtools.export.trees.FTreeExporterOptions;
import de.unijena.bioinf.ms.frontend.subtools.fingerblast.FingerblastOptions;
import de.unijena.bioinf.ms.frontend.subtools.fingerprint.FingerprintOptions;
import de.unijena.bioinf.ms.frontend.subtools.lcms_align.LcmsAlignOptions;
import de.unijena.bioinf.ms.frontend.subtools.login.LoginOptions;
import de.unijena.bioinf.ms.frontend.subtools.passatutto.PassatuttoOptions;
import de.unijena.bioinf.ms.frontend.subtools.projectspace.ProjecSpaceOptions;
import de.unijena.bioinf.ms.frontend.subtools.similarity.SimilarityMatrixOptions;
import de.unijena.bioinf.ms.frontend.subtools.spectra_search.SpectraSearchOption;
import de.unijena.bioinf.ms.frontend.subtools.summaries.SummaryOptions;
import de.unijena.bioinf.ms.frontend.subtools.webservice.WebserviceOptions;
import de.unijena.bioinf.ms.frontend.subtools.zodiac.ZodiacOptions;
import de.unijena.bioinf.ms.frontend.workflow.SimpleInstanceBuffer;
import de.unijena.bioinf.ms.frontend.workflow.WorkflowBuilder;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.projectspace.ProjectSpaceManagerFactory;
import picocli.AutoComplete;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "sirius")
public class AutoCompletionScript implements Callable<Integer> {
    /**
     * Pass this CommandLine instance and the name of the script to the picocli.AutoComplete::bash method.
     * The method will return the source code of a completion script. Save the source code to a file and install it.
     * For the installation of the completion Script, please see the following: <a href="https://picocli.info/autocomplete.html#_install_completion_script">...</a>
     */
    public Integer call() throws IOException{
        //TODO cannot load ConfigOptions subcommand -> not enclosed class?
        //TODO maybe get Subcommandinstances from WorkflowBuilder
        //TODO generate completion Script during build: See https://picocli.info/autocomplete.html#_generating_completion_scripts_during_the_build

        DefaultParameterConfigLoader defaultparameter = new DefaultParameterConfigLoader();
        CommandLine hierarchy = new CommandLine(new CLIRootOptions<>(defaultparameter, new ProjectSpaceManagerFactory.Default()));
        //hierarchy.addSubcommand("config", new DefaultParameterConfigLoader.ConfigOptions());
        hierarchy.addSubcommand("canopus", new CanopusOptions(defaultparameter));
        hierarchy.addSubcommand("custom-db", new CustomDBOptions());
        hierarchy.addSubcommand("decomp", new DecompOptions());
        hierarchy.addSubcommand("mgf-export", new MgfExporterOptions());
        hierarchy.addSubcommand("prediction-export", new ExportPredictionsOptions());
        hierarchy.addSubcommand("ftree-export", new FTreeExporterOptions());
        hierarchy.addSubcommand("structure", new FingerblastOptions(defaultparameter));
        hierarchy.addSubcommand("fingerprint", new FingerprintOptions(defaultparameter));
        hierarchy.addSubcommand("lcms-align", new LcmsAlignOptions());
        hierarchy.addSubcommand("login", new LoginOptions());
        hierarchy.addSubcommand("passatutto", new PassatuttoOptions(defaultparameter));
        hierarchy.addSubcommand("project-space", new ProjecSpaceOptions()); // missing t in ProjectSpaceOptions
        hierarchy.addSubcommand("similarity", new SimilarityMatrixOptions());
        hierarchy.addSubcommand("spectra-search", new SpectraSearchOption());
        hierarchy.addSubcommand("write-summaries", new SummaryOptions());
        hierarchy.addSubcommand("webservice", new WebserviceOptions());
        hierarchy.addSubcommand("zodiac", new ZodiacOptions(defaultparameter));
        //hierarchy.addSubcommand("generate-autocompletion", new AutoCompletionScript());


        String script = AutoComplete.bash("sirius", hierarchy);
        Files.writeString(Path.of("/home/debian/sirius_completion3"), script);
        return 0;
    }

    public static void main(String... args) throws IOException {
        //AutoCompletionScript complete = new AutoCompletionScript();
        //complete.call();
        String NAME = "SiriusLinuxCompletionScript";
        Path PATH = Path.of(String.format("./sirius_cli/scripts/%s",NAME));
        System.setProperty("de.unijena.bioinf.ms.propertyLocations", "sirius_frontend.build.properties");
        FingerIDProperties.sirius_guiVersion();
        //int exitCode = new CommandLine(new AutoCompletionScript()).execute(args);
        final DefaultParameterConfigLoader configOptionLoader = new DefaultParameterConfigLoader();
        WorkflowBuilder<CLIRootOptions<ProjectSpaceManager>> builder = new WorkflowBuilder<>(new CLIRootOptions<>(configOptionLoader, new ProjectSpaceManagerFactory.Default()), configOptionLoader, new SimpleInstanceBuffer.Factory());
        builder.initRootSpec();
        CommandLine commandline = new CommandLine(builder.getRootSpec());
        commandline.setCaseInsensitiveEnumValuesAllowed(true);
        commandline.registerConverter(DefaultParameter.class, new DefaultParameter.Converter());
        int depth;
        if (args.length == 0) depth = 5;
        else depth = Integer.parseInt(args[0]);
        System.out.println(String.format("Creating AutocompletionScript of length %d", depth));
        setRecursionDepthLimit(commandline, depth);
        String s = AutoComplete.bash("sirius", commandline);
        System.out.println(String.format("AutocompletionScript created successfull at %s", PATH));
        Files.writeString(PATH, s);
        System.out.println(String.format("Please install the Script temporarly by typing the following into the Terminal: "+ (char)27 + "[1m. %s", NAME));

    }

    private static void setRecursionDepthLimit(CommandLine commandline, int remaining_depth) {
        CommandLine.Model.CommandSpec subcommandsSpec = commandline.getCommandSpec();
        if(subcommandsSpec.subcommands().isEmpty()) return;

        //TODO resolve concurrent modification Exception
        if(remaining_depth < 1) {
            Set<String> commands = subcommandsSpec.subcommands().keySet();
            while(!commands.isEmpty()){
                subcommandsSpec.removeSubcommand(commands.stream().iterator().next());
                commands = subcommandsSpec.subcommands().keySet();
                }
            }
        ////////////////////////////////////////////////////////////////////////////////
        else {
            subcommandsSpec.subcommands().forEach((name, command) -> setRecursionDepthLimit(command, remaining_depth - 1));
        }
    }
}
