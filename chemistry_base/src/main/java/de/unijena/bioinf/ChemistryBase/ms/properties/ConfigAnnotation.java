package de.unijena.bioinf.ChemistryBase.ms.properties;

import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.ParameterConfig;

public abstract class ConfigAnnotation implements DataAnnotation {
    // this are the the configs read from this file (@ParameterConfig)
    public final ParameterConfig config;

    protected ConfigAnnotation(ParameterConfig config) {
        this.config = config;
    }

}
