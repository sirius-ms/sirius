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

import com.google.common.collect.Streams;
import de.unijena.bioinf.ms.frontend.DefaultParameter;
import de.unijena.bioinf.ms.frontend.subtools.*;
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
import de.unijena.bioinf.ms.frontend.subtools.settings.SettingsOptions;
import de.unijena.bioinf.ms.frontend.subtools.similarity.SimilarityMatrixOptions;
import de.unijena.bioinf.ms.frontend.subtools.sirius.SiriusOptions;
import de.unijena.bioinf.ms.frontend.subtools.summaries.SummaryOptions;
import de.unijena.bioinf.ms.frontend.subtools.zodiac.ZodiacOptions;
import de.unijena.bioinf.ms.frontend.utils.AutoCompletionScript;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
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
 * This class is also intended to be used from the GUI but with a different {@link RootOptions) class.
 * <p>
 * Buy using this class we do not need to write new Workflows every time we add a new tool.
 * We just have to define its parameters in h
 */


/*
 * NOTE: In general a lot of the configuration here could be done on compile time.
 * On the other hand I do not think it is performance critical.
 */

public class WorkflowBuilder<R extends RootOptions<?, ?, ?, ?>> {

    private final InstanceBufferFactory<?> bufferFactory;
    //root
    private CommandLine.Model.CommandSpec rootSpec;

    public CommandLine.Model.CommandSpec getRootSpec() {
        return rootSpec;
    }

    public final R rootOptions;

    //global configs (subtool)
    DefaultParameterConfigLoader configOptionLoader;

    //standalone tools
    public final CustomDBOptions customDBOptions;
    public final ProjecSpaceOptions projectSpaceOptions; // this is also singleton
    public final SimilarityMatrixOptions similarityMatrixOptions;
    public final DecompOptions decompOptions;
    public final LoginOptions loginOptions;
    public final SettingsOptions settingsOptions;

    //postprocessing, project-space consuming tool, exporting tools,
    public final SummaryOptions summaryOptions;
    public final ExportPredictionsOptions exportPredictions;
    public final MgfExporterOptions mgfExporterOptions;
    public final FTreeExporterOptions ftreeExporterOptions;
    public final AutoCompletionScript autocompleteOptions;

    //preprocessing, project-space providing tool, pre-project-space tool
    public final LcmsAlignOptions lcmsAlignOptions = new LcmsAlignOptions();

    //toolchain subtools
    protected final Map<Class<? extends ToolChainOptions<?, ?>>, ToolChainOptions<?, ?>> toolChainTools;

    protected final @NotNull List<StandaloneTool<?>> additionalTools;

    public WorkflowBuilder(@NotNull R rootOptions, @NotNull DefaultParameterConfigLoader configOptionLoader, InstanceBufferFactory<?> bufferFactory) throws IOException {
        this(rootOptions, configOptionLoader, bufferFactory, List.of());
    }

