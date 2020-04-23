package de.unijena.bioinf.ms.frontend.subtools.canopus;

import de.unijena.bioinf.ms.frontend.subtools.InstanceJob;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.ToolChainJob;
import de.unijena.bioinf.ms.frontend.subtools.ToolChainOptions;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.frontend.subtools.sirius.SiriusSubToolJob;
import picocli.CommandLine;
import picocli.CommandLine.Option;


import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * This is for Canopus specific parameters.
 *
 * They may be annotated to the MS2 Experiment
 *
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 * */
@CommandLine.Command(name = "canopus", aliases = {"C"}, description = "<COMPOUND_TOOL> Predict compound categories for each compound individually based on its predicted molecular fingerprint (CSI:FingerID) using CANOPUS.", versionProvider = Provide.Versions.class,  mixinStandardHelpOptions = true, showDefaultValues = true)
public class CanopusOptions implements ToolChainOptions<CanopusSubToolJob, InstanceJob.Factory<CanopusSubToolJob>> {
    protected final DefaultParameterConfigLoader defaultConfigOptions;

    public CanopusOptions(DefaultParameterConfigLoader defaultConfigOptions) {
        this.defaultConfigOptions = defaultConfigOptions;
    }

    @Override
    public InstanceJob.Factory<CanopusSubToolJob> call() throws Exception {
        return CanopusSubToolJob::new;
    }

    @Override
    public ToolChainJob.Invalidator getInvalidator() {
        return null;
    }

    @Override
    public List<Class<? extends ToolChainOptions<?, ?>>> getSubCommands() {
        return List.of();
    }
}
