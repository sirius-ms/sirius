package de.unijena.bioinf.ms.frontend.workflow;

import com.google.common.collect.Streams;
import de.unijena.bioinf.ms.frontend.DefaultParameter;
import de.unijena.bioinf.ms.frontend.subtools.*;
import de.unijena.bioinf.ms.frontend.subtools.canopus.CanopusOptions;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.frontend.subtools.custom_db.CustomDBOptions;
import de.unijena.bioinf.ms.frontend.subtools.decomp.DecompOptions;
import de.unijena.bioinf.ms.frontend.subtools.fingerid.FingerIdOptions;
import de.unijena.bioinf.ms.frontend.subtools.lcms_align.LcmsAlignOptions;
import de.unijena.bioinf.ms.frontend.subtools.passatutto.PassatuttoOptions;
import de.unijena.bioinf.ms.frontend.subtools.projectspace.ProjecSpaceOptions;
import de.unijena.bioinf.ms.frontend.subtools.similarity.SimilarityMatrixOptions;
import de.unijena.bioinf.ms.frontend.subtools.sirius.SiriusOptions;
import de.unijena.bioinf.ms.frontend.subtools.zodiac.ZodiacOptions;
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

// todo In general a lot of the configuration here could be done on compile time.
//  but on the other hand I do not think it is a performance critical thing.

public class WorkflowBuilder<R extends RootOptions<?,?,?>> {

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

    //preprocessing, project-space providing tool, preprojectspace tool
    public final LcmsAlignOptions lcmsAlignOptions = new LcmsAlignOptions();

    //toolchain subtools
    protected final Map<Class<? extends ToolChainOptions<?, ?>>, ToolChainOptions<?, ?>> toolChainTools;
    public WorkflowBuilder(@NotNull R rootOptions, @NotNull DefaultParameterConfigLoader configOptionLoader, InstanceBufferFactory<?> bufferFactory) throws IOException {
        this.bufferFactory = bufferFactory;
        this.rootOptions = rootOptions;
        this.configOptionLoader = configOptionLoader;

        toolChainTools = Map.of(
                SiriusOptions.class, new SiriusOptions(configOptionLoader),
                ZodiacOptions.class, new ZodiacOptions(configOptionLoader),
                PassatuttoOptions.class, new PassatuttoOptions(configOptionLoader),
                FingerIdOptions.class, new FingerIdOptions(configOptionLoader),
                CanopusOptions.class, new CanopusOptions(configOptionLoader)
        );

        customDBOptions = new CustomDBOptions();
        projectSpaceOptions = new ProjecSpaceOptions();
        similarityMatrixOptions = new SimilarityMatrixOptions();
        decompOptions = new DecompOptions();
    }

    public void initRootSpec() {
        System.setProperty("picocli.color.commands", "bold,blue");
        if (rootSpec != null)
            throw new IllegalStateException("Root spec already initialized");

        // define execution order and dependencies of different Subtools
        final Map<Class<? extends ToolChainOptions>, CommandLine.Model.CommandSpec> chainToolSpecs = configureChainTools();

        final CommandLine.Model.CommandSpec lcmsAlignSpec = forAnnotatedObjectWithSubCommands(lcmsAlignOptions, chainToolSpecs.get(SiriusOptions.class));

        final CommandLine.Model.CommandSpec configSpec = forAnnotatedObjectWithSubCommands(configOptionLoader.asCommandSpec(),
                Stream.concat(Stream.of(customDBOptions, lcmsAlignSpec), chainToolSpecs.values().stream()).toArray());

        rootSpec = forAnnotatedObjectWithSubCommands(
                this.rootOptions,
                Streams.concat(Stream.of(standaloneTools()), Stream.of(configSpec, lcmsAlignSpec), chainToolSpecs.values().stream())
                        .toArray()
        );
    }

    protected Object[] standaloneTools() {
        return new Object[]{projectSpaceOptions, customDBOptions, similarityMatrixOptions, decompOptions};
    }

    protected Map<Class<? extends ToolChainOptions>, CommandLine.Model.CommandSpec> configureChainTools() {
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


    private class ParseResultHandler extends CommandLine.AbstractParseResultHandler<Workflow> {
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
                if (parseResult.commandSpec().commandLine() instanceof PostprocessingTool) {
                    postproJob = ((PostprocessingTool<?>) parseResult.commandSpec().commandLine()).makePostprocessingJob(rootOptions, configOptionLoader.config);
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
                    // create a JobFactory (Task) an configures it invalidation behavior based on its
                    // possible SubTools.
                    ToolChainOptions<?, ?> toolChainOptions = (ToolChainOptions<?, ?>) command;
                    ToolChainJob.Factory<?> task = toolChainOptions.call();

                    final Set<Class<? extends ToolChainOptions<?, ?>>> reachable = new LinkedHashSet<>();
                    Set<Class<? extends ToolChainOptions<?, ?>>> tmp = Set.copyOf(toolChainOptions.getSubCommands());
                    while (tmp != null && !tmp.isEmpty()) {
                        reachable.addAll(tmp);
                        tmp = tmp.stream().map(toolChainTools::get).map(ToolChainOptions::getSubCommands)
                                .flatMap(Collection::stream).collect(Collectors.toSet());
                    }
                    reachable.stream().map(toolChainTools::get).forEach(sub -> task.addInvalidator(sub.getInvalidator()));

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
}