package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.ms.properties.ConfigAnnotation;
import de.unijena.bioinf.ms.properties.ParameterConfig;

/*this is an annotation that stores the default config from the Projectspace to an MS2Experiment*/
public class ProjectSpaceConfig extends ConfigAnnotation {
    protected ProjectSpaceConfig(ParameterConfig config) {
        super(config);
    }
}