    public WorkflowBuilder(@NotNull R rootOptions, @NotNull DefaultParameterConfigLoader configOptionLoader, InstanceBufferFactory<?> bufferFactory, @NotNull List<StandaloneTool<?>> additionalTools) throws IOException {
        this.bufferFactory = bufferFactory;
        this.rootOptions = rootOptions;
        this.configOptionLoader = configOptionLoader;
        this.additionalTools = additionalTools;

        toolChainTools = Map.of(
                SiriusOptions.class, new SiriusOptions(configOptionLoader),
                ZodiacOptions.class, new ZodiacOptions(configOptionLoader),
                PassatuttoOptions.class, new PassatuttoOptions(configOptionLoader),
                FingerprintOptions.class, new FingerprintOptions(configOptionLoader),
                FingerblastOptions.class, new FingerblastOptions(configOptionLoader),
                CanopusOptions.class, new CanopusOptions(configOptionLoader)
        );

        customDBOptions = new CustomDBOptions();
        projectSpaceOptions = new ProjecSpaceOptions();
        similarityMatrixOptions = new SimilarityMatrixOptions();
        decompOptions = new DecompOptions();
        mgfExporterOptions = new MgfExporterOptions();
        ftreeExporterOptions = new FTreeExporterOptions();
        summaryOptions = new SummaryOptions();
        exportPredictions = new ExportPredictionsOptions();
        loginOptions = new LoginOptions();
        settingsOptions = new SettingsOptions();
        autocompleteOptions = new AutoCompletionScript();
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
        return Streams.concat(
                Stream.of(projectSpaceOptions, customDBOptions, similarityMatrixOptions, decompOptions, mgfExporterOptions, ftreeExporterOptions, exportPredictions),
                additionalTools.stream(), Stream.of(loginOptions, settingsOptions, autocompleteOptions)
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

    public ParseResultHandler makeParseResultHandler() {
        return new ParseResultHandler();
    }


    public class ParseResultHandler extends CommandLine.AbstractParseResultHandler<Workflow> {
        @Override
        protected Workflow handle(CommandLine.ParseResult parseResult) throws CommandLine.ExecutionException {
            //here we create the workflow that we will execute later
            if (!(parseResult.commandSpec().commandLine().getCommand() == rootOptions))
                throw new CommandLine.ExecutionException(parseResult.commandSpec().commandLine(), "Illegal root CLI found!");


            // init special preprocessing and config jobs
            PreprocessingJob<?> preproJob = null;

            List<Object> toolchain = new ArrayList<>();
            // look for an alternative input in the first subtool that is not the CONFIG subtool.
            if (parseResult.hasSubcommand()) {
                parseResult = parseResult.subcommand();
                if (parseResult.commandSpec().commandLine().getCommand() instanceof DefaultParameterConfigLoader.ConfigOptions)
                    parseResult = parseResult.subcommand();
                if (parseResult.commandSpec().commandLine().getCommand() instanceof StandaloneTool)
                    return ((StandaloneTool<?>) parseResult.commandSpec().commandLine().getCommand()).makeWorkflow(rootOptions, configOptionLoader.config);
                if (parseResult.commandSpec().commandLine().getCommand() instanceof PreprocessingTool)
                    preproJob = ((PreprocessingTool<?>) parseResult.commandSpec().commandLine().getCommand()).makePreprocessingJob(rootOptions, configOptionLoader.config);
                else
                    execute(parseResult.commandSpec().commandLine(), toolchain);
            } else {
                return () -> LoggerFactory.getLogger(getClass()).warn("No execution steps have been Specified!");
            }

            // handle toolchain and posprocessing jobs
            PostprocessingJob<?> postproJob = null;

            while (parseResult.hasSubcommand()) {
                parseResult = parseResult.subcommand();
                if (parseResult.commandSpec().commandLine().getCommand() instanceof PostprocessingTool) {
                    postproJob = ((PostprocessingTool<?>) parseResult.commandSpec().commandLine().getCommand()).makePostprocessingJob(rootOptions, configOptionLoader.config);
                    break;
                } else {
                    execute(parseResult.commandSpec().commandLine(), toolchain);
                }
            }

            if (preproJob == null)
                preproJob = rootOptions.makeDefaultPreprocessingJob();
            if (postproJob == null)
                postproJob = rootOptions.makeDefaultPostprocessingJob();

            final ToolChainWorkflow wf = new ToolChainWorkflow(preproJob, postproJob, configOptionLoader.config, toolchain, bufferFactory);
            return returnResultOrExit(wf);
        }

        private void execute(CommandLine parsed, List<Object> executionResult) {
            Object command = parsed.getCommand();
            if (command instanceof ToolChainOptions) {
                try {
                    // create a JobFactory (Task) and configures it invalidation behavior based on its subtools.
                    final ToolChainOptions<?, ?> toolChainOptions = (ToolChainOptions<?, ?>) command;
                    final ToolChainJob.Factory<?> task = toolChainOptions.call();

                    // detect tool dependencies and configure invalidators
                    configureInvalidator(toolChainOptions, task);

                    executionResult.add(task);
                } catch (CommandLine.ParameterException | CommandLine.ExecutionException ex) {
                    throw ex;
                } catch (Exception ex) {
                    throw new CommandLine.ExecutionException(parsed, "Error while calling command (" + command + "): " + ex, ex);
                }
            } else if (command instanceof Runnable) {
                try {
                    ((Runnable) command).run();
                } catch (CommandLine.ParameterException | CommandLine.ExecutionException ex) {
                    throw ex;
                } catch (Exception ex) {
                    throw new CommandLine.ExecutionException(parsed, "Error while running command (" + command + "): " + ex, ex);
                }
            } else if (command instanceof Callable) {
                try {
                    @SuppressWarnings("unchecked") Callable<Object> callable = (Callable<Object>) command;
                    executionResult.add(callable.call());
                } catch (CommandLine.ParameterException | CommandLine.ExecutionException ex) {
                    throw ex;
                } catch (Exception ex) {
                    throw new CommandLine.ExecutionException(parsed, "Error while calling command (" + command + "): " + ex, ex);
                }
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
}