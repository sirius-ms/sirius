package de.unijena.bioinf.ms.frontend.subtools;


import de.unijena.bioinf.ms.properties.ParameterConfig;

public interface PostprocessingTool<T extends PostprocessingJob<?>> {
    T makePostprocessingJob(RootOptions<?,?,?> rootOptions, ParameterConfig config);
}
