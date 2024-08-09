/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.frontend.workflow;

import de.unijena.bioinf.ms.frontend.DefaultParameter;
import de.unijena.bioinf.ms.frontend.subtools.*;
import de.unijena.bioinf.ms.frontend.subtools.canopus.CanopusOptions;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.frontend.subtools.custom_db.CustomDBOptions;
import de.unijena.bioinf.ms.frontend.subtools.decomp.DecompOptions;
import de.unijena.bioinf.ms.frontend.subtools.export.mgf.MgfExporterOptions;
import de.unijena.bioinf.ms.frontend.subtools.fingerblast.FingerblastOptions;
import de.unijena.bioinf.ms.frontend.subtools.fingerprint.FingerprintOptions;
import de.unijena.bioinf.ms.frontend.subtools.fingerprinter.FingerprinterOptions;
import de.unijena.bioinf.ms.frontend.subtools.lcms_align.LcmsAlignOptions;
import de.unijena.bioinf.ms.frontend.subtools.login.LoginOptions;
import de.unijena.bioinf.ms.frontend.subtools.msnovelist.MsNovelistOptions;
import de.unijena.bioinf.ms.frontend.subtools.settings.SettingsOptions;
import de.unijena.bioinf.ms.frontend.subtools.similarity.SimilarityMatrixOptions;
import de.unijena.bioinf.ms.frontend.subtools.sirius.SiriusOptions;
import de.unijena.bioinf.ms.frontend.subtools.spectra_search.SpectraSearchOptions;
import de.unijena.bioinf.ms.frontend.subtools.summaries.SummaryOptions;
import de.unijena.bioinf.ms.frontend.subtools.zodiac.ZodiacOptions;
import de.unijena.bioinf.ms.frontend.utils.AutoCompletionScript;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.projectspace.ProjectSpaceManagerFactory;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is used to create a toolchain workflow to be executed
 * in a CLI run Based on the given Parameters given by the User.
 * <p>
 * All possible SubToolOption  of the SIRIUS CLi need to be added to this class
 * to be part of an automated execution.
 * <p>
 * In the Constructor it needs to be defined how the different Subtools depend on each other and
 * in which order they have to be executed.
 * <p>
 * This class is also intended to be used from the GUI but with a different {@link RootOptions} class.
 * <p>
 * Buy using this class we do not need to write new Workflows every time we add a new tool.
 * We just have to define its parameters in h
 */


/*
 * NOTE: In general a lot of the configuration here could be done on compile time.
 * On the other hand I do not think it is performance critical.
 */

public class WorkflowBuilder {
    //root
    private CommandLine.Model.CommandSpec rootSpec;

    public CommandLine.Model.CommandSpec getRootSpec() {
        return rootSpec;
    }

    public final RootOptions<?> rootOptions;

    //global configs (subtool)
    DefaultParameterConfigLoader configOptionLoader;

    //standalone tools
    public final CustomDBOptions customDBOptions;

    public final SimilarityMatrixOptions similarityMatrixOptions;
    public final DecompOptions decompOptions;
    public final LoginOptions loginOptions;
    public final SettingsOptions settingsOptions;

    public final FingerprinterOptions fingerprinterOptions;

    //postprocessing, project-space consuming tool, exporting tools,
    public final SummaryOptions summaryOptions;
    //    public final ExportPredictionsOptions exportPredictions;
    public final MgfExporterOptions mgfExporterOptions;
    //    public final UpdateFingerprintOptions updateFingerprintOptions;
    public final AutoCompletionScript autocompleteOptions;

    //preprocessing, project-space providing tool, pre-project-space tool
    public final LcmsAlignOptions lcmsAlignOptions = new LcmsAlignOptions();

    //toolchain subtools
    protected final Map<Class<? extends ToolChainOptions<?, ?>>, ToolChainOptions<?, ?>> toolChainTools;

    protected final @NotNull List<StandaloneTool<?>> additionalTools;

    protected final ProjectSpaceManagerFactory<? extends ProjectSpaceManager> spaceManagerFactory;

    boolean closeProject = true;

    public WorkflowBuilder(@NotNull CLIRootOptions rootOptions) {
        this(rootOptions, rootOptions.getDefaultConfigOptions(), rootOptions.getSpaceManagerFactory());
    }

    public WorkflowBuilder(@NotNull RootOptions<?> rootOptions, @NotNull DefaultParameterConfigLoader configOptionLoader, ProjectSpaceManagerFactory<? extends ProjectSpaceManager> spaceManagerFactory, boolean closeProject) {
        this(rootOptions, configOptionLoader, spaceManagerFactory);
        this.closeProject = closeProject;
    }

