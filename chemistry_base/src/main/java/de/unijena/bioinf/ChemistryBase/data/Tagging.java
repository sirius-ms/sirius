package de.unijena.bioinf.ChemistryBase.data;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Stream;

/**
 * A tag is an arbitrary label put onto Ms2Experiments. It can be used to organize compounds into datasets, control groups, and so on
 */
public final class Tagging implements Ms2ExperimentAnnotation, Iterable<String> {

    private final static Tagging EMPTY = new Tagging();

    public static Tagging none() {
        return EMPTY;
    }

    private final HashSet<String> labels;

    private Tagging(HashSet<String> lb) {
        this.labels = lb;
    }

    public Tagging(String... labels) {
        this.labels = new HashSet<>();
        if (labels.length>0) this.labels.addAll(Arrays.asList(labels));
    }

    public Tagging concat(Tagging other) {
        final HashSet<String> copy = new HashSet<>(labels);
        copy.addAll(other.labels);
        return new Tagging(copy);
    }

    public Tagging concat(String... labels) {
        final HashSet<String> copy = new HashSet<>(Arrays.asList(labels));
        copy.addAll(Arrays.asList(labels));
        return new Tagging(copy);
    }

    public boolean isEmpty() {
        return labels.isEmpty();
    }

    public boolean contains(String key) {
        return labels.contains(key);
    }

    public Stream<String> stream() {
        return Collections.unmodifiableSet(labels).stream();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        Tagging strings = (Tagging) o;
        return labels.equals(strings.labels);
    }

    @Override
    public int hashCode() {
        return Objects.hash(labels);
    }

    @NotNull
    @Override
    public Iterator<String> iterator() {
        return Collections.unmodifiableSet(labels).iterator();
    }
}
