package de.unijena.bioinf.ms.frontend.subtools.passatutto;

import de.unijena.bioinf.ms.frontend.subtools.InstanceJob;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.ToolChainJob;
import de.unijena.bioinf.ms.frontend.subtools.ToolChainOptions;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.frontend.subtools.fingerid.FingerIdOptions;
import de.unijena.bioinf.ms.frontend.subtools.zodiac.ZodiacOptions;
import picocli.CommandLine;

import java.util.List;

@CommandLine.Command(name = "passatutto", aliases = {"P"}, description = "<COMPOUND_TOOL> Compute a decoy spectra based on the fragmentation trees of the given input spectra. If no molecular formula is provided in the input, the top scoring computed formula is used.", versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, showDefaultValues = true)
public class PassatuttoOptions implements ToolChainOptions<PassatuttoSubToolJob, InstanceJob.Factory<PassatuttoSubToolJob>> {

    protected final DefaultParameterConfigLoader defaultConfigOptions;

    public PassatuttoOptions(DefaultParameterConfigLoader defaultConfigOptions) {
        this.defaultConfigOptions = defaultConfigOptions;
    }

    @Override
    public InstanceJob.Factory<PassatuttoSubToolJob> call() {
        return PassatuttoSubToolJob::new;
    }

    @Override
    public ToolChainJob.Invalidator getInvalidator() {
        return null;
    }

    @Override
    public List<Class<? extends ToolChainOptions<?, ?>>> getSubCommands() {
        return List.of(FingerIdOptions.class);
    }
}