    public WorkflowBuilder(@NotNull RootOptions<?> rootOptions, @NotNull DefaultParameterConfigLoader configOptionLoader, ProjectSpaceManagerFactory<? extends ProjectSpaceManager> spaceManagerFactory) {
        this(rootOptions, configOptionLoader, spaceManagerFactory, List.of());
    }

    public WorkflowBuilder(@NotNull CLIRootOptions rootOptions, @NotNull List<StandaloneTool<?>> additionalTools) {
        this(rootOptions, rootOptions.getDefaultConfigOptions(), rootOptions.getSpaceManagerFactory(), additionalTools);
    }

    public WorkflowBuilder(@NotNull RootOptions<?> rootOptions, @NotNull DefaultParameterConfigLoader configOptionLoader, ProjectSpaceManagerFactory<? extends ProjectSpaceManager> spaceManagerFactory, @NotNull List<StandaloneTool<?>> additionalTools) {
        this.rootOptions = rootOptions;
        this.spaceManagerFactory = spaceManagerFactory;
        this.configOptionLoader = configOptionLoader;
        this.additionalTools = additionalTools;

        toolChainTools = Map.of(
                SpectraSearchOptions.class, new SpectraSearchOptions(configOptionLoader),
                SiriusOptions.class, new SiriusOptions(configOptionLoader),
                ZodiacOptions.class, new ZodiacOptions(configOptionLoader),
//                PassatuttoOptions.class, new PassatuttoOptions(configOptionLoader),
                FingerprintOptions.class, new FingerprintOptions(configOptionLoader),
                FingerblastOptions.class, new FingerblastOptions(configOptionLoader),
                CanopusOptions.class, new CanopusOptions(configOptionLoader),
                MsNovelistOptions.class, new MsNovelistOptions(configOptionLoader)
        );

        customDBOptions = new CustomDBOptions();
        similarityMatrixOptions = new SimilarityMatrixOptions(spaceManagerFactory);
        decompOptions = new DecompOptions();
        mgfExporterOptions = new MgfExporterOptions();
        summaryOptions = new SummaryOptions();
//        exportPredictions = new ExportPredictionsOptions();
        loginOptions = new LoginOptions();
        settingsOptions = new SettingsOptions();
        autocompleteOptions = new AutoCompletionScript();
        fingerprinterOptions = new FingerprinterOptions();
//        updateFingerprintOptions = new UpdateFingerprintOptions();
    }

    public void initRootSpec() {
        System.setProperty("picocli.color.commands", "bold,blue");
        if (rootSpec != null)
            throw new IllegalStateException("Root spec already initialized");

        final CommandLine.Model.CommandSpec summarySpec = forAnnotatedObjectWithSubCommands(summaryOptions);

        // define execution order and dependencies of different Subtools
        final Map<Class<? extends ToolChainOptions>, CommandLine.Model.CommandSpec> chainToolSpecs = configureChainTools(summarySpec);

        final CommandLine.Model.CommandSpec lcmsAlignSpec = forAnnotatedObjectWithSubCommands(lcmsAlignOptions, chainToolSpecs.get(SiriusOptions.class));

        Object[] standaloneTools = standaloneTools();

        final CommandLine.Model.CommandSpec configSpec = forAnnotatedObjectWithSubCommands(configOptionLoader.asCommandSpec(),
                Stream.concat(Stream.concat(Stream.of(lcmsAlignSpec), chainToolSpecs.values().stream()), Stream.concat(Arrays.stream(standaloneTools), Stream.of(summarySpec))).toArray());

        rootSpec = forAnnotatedObjectWithSubCommands(this.rootOptions,
                Stream.concat(Stream.concat(Stream.concat(Stream.of(configSpec), Stream.of(standaloneTools())), Stream.of(summarySpec, lcmsAlignSpec)), chainToolSpecs.values().stream()).toArray()
        );
    }

    protected Object[] standaloneTools() {
        return Stream.concat(
                Stream.concat(
                        Stream.of(customDBOptions, similarityMatrixOptions, decompOptions, mgfExporterOptions, /*exportPredictions,*/ fingerprinterOptions/*, updateFingerprintOptions*/),
                        additionalTools.stream()
                ), Stream.of(loginOptions, settingsOptions, autocompleteOptions)
        ).toArray(Object[]::new);

    }

