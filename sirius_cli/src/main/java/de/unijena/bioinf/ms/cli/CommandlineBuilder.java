package de.unijena.bioinf.ms.cli;

import de.unijena.bioinf.ms.cli.parameters.BasicOptions;
import de.unijena.bioinf.ms.cli.parameters.DefaultParameterOptionLoader;
import de.unijena.bioinf.ms.cli.parameters.canopus.CanopusOptions;
import de.unijena.bioinf.ms.cli.parameters.fingerid.FingerIdOptions;
import de.unijena.bioinf.ms.cli.parameters.sirius.SiriusOptions;
import de.unijena.bioinf.ms.cli.parameters.zodiac.ZodiacOptions;
import de.unijena.bioinf.sirius.core.ApplicationCore;
import picocli.CommandLine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class CommandlineBuilder {
    //root
    public final CommandLine.Model.CommandSpec rootSpec;
    public final BasicOptions basicOptions = new BasicOptions();

    //global configs (subtool)
    DefaultParameterOptionLoader configOptionLoader = new DefaultParameterOptionLoader();

    //subtools
    public final SiriusOptions siriusOptions = new SiriusOptions(configOptionLoader);
    public final ZodiacOptions zodiacOptions = new ZodiacOptions(configOptionLoader);
    public final FingerIdOptions fingeridOptions = new FingerIdOptions(configOptionLoader);
    public final CanopusOptions canopusOptions = new CanopusOptions(configOptionLoader);


    public CommandlineBuilder() throws IOException {
        CommandLine.Model.CommandSpec fingeridSpec = forAnnotatedObjectWithSubCommands(fingeridOptions, canopusOptions);
        CommandLine.Model.CommandSpec zodiacSpec = forAnnotatedObjectWithSubCommands(zodiacOptions, fingeridSpec);
        CommandLine.Model.CommandSpec siriusSpec = forAnnotatedObjectWithSubCommands(siriusOptions, zodiacSpec, fingeridSpec);

        CommandLine.Model.CommandSpec configSpec = forAnnotatedObjectWithSubCommands(configOptionLoader.asCommandSpec(), siriusSpec, zodiacSpec, fingeridSpec, canopusOptions);
        rootSpec = forAnnotatedObjectWithSubCommands(basicOptions, configSpec, siriusSpec, zodiacSpec, fingeridSpec, canopusOptions);

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

    ParseResultHandler makeParseResultHandler() {
        return new ParseResultHandler();
    }


    private class ParseResultHandler extends CommandLine.AbstractParseResultHandler<List<Object>> {
        @Override
        protected List<Object> handle(CommandLine.ParseResult parseResult) throws CommandLine.ExecutionException {
            //todo check for citation command
            //todo this shoud already create a valid job factory list
            //here we create the workflow we will execute later
            List<Object> result = new ArrayList<>();
            execute(parseResult.commandSpec().commandLine(), result);
            while (parseResult.hasSubcommand()) {
                parseResult = parseResult.subcommand();
                execute(parseResult.commandSpec().commandLine(), result);
            }
            return returnResultOrExit(result);
        }

        private List<Object> execute(CommandLine parsed, List<Object> executionResult) {
            Object command = parsed.getCommand();
            if (command instanceof Runnable) {
                try {
                    ((Runnable) command).run();
                    return executionResult;
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
                    return executionResult;
                } catch (CommandLine.ParameterException ex) {
                    throw ex;
                } catch (CommandLine.ExecutionException ex) {
                    throw ex;
                } catch (Exception ex) {
                    throw new CommandLine.ExecutionException(parsed, "Error while calling command (" + command + "): " + ex, ex);
                }
            }
            throw new CommandLine.ExecutionException(parsed, "Parsed command (" + command + ") is not Method, Runnable or Callable");
        }

        @Override
        protected ParseResultHandler self() {
            return this;
        }
    }


}