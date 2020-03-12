package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


/**
 * Collects multiple PossibleAdducts adducts from different sources and stores a source identifier.
 * This is intended to collect Adducts from different detection sources.
 * Can be attached to MsExperiment
 */
public final class DetectedAdducts extends ConcurrentHashMap<String, PossibleAdducts> implements Ms2ExperimentAnnotation {

    public DetectedAdducts() {
        super();
    }

    public DetectedAdducts(Class<?> producer, PossibleAdducts adducts) {
        this(producer.getSimpleName(), adducts);
    }

    public DetectedAdducts(String id, PossibleAdducts adducts) {
        this();
        put(id, adducts);
    }

    public LinkedHashSet<PrecursorIonType> getAdducts() {
        return values().stream().flatMap(it -> it.getAdducts().stream()).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public PossibleAdducts asPossibleAdducts() {
        return new PossibleAdducts(getAdducts());
    }

    @Override
    public String toString() {
        return toString(this);
    }

    public static String toString(DetectedAdducts in) {
        return in.entrySet().stream().map(e -> e.getKey() + ":{" + e.getValue().getAdducts().stream().map(PrecursorIonType::toString).collect(Collectors.joining(",")) + "}").collect(Collectors.joining(","));
    }

    public static DetectedAdducts fromString(String json) {
        final DetectedAdducts ads = new DetectedAdducts();
        String[] mappings = json.split("\\s*}\\s*,\\s*");
        for (String mapping : mappings) {
            String[] keyValue = mapping.replace("}", "").split("\\s*(:|->)\\s*\\{\\s*");
            PossibleAdducts val = keyValue.length > 1 ? Arrays.stream(keyValue[1].split(",")).filter(Objects::nonNull).filter(s -> !s.isBlank()).map(PrecursorIonType::parsePrecursorIonType).flatMap(Optional::stream)
                    .collect(Collectors.collectingAndThen(Collectors.toSet(), PossibleAdducts::new)) : new PossibleAdducts();
            ads.put(keyValue[0], val);
        }
        return ads;

    }
}
