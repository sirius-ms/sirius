package de.unijena.bioinf.ms.annotations;

import de.unijena.bioinf.ms.properties.DefaultProperty;

@DefaultProperty
public class RecomputeResults implements Ms2ExperimentAnnotation {
    public static final RecomputeResults TRUE = new RecomputeResults(true);
    public static final RecomputeResults FALSE = new RecomputeResults(false);

    public final boolean value;

    private RecomputeResults() {
        this(false);
    }
    public RecomputeResults(boolean value) {
        this.value = value;
    }
}
