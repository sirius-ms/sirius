package de.unijena.bioinf.ms.cli.parameters.canopus;

import de.unijena.bioinf.ms.cli.parameters.InstanceJob;
import de.unijena.bioinf.ms.cli.parameters.Provide;
import de.unijena.bioinf.ms.cli.parameters.config.DefaultParameterConfigLoader;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.io.File;
import java.util.concurrent.Callable;

/**
 * This is for Canopus specific parameters.
 *
 * They may be annotated to the MS2 Experiment
 *
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 * */
@CommandLine.Command(name = "canopus", aliases = {"C"}, description = "Predict compound categories for the whole dataset using CANOPUS.", defaultValueProvider = Provide.Defaults.class, versionProvider = Provide.Versions.class,  mixinStandardHelpOptions = true)
public class CanopusOptions implements Callable<InstanceJob.Factory<CanopusSubToolJob>> {
    protected final DefaultParameterConfigLoader defaultConfigOptions;

    public CanopusOptions(DefaultParameterConfigLoader defaultConfigOptions) {
        this.defaultConfigOptions = defaultConfigOptions;
    }
    @Option(names = {"--experimental-canopus"}, hidden = true) //experimental/temporary/internal
    public File experimentalCanopus;

    @Override
    public InstanceJob.Factory<CanopusSubToolJob> call() throws Exception {
        return CanopusSubToolJob::new;
    }
}
