package de.unijena.bioinf.ms.cli.workflow;

import de.unijena.bioinf.ms.cli.parameters.RootOptions;
import de.unijena.bioinf.ms.cli.parameters.canopus.CanopusOptions;
import de.unijena.bioinf.ms.cli.parameters.config.DefaultParameterOptionLoader;
import de.unijena.bioinf.ms.cli.parameters.fingerid.FingerIdOptions;
import de.unijena.bioinf.ms.cli.parameters.sirius.SiriusOptions;
import de.unijena.bioinf.ms.cli.parameters.zodiac.ZodiacOptions;
import de.unijena.bioinf.sirius.core.ApplicationCore;
import org.jetbrains.annotations.NotNull;
import picocli.CommandLine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class WorkflowBuilder<R extends RootOptions> {

    //root
    public final CommandLine.Model.CommandSpec rootSpec;
    public final R rootOptions;

    //global configs (subtool)
    DefaultParameterOptionLoader configOptionLoader = new DefaultParameterOptionLoader();

    //subtools
    public final SiriusOptions siriusOptions = new SiriusOptions(configOptionLoader);
    public final ZodiacOptions zodiacOptions = new ZodiacOptions(configOptionLoader);
    public final FingerIdOptions fingeridOptions = new FingerIdOptions(configOptionLoader);
    public final CanopusOptions canopusOptions = new CanopusOptions(configOptionLoader);


    public WorkflowBuilder(@NotNull R rootOptions) throws IOException {
        this.rootOptions = rootOptions;

        CommandLine.Model.CommandSpec fingeridSpec = forAnnotatedObjectWithSubCommands(fingeridOptions, canopusOptions);
        CommandLine.Model.CommandSpec zodiacSpec = forAnnotatedObjectWithSubCommands(zodiacOptions, fingeridSpec);
        CommandLine.Model.CommandSpec siriusSpec = forAnnotatedObjectWithSubCommands(siriusOptions, zodiacSpec, fingeridSpec);

        CommandLine.Model.CommandSpec configSpec = forAnnotatedObjectWithSubCommands(configOptionLoader.asCommandSpec(), siriusSpec, zodiacSpec, fingeridSpec, canopusOptions);
        rootSpec = forAnnotatedObjectWithSubCommands(this.rootOptions, configSpec, siriusSpec, zodiacSpec, fingeridSpec, canopusOptions);

        rootSpec.usageMessage().footerHeading(System.lineSeparator() + System.lineSeparator() + "Please cite the following publications when using our tool:" + System.lineSeparator() + System.lineSeparator());
        rootSpec.usageMessage().footer(ApplicationCore.CITATION);
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
            //todo check for citation command
            //here we create the workflow that we will execute later
            List<Object> result = new ArrayList<>();
            execute(parseResult.commandSpec().commandLine(), result);

            while (parseResult.hasSubcommand()) {
                parseResult = parseResult.subcommand();
                execute(parseResult.commandSpec().commandLine(), result);
            }

            final List<Object> toolchain = new ArrayList<>(result.subList(1, result.size()));
            return returnResultOrExit(new Workflow((RootOptions.IO) result.get(0), configOptionLoader.config, toolchain));
        }

        private void execute(CommandLine parsed, List<Object> executionResult) {
            Object command = parsed.getCommand();
            if (command instanceof Runnable) {
                try {
                    ((Runnable) command).run();
                } catch (CommandLine.ParameterException ex) {
                    throw ex;
                } catch (CommandLine.ExecutionException ex) {
                    throw ex;
                } catch (Exception ex) {
                    throw new CommandLine.ExecutionException(parsed, "Error while running command (" + command + "): " + ex, ex);
                }
            } else if (command instanceof Callable) {
                try {
                    @SuppressWarnings("unchecked") Callable<Object> callable = (Callable<Object>) command;
                    executionResult.add(callable.call());
                } catch (CommandLine.ParameterException ex) {
                    throw ex;
                } catch (CommandLine.ExecutionException ex) {
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