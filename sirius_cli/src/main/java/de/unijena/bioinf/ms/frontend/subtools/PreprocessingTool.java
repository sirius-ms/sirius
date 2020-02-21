package de.unijena.bioinf.ms.frontend.subtools;


import de.unijena.bioinf.ms.properties.ParameterConfig;

public interface PreprocessingTool<T extends PreprocessingJob<?>> {
    T makePreprocessingJob(RootOptions<?,?> rootOptions, ParameterConfig config);
}
