/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.annotations;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public interface Annotated<A extends DataAnnotation> {

    Annotations<A> annotations();

    /**
     * An iterator over all map
     */
    default void forEachAnnotation(BiConsumer<? super Class<A>, ? super A> action) {
        annotations().forEach(action);
    }

    default Iterator<Map.Entry<Class<A>, A>> annotationIterator() {
        return annotations().map.entrySet().iterator();
    }


    /**
     * @return annotation value for the given class/key
     * @throws NullPointerException if there is no entry for this key
     */
    @NotNull
    @JsonIgnore
    default <T extends A> T getAnnotationOrThrow(Class<T> klass) {
        return getAnnotationOrThrow(klass, () -> new NullPointerException("No annotation for key: " + klass.getName()));
    }

    /**
     * @param exceptionSupplier exception to throw if key does not exist
     * @return annotation value for the given class/key
     * @throws RuntimeException of your choice (@param ex)
     */
    @NotNull
    @JsonIgnore
    default <T extends A> T getAnnotationOrThrow(Class<T> klass, Supplier<? extends RuntimeException> exceptionSupplier) {
        return getAnnotation(klass).orElseThrow(exceptionSupplier);
    }

    /**
     * @return annotation value for the given class/key or null
     */
    @JsonIgnore
    default <T extends A> T getAnnotationOrNull(@NotNull Class<T> klass) {
        return (T) annotations().map.get(klass);
    }

    /**
     * @return annotation value for the given class/key or the given default value
     */
    @JsonIgnore
    default <T extends A> T getAnnotation(@NotNull Class<T> klass, @NotNull Supplier<T> defaultValueSupplier) {
        return getAnnotation(klass).orElseGet(defaultValueSupplier);
    }

    @JsonIgnore
    default <T extends A> T getAnnotationOr(@NotNull Class<T> klass, @NotNull Function<Class<T>, T> defaultValueFunction) {
        return getAnnotation(klass).orElse(defaultValueFunction.apply(klass));
    }

    @JsonIgnore
    default <T extends A> Optional<T> getAnnotation(@NotNull Class<T> klass) {
        final T val = getAnnotationOrNull(klass);
        if (val == null) return Optional.empty();
        else return Optional.of(val);
    }

    /**
     * @return true if the given annotation is present
     */
    default <T extends A> boolean hasAnnotation(Class<T> klass) {
        return annotations().map.containsKey(klass);
    }

    /**
     * Set the annotation with the given key
     * Setting the value to null will remove the annotation (the key-value-pair from the map)
     *
     *
     * @return true if there was no previous value for this annotation
     */
    @JsonIgnore
    default <T extends A> boolean setAnnotation(Class<T> klass, T value) {
        if (value == null)
            return removeAnnotation(klass) != null;

        final T val = (T) annotations().map.put((Class<A>) klass, value);
        fireAnnotationChange(val, value);
        return val != null;
    }

    /**
     * Set the annotation with lazily extracted the Key
     * Setting the value to null will be ignored since the Key cannot be extracted
     *
     * @param annotation The value that will be annotated to this object.
     *
     * @return The value that was annotated or null
     */
    default <D extends A> D annotate(@Nullable final D annotation) {
        return annotate(annotation, true);
    }

    /**
     * Annotates this object with lazily extracted the Key
     * Setting the value to null will be ignored since the Key cannot be extracted
     *
     * @param annotation The value that will be annotated to this object.
     * @param overrideExisting if true existing value will be overwritten.
     *
     * @return The value that was annotated or null
     */
    default <D extends A> D annotate(@Nullable final D annotation, boolean overrideExisting) {
        if (annotation != null) {
            Class<D> clzz = (Class<D>) annotation.getClass();
            if (overrideExisting || !annotations().map.containsKey(clzz))
                setAnnotation(clzz, annotation);
        }
        return annotation;
    }

    default <D extends A> D takeAndAnnotate(@NotNull final JJob<D> resultJJob) {
        return annotate(resultJJob.takeResult());
    }


    default <T extends A> void addAnnotationIfAbsend(Class<T> klass, T value) {
        if (!annotations().map.containsKey(klass))
            setAnnotation(klass, value);
    }

    default <T extends A> void addAnnotation(Class<T> klass, T value) {
        if (annotations().map.containsKey(klass))
            throw new RuntimeException("Annotation '" + klass.getName() + "' is already present.");
        setAnnotation(klass, value);
    }

    /**
     * Compute the annotation with the given key if it is absent.
     * Return the current value otherwise.
     *
     * @return true if there was no previous value for this annotation
     */
    default <T extends A> T computeAnnotationIfAbsent(final Class<T> klass, Supplier<T> defaultValueSupplier) {
        return (T) annotations().map.computeIfAbsent((Class<A>) klass, (c) -> {
            T newVal = defaultValueSupplier.get();
            return fireAnnotationChange(null, newVal);
        });
    }


    /**
     * Remove the annotation with the given key
     *
     * @return the value associated with this key or null if there is no value for this key
     */

    default <T extends A> T removeAnnotation(Class<T> klass) {
        final T old = (T) annotations().map.remove(klass);
        fireAnnotationChange(old, null);
        return old;
    }

    /**
     * Remove all map from this experiment
     */
    default void clearAnnotations() {
        final Iterator<Class<A>> it = annotations().iterator();
        while (it.hasNext()) {
            getAnnotation(it.next()).ifPresent(old -> {
                it.remove();
                fireAnnotationChange(old, null);
            });
        }
    }

    /**
     * Add all given annotations. Overrides existing.
     *
     * @param annotated annotations to add
     */
    @JsonIgnore
    default void setAnnotationsFrom(Annotated<A> annotated) {
        final Iterator<Map.Entry<Class<A>, A>> iter = annotated.annotationIterator();
        while (iter.hasNext()) {
            final Map.Entry<Class<A>, A> v = iter.next();
            setAnnotation(v.getKey(), v.getValue());
        }
    }

    /**
     * Add all given annotations. Overrides existing.
     *
     * @param annotations annotations to add
     */
    @JsonIgnore
    default void setAnnotationsFrom(Map<Class<A>, A> annotations) {
        annotations.forEach(this::setAnnotation);
    }

    /**
     * Add all annotations of type  {@literal Class<A>} from the given config. Overrides existing.
     * Missing keys will be skipped with warning.
     *
     * @param config from which the annotations will be add
     * @param clz type of the annotations that will be parsed
     */
    @JsonIgnore
    default void setAnnotationsFrom(ParameterConfig config, Class<A> clz) {
        setAnnotationsFrom(config, clz, true);
    }

    /**
     * Add all annotations of type {@literal Class<A>} from the given config. Overrides existing.
     *
     * @param config from which the annotations will be add
     * @param clz type of the annotations that will be parsed
     * @param skipMissingKeys specify how to handle missing keys*
     */
    @JsonIgnore
    default void setAnnotationsFrom(ParameterConfig config, Class<A> clz, boolean skipMissingKeys) {
        config.createInstancesWithDefaults(clz, skipMissingKeys).forEach(this::setAnnotation);
    }

    /**
     * Add all given annotations if they do not exist
     *
     * @param annotated annotations to add
     */
    default void addAnnotationsFrom(Annotated<A> annotated) {
        final Iterator<Map.Entry<Class<A>, A>> iter = annotated.annotationIterator();
        while (iter.hasNext()) {
            final Map.Entry<Class<A>, A> v = iter.next();
            addAnnotationIfAbsend(v.getKey(), v.getValue());
        }
    }

    /**
     * Add all given annotations if they do not exist
     *
     * @param annotations annotations to add
     */
    default void addAnnotationsFrom(Map<Class<A>, A> annotations) {
        annotations.forEach(this::addAnnotationIfAbsend);
    }

    /**
     * Add all annotations of type {@literal Class<A>} from the given config if they do  NOT already exist.
     * Missing keys will be skipped with warning.
     *
     * @param config from which the annotations will be add
     * @param clz type of the annotations that will be parsed
     */
    default void addAnnotationsFrom(ParameterConfig config, Class<A> clz) {
        addAnnotationsFrom(config, clz, true);
    }

    /**
     * Add all annotations of type {@literal Class<A>} from the given config if they do  NOT already exist.
     *
     * @param config from which the annotations will be add
     * @param clz type of the annotations that will be parsed
     * @param skipMissingKeys specify how to handle missing keys
     */
    default void addAnnotationsFrom(ParameterConfig config, Class<A> clz, boolean skipMissingKeys) {
        config.createInstancesWithDefaults(clz, skipMissingKeys).forEach(this::addAnnotationIfAbsend);
    }


    // delegate change support
    //todo doc
    default void addAnnotationChangeListener(PropertyChangeListener listener) {
        annotations().annotationChangeSupport.addPropertyChangeListener(listener);
    }

    default <T extends A> void addAnnotationChangeListener(Class<T> annotationToListenOn, PropertyChangeListener listener) {
        annotations().annotationChangeSupport.addPropertyChangeListener(DataAnnotation.getIdentifier(annotationToListenOn), listener);
    }

    default void removeAnnotationChangeListener(PropertyChangeListener listener) {
        annotations().annotationChangeSupport.removePropertyChangeListener(listener);
    }

    default <T extends A> PropertyChangeListener[] getAnnotationChangeListeners(Class<T> annotationToListenOn) {
        return annotations().annotationChangeSupport.getPropertyChangeListeners(DataAnnotation.getIdentifier(annotationToListenOn));
    }

    default <T extends A> T fireAnnotationChange(T oldValue, T newValue) {
        if (oldValue != null) {
            annotations().annotationChangeSupport.firePropertyChange(oldValue.getIdentifier(), oldValue, newValue);
        } else if (newValue != null) {
            annotations().annotationChangeSupport.firePropertyChange(newValue.getIdentifier(), oldValue, newValue);
        }
        return newValue;
    }

    default boolean hasListeners(String propertyName) {
        return annotations().annotationChangeSupport.hasListeners(propertyName);
    }


    /**
     * This allows us to hide the annotation map from the outside
     * but inject it from the class that implements the interface.
     * So we can implement all annotation functionality within this interface
     * instead of each class separately.
     */
    final class Annotations<Annotation extends DataAnnotation> implements Cloneable, Iterable<Class<Annotation>> {

        @JsonIgnore
        private final PropertyChangeSupport annotationChangeSupport;
        private final Map<Class<Annotation>, Annotation> map;

        public Annotations() {
            this(new ConcurrentHashMap<>());
        }

        private Annotations(Map<Class<Annotation>, Annotation> annotations) {
            this.annotationChangeSupport = new PropertyChangeSupport(this);
            this.map = annotations;
        }

        public Annotations<Annotation> clone() {
            final Map<Class<Annotation>, Annotation> cloneMap = new ConcurrentHashMap<>(map);
            return new Annotations<>(cloneMap);
        }

        @NotNull
        @Override
        public Iterator<Class<Annotation>> iterator() {
            return map.keySet().iterator();
        }

        public Iterator<Annotation> valueIterator() {
            return map.values().iterator();
        }


        public void forEach(BiConsumer<? super Class<Annotation>, ? super Annotation> action) {
            map.forEach(action);
        }

        public Class<Annotation>[] getKeysArray() {
            return map.keySet().toArray(Class[]::new);
        }

    }
}
