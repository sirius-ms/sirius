package de.unijena.bioinf.ms.cli;

import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by ge28quv on 23/05/17.
 */
public class ZodiacCLI<Options extends ZodiacOptions> extends CLI<Options> {
    @Override
    public void compute() {
        if (options.isZodiac()) {
            Zodiac zodiac = new Zodiac(options);
            zodiac.run();
        } else {
            super.compute();
        }

    }

    @Override
    protected void handleOutputOptions(Options options) {
        //todo zodiac output handling should be here at some time
        if (!options.isZodiac()) super.handleOutputOptions(options);
    }

    /*@Override
    public void parseArgs(String[] args) {
        super.parseArgs(args);
    }

    @Override
    protected void parseArgsAndInit(String[] args) {
        super.parseArgsAndInit(args);
    }*/

    /*@Override
    public void parseArgs(String[] args, Class<Options> optionsClass) {
        try {
            if (isZodiac(args)) {
                if (args.length == 1) {
                    System.out.println(CliFactory.createCli(ZodiacOptions.class).getHelpMessage());
                    System.exit(0);
                }
                zodiacOptions = CliFactory.createCli(ZodiacOptions.class).parseArguments(Arrays.copyOfRange(args, 1, args.length));
                super.parseArgs(new String[]{"--zodiac"}, optionsClass);
            } else {
                super.parseArgs(args, optionsClass);
            }
        } catch (HelpRequestedException e) {
            super.parseArgs(args, optionsClass);
        }
    }*/


    /*private boolean isZodiac(String[] args) {
        for (String arg : args) {
            if (arg.toLowerCase().equals("--zodiac")) return true;
        }
        return false;
    }*/


    @Override
    public void setup() {
        if (!options.isZodiac()) {
            super.setup();
            return;
        }
        Path output = Paths.get(options.getOutput());
        if (!Files.exists(output)) {
            try {
                Files.createDirectories(output);
            } catch (IOException e) {
                LoggerFactory.getLogger(this.getClass()).error("Cannot create output directory: " + e.getMessage());
            }
        }
    }

    @Override
    public void validate() {
        if (!options.isZodiac()) {
            super.validate();
            return;
        }
        Path output = Paths.get(options.getOutput());
        if (!Files.isDirectory(output) && Files.exists(output)) {
            LoggerFactory.getLogger(this.getClass()).error("the output must be a directory or non-existing.");
            System.exit(1);
        }
    }

    /*private boolean isZodiac() {
        return zodiacOptions != null;
    }*/
}
