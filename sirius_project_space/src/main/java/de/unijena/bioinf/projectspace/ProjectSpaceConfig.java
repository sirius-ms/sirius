package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.ms.properties.ConfigAnnotation;
import de.unijena.bioinf.ms.properties.ParameterConfig;

/*this is an annotation that saves the global defaultproperties to the project space*/
public class ProjectSpaceConfig extends ConfigAnnotation {
    protected ProjectSpaceConfig(ParameterConfig config) {
        super(config);
    }
}
