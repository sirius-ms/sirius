package de.unijena.bioinf.ms.frontend.subtools.canopus;

import de.unijena.bioinf.canopus.CanopusResult;
import de.unijena.bioinf.ms.frontend.subtools.InstanceJob;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.ToolChainOptions;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.projectspace.Instance;
import picocli.CommandLine;

import java.util.List;
import java.util.function.Consumer;

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
        return new InstanceJob.Factory<>(
                CanopusSubToolJob::new,
                getInvalidator()
        );
    }

    @Override
    public Consumer<Instance> getInvalidator() {
        return inst -> inst.deleteFromFormulaResults(CanopusResult.class);
    }

    @Override
    public List<Class<? extends ToolChainOptions<?, ?>>> getSubCommands() {
        return List.of();
    }
}
