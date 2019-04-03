package de.unijena.bioinf.ms.cli.workflow;

import de.unijena.bioinf.ms.cli.parameters.InputProvider;
import de.unijena.bioinf.ms.cli.parameters.RootOptions;
import de.unijena.bioinf.ms.cli.parameters.RootOptionsCLI;
import de.unijena.bioinf.ms.cli.parameters.SingeltonTool;
import de.unijena.bioinf.ms.cli.parameters.canopus.CanopusOptions;
import de.unijena.bioinf.ms.cli.parameters.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.cli.parameters.custom_db.CustomDBOptions;
import de.unijena.bioinf.ms.cli.parameters.fingerid.FingerIdOptions;
import de.unijena.bioinf.ms.cli.parameters.sirius.SiriusOptions;
import de.unijena.bioinf.ms.cli.parameters.zodiac.ZodiacOptions;
import de.unijena.bioinf.ms.io.projectspace.SiriusProjectSpace;
import de.unijena.bioinf.sirius.ExperimentResult;
import de.unijena.bioinf.sirius.core.ApplicationCore;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

public class WorkflowBuilder<R extends RootOptionsCLI> {

    //root
    public final CommandLine.Model.CommandSpec rootSpec;
    public final R rootOptions;

    //global configs (subtool)
    DefaultParameterConfigLoader configOptionLoader = new DefaultParameterConfigLoader();

    //singelton tools
    public final CustomDBOptions customDBOptions = new CustomDBOptions();

    //toolchain subtools
    public final SiriusOptions siriusOptions = new SiriusOptions(configOptionLoader);
    public final ZodiacOptions zodiacOptions = new ZodiacOptions(configOptionLoader);
    public final FingerIdOptions fingeridOptions = new FingerIdOptions(configOptionLoader);
    public final CanopusOptions canopusOptions = new CanopusOptions(configOptionLoader);


    public WorkflowBuilder(@NotNull R rootOptions) throws IOException {
        this.rootOptions = rootOptions;

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
            final SiriusProjectSpace space;

            try {
                space = ((RootOptions) parseResult.commandSpec().commandLine().getCommand()).getProjectSpace();
            } catch (IOException e) {
                throw new CommandLine.ExecutionException(parseResult.commandSpec().commandLine(), "Could not Instantiate Sirius Project Space", e);
            }

            Iterator<ExperimentResult> input = space.parseExperimentIterator();
            List<Object> toolchain = new ArrayList<>();
            // look for an alternative input in the first subtool, that is not the CONFIG tool.
            if (parseResult.hasSubcommand()) {
                parseResult = parseResult.subcommand();
                if (parseResult.commandSpec().commandLine().getCommand() instanceof DefaultParameterConfigLoader.ConfigOptions)
                    parseResult = parseResult.subcommand();
                if (parseResult.commandSpec().commandLine().getCommand() instanceof SingeltonTool)
                    return ((SingeltonTool) parseResult.commandSpec().commandLine().getCommand()).getSingeltonWorkflow();

                execute(parseResult.commandSpec().commandLine(), toolchain);
                if (parseResult.commandSpec().commandLine().getCommand() instanceof InputProvider)
                    input = ((InputProvider) parseResult.commandSpec().commandLine().getCommand()).newInputExperimentIterator();
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