    protected Map<Class<? extends ToolChainOptions>, CommandLine.Model.CommandSpec> configureChainTools(CommandLine.Model.CommandSpec... postProcessors) {
        final Map<Class<? extends ToolChainOptions>, CommandLine.Model.CommandSpec> specs = new LinkedHashMap<>();
        //inti command specs
        toolChainTools.values().forEach(t -> {
            final CommandLine.Model.CommandSpec parentSpec = CommandLine.Model.CommandSpec.forAnnotatedObject(t);
            new ArrayList<>(parentSpec.options()).stream().filter(it -> DefaultParameter.class.isAssignableFrom(it.type())).forEach(opt -> {
                parentSpec.remove(opt);
                String[] desc = Stream.concat(Stream.of(opt.description()), Stream.of("Default: " + configOptionLoader.config.getConfigValue(opt.descriptionKey()))).toArray(String[]::new);
                parentSpec.addOption(opt.toBuilder().description(desc).descriptionKey("").build());
            });
            specs.put(t.getClass(), parentSpec);
        });

        //add possible subtools
        toolChainTools.values().forEach(parent -> {
            CommandLine.Model.CommandSpec parentSpec = specs.get(parent.getClass());
            parent.getSubCommands().stream().map(specs::get).forEach(subSpec -> parentSpec.addSubcommand(subSpec.name(), subSpec));
        });

        //add possible postprocessors
        toolChainTools.values().forEach(parent -> {
            CommandLine.Model.CommandSpec parentSpec = specs.get(parent.getClass());
            for (CommandLine.Model.CommandSpec postCommandSpec : postProcessors)
                parentSpec.addSubcommand(postCommandSpec.name(), postCommandSpec);
        });

        return specs;
    }

    protected CommandLine.Model.CommandSpec forAnnotatedObjectWithSubCommands(Object parent, Object... subsToolInExecutionOrder) {
        final CommandLine.Model.CommandSpec parentSpec;
        if (parent instanceof CommandLine.Model.CommandSpec) {
            parentSpec = (CommandLine.Model.CommandSpec) parent;
        } else {
            parentSpec = CommandLine.Model.CommandSpec.forAnnotatedObject(parent);
            new ArrayList<>(parentSpec.options()).stream().filter(it -> DefaultParameter.class.isAssignableFrom(it.type())).forEach(opt -> {
                parentSpec.remove(opt);
                String[] desc = Stream.concat(Stream.of(opt.description()), Stream.of("Default: " + configOptionLoader.config.getConfigValue(opt.descriptionKey()))).toArray(String[]::new);
                parentSpec.addOption(opt.toBuilder().description(desc).descriptionKey("").build());
            });
        }

        for (Object sub : subsToolInExecutionOrder) {
            final CommandLine.Model.CommandSpec subSpec = sub instanceof CommandLine.Model.CommandSpec
                    ? (CommandLine.Model.CommandSpec) sub
                    : CommandLine.Model.CommandSpec.forAnnotatedObject(sub);
            parentSpec.addSubcommand(subSpec.name(), subSpec);
        }
        return parentSpec;
    }

    public ParseResultHandler makeParseResultHandler(@NotNull InstanceBufferFactory<?> bufferFactory) {
        return new ParseResultHandler(bufferFactory);
    }


    public class ParseResultHandler extends CommandLine.AbstractParseResultHandler<Workflow> {
        private final InstanceBufferFactory<?> bufferFactory;

        public ParseResultHandler(InstanceBufferFactory<?> bufferFactory) {
            this.bufferFactory = bufferFactory;
        }

        @Override
        protected Workflow handle(CommandLine.ParseResult parseResult) throws CommandLine.ExecutionException {
            //here we create the workflow that we will execute later
            if (!(parseResult.commandSpec().commandLine().getCommand() == rootOptions))
                throw new CommandLine.ExecutionException(parseResult.commandSpec().commandLine(), "Illegal root Options object found!");


            // init special preprocessing and config jobs
            PreprocessingJob<?> preproJob = null;

            List<ToolChainJob.Factory<?>> toolchain = new ArrayList<>();
            List<ToolChainOptions<?, ?>> toolchainOptions = new ArrayList<>();

            // look for an alternative input in the first subtool that is not the CONFIG subtool.
            if (parseResult.hasSubcommand()) {
                parseResult = parseResult.subcommand();
                if (parseResult.commandSpec().commandLine().getCommand() instanceof DefaultParameterConfigLoader.ConfigOptions)
                    parseResult = parseResult.subcommand();
                if (parseResult.commandSpec().commandLine().getCommand() instanceof StandaloneTool)
                    return ((StandaloneTool<?>) parseResult.commandSpec().commandLine().getCommand())
                            .makeWorkflow(rootOptions, configOptionLoader.config);
                if (parseResult.commandSpec().commandLine().getCommand() instanceof PreprocessingTool) {
                    if (spaceManagerFactory == null)
                        throw new IllegalStateException("Preprocessing tool requires a ProjectSpaceManagerFactory!");
                    preproJob = ((PreprocessingTool<?>) parseResult.commandSpec().commandLine().getCommand())
                            .makePreprocessingJob(rootOptions.getInput(), rootOptions.getOutput(), spaceManagerFactory, configOptionLoader.config);
                } else {
                    execute(parseResult.commandSpec().commandLine(), toolchain, toolchainOptions);
                }
            } else {
                return () -> LoggerFactory.getLogger(getClass()).warn("No execution steps have been Specified!");
            }

            // handle toolchain and posprocessing jobs
            PostprocessingJob<?> postproJob = null;

            while (parseResult.hasSubcommand()) {
                parseResult = parseResult.subcommand();
                if (parseResult.commandSpec().commandLine().getCommand() instanceof PostprocessingTool) {
                    postproJob = ((PostprocessingTool<?>) parseResult.commandSpec().commandLine().getCommand())
                            .makePostprocessingJob();
                    break;
                } else {
                    execute(parseResult.commandSpec().commandLine(), toolchain, toolchainOptions);
                }
            }

            if (preproJob == null)
                preproJob = rootOptions.makeDefaultPreprocessingJob();
            if (closeProject && postproJob == null)
                postproJob = new ClosingProjectPostprocessor();

            //find dependent jobs
            assignEarliestInputProvider(toolchain, toolchainOptions);

            final ToolChainWorkflow wf = new ToolChainWorkflow(preproJob, postproJob, configOptionLoader.config, toolchain, bufferFactory);
            return returnResultOrExit(wf);
        }

