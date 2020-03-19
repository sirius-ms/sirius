package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
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
    public enum Keys {LCMS_ALIGN, MS1_PREPROCESSOR}

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

    public Optional<PossibleAdducts> getAdducts() {
        return getAdducts(Keys.values());
    }

    public Optional<PossibleAdducts> getAdducts(Keys... keyPrio) {
        return getAdducts(Arrays.stream(keyPrio).map(Keys::name).toArray(String[]::new));
    }

    public Optional<PossibleAdducts> getAdducts(String... keyPrio) {
        for (String key : keyPrio)
            if (containsKey(key))
                return Optional.of(get(key));

        return Optional.empty();
    }

    public PossibleAdducts getAllAdducts() {
        return values().stream().flatMap(it -> it.getAdducts().stream()).collect(Collectors.collectingAndThen(Collectors.toSet(), PossibleAdducts::new));
    }


    public boolean hasAdducts() {
        if (isEmpty())
            return false;
        return values().stream().anyMatch(it -> !it.isEmpty());
    }

    public boolean containsKey(Keys key) {
        return containsKey(key.name());
    }

    public PossibleAdducts get(Keys key) {
        return get(key.name());
    }

    public PossibleAdducts put(@NotNull Keys key, @NotNull PossibleAdducts value) {
        return put(key.name(), value);
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
