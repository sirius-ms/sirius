package de.unijena.bioinf.treemotifs.model;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultProperty;

@DefaultProperty
public class MotifDbFile implements Ms2ExperimentAnnotation {

    public String value;

    public MotifDbFile() {
        this.value = "";
    }
}
