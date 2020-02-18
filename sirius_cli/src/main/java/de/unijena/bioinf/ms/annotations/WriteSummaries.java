package de.unijena.bioinf.ms.annotations;

import de.unijena.bioinf.ms.properties.DefaultInstanceProvider;
import de.unijena.bioinf.ms.properties.DefaultProperty;

public class WriteSummaries implements Ms2ExperimentAnnotation {
    public static final WriteSummaries TRUE = new WriteSummaries(true);
    public static final WriteSummaries FALSE = new WriteSummaries(false);

    public final boolean value;

    private WriteSummaries(boolean value) {
        this.value = value;
    }

    @DefaultInstanceProvider
    public static WriteSummaries newInstance(@DefaultProperty boolean value){
        return value ? TRUE : FALSE;
    }
}
