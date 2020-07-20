package de.unijena.bioinf.babelms.ms;

import de.unijena.bioinf.ChemistryBase.ms.properties.ConfigAnnotation;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.ParameterConfig;

public class InputFileConfig extends ConfigAnnotation implements Ms2ExperimentAnnotation {
    public InputFileConfig(ParameterConfig config) {
        super(config);
    }
}
