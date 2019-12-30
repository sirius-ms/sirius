package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;

import java.net.URL;

public abstract class SourceLocation implements Ms2ExperimentAnnotation {
    public final URL value;

    protected SourceLocation(URL value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
