package de.unijena.bioinf.ms.cli.parameters.canopus;

import de.unijena.bioinf.ms.cli.parameters.DefaultParameterOptionLoader;
import de.unijena.bioinf.ms.cli.parameters.Provide;
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
    protected final DefaultParameterOptionLoader defaultConfigOptions;

    public CanopusOptions(DefaultParameterOptionLoader defaultConfigOptions) {
        this.defaultConfigOptions = defaultConfigOptions;
    }
    @Option(names = {"--experimental-canopus"}, hidden = true) //experimental/temporary/internal
    public File experimentalCanopus;

}
