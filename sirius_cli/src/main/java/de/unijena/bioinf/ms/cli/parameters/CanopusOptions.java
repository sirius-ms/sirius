package de.unijena.bioinf.ms.cli.parameters;

import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.io.File;
/**
 * This is for Canopus specific parameters.
 *
 * They may be annotated to the MS2 Experiment
 *
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 * */
@CommandLine.Command(name = "canopus", aliases = {"C"}, description = "Predict compound categories for the whole dataset using CANOPUS.", defaultValueProvider = Provide.Defaults.class, versionProvider = Provide.Versions.class,  mixinStandardHelpOptions = true)

public class CanopusOptions {

    @Option(names = {"--experimental-canopus"}, hidden = true) //experimental/temporary/internal
    public File experimentalCanopus;

}
