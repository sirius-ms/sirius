package de.unijena.bioinf.ChemistryBase.ms.properties;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.ParameterConfig;

public class FinalConfig extends ConfigAnnotation implements Ms2ExperimentAnnotation {
    public FinalConfig(ParameterConfig config) {
        super(config);
    }
}
