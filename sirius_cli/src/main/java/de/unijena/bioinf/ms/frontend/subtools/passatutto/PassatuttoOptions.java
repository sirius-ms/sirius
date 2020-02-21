package de.unijena.bioinf.ms.frontend.subtools.passatutto;

import de.unijena.bioinf.ms.frontend.subtools.InstanceJob;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.frontend.subtools.fingerid.FingeridSubToolJob;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "passatutto", aliases = {"P"}, description = "<COMPOUND_TOOL> Compute a decoy spectra based on the fragmentation trees of the given input spectra. If no molecular formula is provided in the input, the top scoring computed formula is used.", defaultValueProvider = Provide.Defaults.class, versionProvider = Provide.Versions.class,  mixinStandardHelpOptions = true)
public class PassatuttoOptions implements Callable<InstanceJob.Factory<PassatuttoSubToolJob>> {

    protected final DefaultParameterConfigLoader defaultConfigOptions;

    public PassatuttoOptions(DefaultParameterConfigLoader defaultConfigOptions) {
        this.defaultConfigOptions = defaultConfigOptions;
    }

    @Override
    public InstanceJob.Factory<PassatuttoSubToolJob> call() throws Exception {
        return PassatuttoSubToolJob::new;
    }

}
