package de.unijena.bioinf.ms.frontend.workflow;

import com.google.common.collect.Streams;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

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
 * This class is also intended to be used from the GUI but with a different {@RootOtion) class.
 * <p>
 * Buy using this class we do not need to write new Workflows every time we add a new tool.
 * We just have to define its parameters in h
 */

public class WorkflowBuilder<R extends RootOptions<?,?,?>> {

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
    public final SiriusOptions siriusOptions;
    public final ZodiacOptions zodiacOptions;
    public final FingerIdOptions fingeridOptions;
    public final CanopusOptions canopusOptions;
    public final PassatuttoOptions passatuttoOptions;



    public WorkflowBuilder(@NotNull R rootOptions) throws IOException {
        this(rootOptions,new DefaultParameterConfigLoader());
    }

    public WorkflowBuilder(@NotNull R rootOptions, @NotNull DefaultParameterConfigLoader configOptionLoader) throws IOException {
        this.rootOptions = rootOptions;

        this.configOptionLoader = configOptionLoader;

        siriusOptions = new SiriusOptions(configOptionLoader);
        zodiacOptions = new ZodiacOptions(configOptionLoader);
        fingeridOptions = new FingerIdOptions(configOptionLoader);
        canopusOptions = new CanopusOptions(configOptionLoader);
        passatuttoOptions = new PassatuttoOptions(configOptionLoader);

        customDBOptions =  new CustomDBOptions();
        projectSpaceOptions = new ProjecSpaceOptions();
        similarityMatrixOptions = new SimilarityMatrixOptions();
        decompOptions =  new DecompOptions();
    }

    public void initRootSpec() {
        System.setProperty("picocli.color.commands", "bold,blue");
        if (rootSpec != null)
            throw new  IllegalStateException("Root spec already initialized");

        // define execution order and dependencies of different Subtools
        CommandLine.Model.CommandSpec fingeridSpec = forAnnotatedObjectWithSubCommands(fingeridOptions, canopusOptions);
        CommandLine.Model.CommandSpec passatuttoSpec = forAnnotatedObjectWithSubCommands(passatuttoOptions, fingeridSpec);
        CommandLine.Model.CommandSpec zodiacSpec = forAnnotatedObjectWithSubCommands(zodiacOptions, passatuttoSpec, fingeridSpec);
        CommandLine.Model.CommandSpec siriusSpec = forAnnotatedObjectWithSubCommands(siriusOptions, zodiacSpec, passatuttoSpec, fingeridSpec);
        CommandLine.Model.CommandSpec lcmsAlignSpec = forAnnotatedObjectWithSubCommands(lcmsAlignOptions, siriusSpec);

        CommandLine.Model.CommandSpec configSpec = forAnnotatedObjectWithSubCommands(configOptionLoader.asCommandSpec(), customDBOptions, lcmsAlignSpec, siriusSpec, zodiacSpec, passatuttoSpec, fingeridSpec, canopusOptions);
        rootSpec = forAnnotatedObjectWithSubCommands(
                this.rootOptions,
                Streams.concat(Arrays.stream(standaloneTools()), Arrays.stream(new Object[]{configSpec, lcmsAlignSpec, siriusSpec, zodiacSpec, passatuttoSpec, fingeridSpec, canopusOptions})).toArray()
        );
    }

    protected Object[] standaloneTools() {
        return new Object[]{projectSpaceOptions, customDBOptions, similarityMatrixOptions, decompOptions};
    }

    protected CommandLine.Model.CommandSpec forAnnotatedObjectWithSubCommands(Object parent, Object... subsToolInExecutionOrder) {
        final CommandLine.Model.CommandSpec parentSpec = parent instanceof CommandLine.Model.CommandSpec
                ? (CommandLine.Model.CommandSpec) parent
                : CommandLine.Model.CommandSpec.forAnnotatedObject(parent);

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

            final ToolChainWorkflow wf = new ToolChainWorkflow(preproJob, postproJob, configOptionLoader.config, toolchain);
            return returnResultOrExit(wf);
        }

        private void execute(CommandLine parsed, List<Object> executionResult) {
            Object command = parsed.getCommand();
            if (command instanceof Runnable) {
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