package de.unijena.bioinf.ms.annotations;

import de.unijena.bioinf.ms.properties.DefaultInstanceProvider;
import de.unijena.bioinf.ms.properties.DefaultProperty;

public class RecomputeResults implements Ms2ExperimentAnnotation {
    public static final RecomputeResults TRUE = new RecomputeResults(true);
    public static final RecomputeResults FALSE = new RecomputeResults(false);

    public final boolean value;

    private RecomputeResults(boolean value) {
        this.value = value;
    }

    @DefaultInstanceProvider
    public static RecomputeResults newInstance(@DefaultProperty boolean value){
        return value ? TRUE : FALSE;
    }
}