        //searches for the jobtype another job really depends so that it can be assigned as dependency instead of
        // "just" the previous job in the command.
        private void assignEarliestInputProvider(List<ToolChainJob.Factory<?>> toolchain, List<ToolChainOptions<?, ?>> toolchainOptions) {
            for (int i = 0; i < toolchain.size(); i++) {
                ToolChainJob.Factory<?> factory = toolchain.get(i);
                ToolChainOptions<?, ?> options = toolchainOptions.get(i);
                for (int j = i - 1; j >= 0; j--) {
                    ToolChainOptions<?, ?> probableInputProvider = toolchainOptions.get(j);
                    ToolChainJob.Factory<?> probableInputProviderFactory = toolchain.get(j);

                    if (probableInputProvider.getDependentSubCommands().contains(options.getClass()))
                        factory.setInputProvidingFactory(probableInputProviderFactory);
                }

            }
        }

        private void execute(CommandLine parsed, List<ToolChainJob.Factory<?>> executionResult, List<ToolChainOptions<?, ?>> executionOptions) {
            Object command = parsed.getCommand();
            if (command instanceof ToolChainOptions) {
                try {
                    // create a JobFactory (Task) and configures it invalidation behavior based on its subtools.
                    final ToolChainOptions<?, ?> toolChainOptions = (ToolChainOptions<?, ?>) command;
                    final ToolChainJob.Factory<?> task = toolChainOptions.call();

                    // detect tool dependencies and configure invalidators
                    configureInvalidator(toolChainOptions, task);

                    executionResult.add(task);
                    executionOptions.add(toolChainOptions);
                } catch (CommandLine.ParameterException | CommandLine.ExecutionException ex) {
                    throw ex;
                } catch (Exception ex) {
                    throw new CommandLine.ExecutionException(parsed, "Error while calling command (" + command + "): " + ex, ex);
                }
            } else {
                throw new IllegalArgumentException("Found " + command.getClass().getName() + " but Expected " + ToolChainOptions.class.getName());
            }
        }

        @Override
        protected ParseResultHandler self() {
            return this;
        }
    }

    private void configureInvalidator(ToolChainOptions<?, ?> toolChainOptions, ToolChainJob.Factory<?> task) {
        final Set<Class<? extends ToolChainOptions<?, ?>>> reachable = new LinkedHashSet<>();
        Set<Class<? extends ToolChainOptions<?, ?>>> tmp = Set.copyOf(toolChainOptions.getDependentSubCommands());
        while (tmp != null && !tmp.isEmpty()) {
            reachable.addAll(tmp);
            tmp = tmp.stream().map(toolChainTools::get).map(ToolChainOptions::getDependentSubCommands)
                    .flatMap(Collection::stream).collect(Collectors.toSet());
        }
        reachable.stream().map(toolChainTools::get).forEach(sub -> task.addInvalidator(sub.getInvalidator()));
    }

    private static class ClosingProjectPostprocessor extends PostprocessingJob<Void> {

        private Iterable<? extends Instance> instances;

        @Override
        public void setInput(Iterable<? extends Instance> instances, ParameterConfig config) {
            this.instances = instances;
        }

        @Override
        protected Void compute() {
            Set<ProjectSpaceManager> managersToClose = new HashSet<>();
            instances.forEach(i -> managersToClose.add(i.getProjectSpaceManager()));
            instances = null;

            managersToClose.forEach(ps -> {
                try {
                    ps.close();
                } catch (IOException e) {
                    LoggerFactory.getLogger(getClass()).warn("Error when closing Project after workflow!", e);
                }
            });
            return null;
        }

        @Override
        protected void cleanup() {
            instances = null;
            super.cleanup();
        }
    }
}