package de.unijena.bioinf.ms.frontend.workflow;

import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.frontend.subtools.RootOptionsCLI;
import de.unijena.bioinf.ms.frontend.subtools.SingeltonTool;
import de.unijena.bioinf.ms.frontend.subtools.canopus.CanopusOptions;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.frontend.subtools.custom_db.CustomDBOptions;
import de.unijena.bioinf.ms.frontend.subtools.fingerid.FingerIdOptions;
import de.unijena.bioinf.ms.frontend.subtools.sirius.SiriusOptions;
import de.unijena.bioinf.ms.frontend.subtools.zodiac.ZodiacOptions;
import de.unijena.bioinf.babelms.projectspace.SiriusProjectSpace;
import de.unijena.bioinf.sirius.ExperimentResult;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
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
public class WorkflowBuilder<R extends RootOptionsCLI> {

    //root
    public final CommandLine.Model.CommandSpec rootSpec;
    public final R rootOptions;

    //global configs (subtool)
    DefaultParameterConfigLoader configOptionLoader;

    //singelton tools
    public final CustomDBOptions customDBOptions = new CustomDBOptions();

    //toolchain subtools
    public final SiriusOptions siriusOptions = new SiriusOptions(configOptionLoader);
    public final ZodiacOptions zodiacOptions = new ZodiacOptions(configOptionLoader);
    public final FingerIdOptions fingeridOptions = new FingerIdOptions(configOptionLoader);
    public final CanopusOptions canopusOptions = new CanopusOptions(configOptionLoader);


    public WorkflowBuilder(@NotNull R rootOptions) throws IOException {
        this(rootOptions,new DefaultParameterConfigLoader());
    }

    public WorkflowBuilder(@NotNull R rootOptions, @NotNull DefaultParameterConfigLoader configOptionLoader) throws IOException {
        this.rootOptions = rootOptions;

        this.configOptionLoader = configOptionLoader;

        // define execution order and dependencies of different Subtools
        CommandLine.Model.CommandSpec fingeridSpec = forAnnotatedObjectWithSubCommands(fingeridOptions, canopusOptions);
        CommandLine.Model.CommandSpec zodiacSpec = forAnnotatedObjectWithSubCommands(zodiacOptions, fingeridSpec);
        CommandLine.Model.CommandSpec siriusSpec = forAnnotatedObjectWithSubCommands(siriusOptions, zodiacSpec, fingeridSpec);

        CommandLine.Model.CommandSpec configSpec = forAnnotatedObjectWithSubCommands(configOptionLoader.asCommandSpec(), customDBOptions, siriusSpec, zodiacSpec, fingeridSpec, canopusOptions);
        rootSpec = forAnnotatedObjectWithSubCommands(this.rootOptions, customDBOptions, configSpec, siriusSpec, zodiacSpec, fingeridSpec, canopusOptions);
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


            //get project space from root cli
            final SiriusProjectSpace space = ((RootOptions) parseResult.commandSpec().commandLine().getCommand()).getProjectSpace();
            final Iterator<ExperimentResult> input = ((RootOptions) parseResult.commandSpec().commandLine().getCommand()).newInputExperimentIterator();

            List<Object> toolchain = new ArrayList<>();
            // look for an alternative input in the first subtool that is not the CONFIG subtool.
            if (parseResult.hasSubcommand()) {
                parseResult = parseResult.subcommand();
                if (parseResult.commandSpec().commandLine().getCommand() instanceof DefaultParameterConfigLoader.ConfigOptions)
                    parseResult = parseResult.subcommand();
                if (parseResult.commandSpec().commandLine().getCommand() instanceof SingeltonTool)
                    return ((SingeltonTool) parseResult.commandSpec().commandLine().getCommand()).getSingeltonWorkflow();

                execute(parseResult.commandSpec().commandLine(), toolchain);
            } else {
                return () -> LoggerFactory.getLogger(getClass()).warn("No execution steps have been Specified!");

            }

            while (parseResult.hasSubcommand()) {
                parseResult = parseResult.subcommand();
                execute(parseResult.commandSpec().commandLine(), toolchain);
            }

            final ToolChainWorkflow wf = new ToolChainWorkflow(space, input, configOptionLoader.config, toolchain);
            wf.setInstanceBuffer(rootOptions.getInitialInstanceBuffer(), rootOptions.getMaxInstanceBuffer());

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