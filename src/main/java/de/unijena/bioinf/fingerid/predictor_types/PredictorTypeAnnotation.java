package de.unijena.bioinf.fingerid.predictor_types;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultInstanceProvider;
import de.unijena.bioinf.ms.properties.DefaultProperty;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;


public class PredictorTypeAnnotation implements Ms2ExperimentAnnotation {
    public final EnumSet<UserDefineablePredictorType> value;


    public PredictorTypeAnnotation(EnumSet<UserDefineablePredictorType> value) {
        this.value = value;
    }

    @DefaultInstanceProvider
    public static PredictorTypeAnnotation newInstance(@DefaultProperty(propertyParent = "StructurePredictors") Set<UserDefineablePredictorType> value) {
        return new PredictorTypeAnnotation(EnumSet.copyOf(value));
    }

    public EnumSet<PredictorType> toPredictors(final int charge) {
        return EnumSet.copyOf(value.stream().map(type -> type.toPredictorType(charge)).collect(Collectors.toSet()));
    }

}
