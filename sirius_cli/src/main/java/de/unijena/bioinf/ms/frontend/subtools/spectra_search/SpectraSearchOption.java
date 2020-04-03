package de.unijena.bioinf.ms.frontend.subtools.spectra_search;

import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.frontend.subtools.StandaloneTool;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import picocli.CommandLine;

@CommandLine.Command(name = "spectra-search",  description = "<STANDALONE> Computes the similarity between all compounds/features in the first porject-space (queries) one vs all compounds/features in the second porject-space (library).",  versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, showDefaultValues = true)
public class SpectraSearchOption implements StandaloneTool<SpectraSearchWorkflow> {
 // todo we want to implement a spectral library searches bases on cosine and similarity search.
    @Override
    public SpectraSearchWorkflow makeWorkflow(RootOptions<?, ?, ?> rootOptions, ParameterConfig config) {
        return null;
    }
}
