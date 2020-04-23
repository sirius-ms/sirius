package de.unijena.bioinf.ms.frontend.subtools.passatutto;

import de.unijena.bioinf.ms.frontend.subtools.InstanceJob;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.ToolChainOptions;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.frontend.subtools.fingerid.FingerIdOptions;
import de.unijena.bioinf.passatutto.Decoy;
import de.unijena.bioinf.projectspace.Instance;
import picocli.CommandLine;

import java.util.List;
import java.util.function.Consumer;

@CommandLine.Command(name = "passatutto", aliases = {"P"}, description = "<COMPOUND_TOOL> Compute a decoy spectra based on the fragmentation trees of the given input spectra. If no molecular formula is provided in the input, the top scoring computed formula is used.", versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, showDefaultValues = true)
public class PassatuttoOptions implements ToolChainOptions<PassatuttoSubToolJob, InstanceJob.Factory<PassatuttoSubToolJob>> {

    protected final DefaultParameterConfigLoader defaultConfigOptions;

    public PassatuttoOptions(DefaultParameterConfigLoader defaultConfigOptions) {
        this.defaultConfigOptions = defaultConfigOptions;
    }

    @Override
    public InstanceJob.Factory<PassatuttoSubToolJob> call() {
        return new InstanceJob.Factory<>(
                PassatuttoSubToolJob::new,
                getInvalidator()
        );
    }

    @Override
    public Consumer<Instance> getInvalidator() {
        return inst -> inst.deleteFromFormulaResults(Decoy.class);
    }

    @Override
    public List<Class<? extends ToolChainOptions<?, ?>>> getSubCommands() {
        return List.of(FingerIdOptions.class);
    }
}
