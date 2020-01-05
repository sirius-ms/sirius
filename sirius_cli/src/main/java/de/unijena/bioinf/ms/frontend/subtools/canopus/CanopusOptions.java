package de.unijena.bioinf.ms.frontend.subtools.canopus;

import de.unijena.bioinf.ms.frontend.subtools.InstanceJob;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
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
@CommandLine.Command(name = "canopus", aliases = {"C"}, description = "<COMPOUND_TOOL> Predict compound categories for the whole dataset using CANOPUS.", defaultValueProvider = Provide.Defaults.class, versionProvider = Provide.Versions.class,  mixinStandardHelpOptions = true)
public class CanopusOptions implements Callable<InstanceJob.Factory<CanopusSubToolJob>> {
    protected final DefaultParameterConfigLoader defaultConfigOptions;

    public CanopusOptions(DefaultParameterConfigLoader defaultConfigOptions) {
        this.defaultConfigOptions = defaultConfigOptions;
    }

    @Override
    public InstanceJob.Factory<CanopusSubToolJob> call() throws Exception {
        return CanopusSubToolJob::new;
    }
}
