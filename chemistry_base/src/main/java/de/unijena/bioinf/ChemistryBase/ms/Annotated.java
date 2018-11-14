package de.unijena.bioinf.ChemistryBase.ms;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public interface Annotated<Annotation> {

    Annotations<Annotation> annotations();

    /**
     * @return an iterator over all map
     */
    default Iterator<Map.Entry<Class<Annotation>, Annotation>> forEachAnnotation() {
        return annotations().map.entrySet().iterator();
    }


    /**
     * @return annotation value for the given class/key
     * @throws NullPointerException if there is no entry for this key
     */
    default <T extends Annotation> T getAnnotationOrThrow(Class<T> klass) {
        final T val = getAnnotation(klass);
        if (val == null) throw new NullPointerException("No annotation for key: " + klass.getName());
        else return val;
    }

    /**
     * @return annotation value for the given class/key or null
     */
    default <T extends Annotation> T getAnnotation(Class<T> klass) {
        return (T) annotations().map.get(klass);
    }

    /**
     * @return annotation value for the given class/key or the given default value
     */
    /*default <T extends Annotation> T getAnnotation(Class<T> klass, T defaultValue) {
        final T val = getAnnotation(klass);
        if (val == null) return defaultValue;
        else return val;
    }
*/
    /**
     * @return annotation value for the given class/key or the given default value
     */
    default <T extends Annotation> T getAnnotation(Class<T> klass, Supplier<T> defaultValueSupplier) {
        final T val = getAnnotation(klass);
        if (val == null) return defaultValueSupplier.get();
        else return val;
    }

    /**
     * @return true if the given annotation is present
     */
    default <T extends Annotation> boolean hasAnnotation(Class<T> klass) {
        return annotations().map.containsKey(klass);
    }

    /**
     * Set the annotation with the given key
     *
     * @return true if there was no previous value for this annotation
     */
    default <T extends Annotation> boolean setAnnotation(Class<T> klass, T value) {
        final T val = (T) annotations().map.put((Class<Annotation>) klass, value);
        return val != null;
    }

    /**
     * Set the annotation with the given key
     *
     * @return true if there was no previous value for this annotation
     */
    default <T extends Annotation> T computeAnnotationIfAbsent(Class<T> klass, Function<Class<Annotation>, Annotation> mappingFunction) {
        return (T) annotations().map.computeIfAbsent((Class<Annotation>) klass, mappingFunction);
    }


    /**
     * Remove the annotation with the given key
     *
     * @return the value associated with this key or null if there is no value for this key
     */

    default <T extends Annotation> Object clearAnnotation(Class<T> klass) {
        return annotations().map.remove(klass);
    }

    /**
     * Remove all map from this experiment
     */
    default void clearAllAnnotations() {
        annotations().map.clear();
    }

    //overrides existing
    default void setAnnotationsFrom(Annotated<Annotation> annotated) {
        final Iterator<Map.Entry<Class<Annotation>, Annotation>> iter = annotated.forEachAnnotation();
        while (iter.hasNext()) {
            final Map.Entry<Class<Annotation>, Annotation> v = iter.next();
            this.annotations().map.put(v.getKey(), v.getValue());
        }
    }

    //doe not override existing
    default void addAnnotationsFrom(Annotated<Annotation> annotated) {
        final Iterator<Map.Entry<Class<Annotation>, Annotation>> iter = annotated.forEachAnnotation();
        while (iter.hasNext()) {
            final Map.Entry<Class<Annotation>, Annotation> v = iter.next();
            this.annotations().map.putIfAbsent(v.getKey(), v.getValue());
        }
    }


    /**
     * This allows us to hide the annotation map from the outside
     * but inject it from the class that implements the interface.
     * So we can implement all annotation functionality within this interface
     * instead of each class separately.
     */
    class Annotations<Annotation> implements Cloneable {
        private final Map<Class<Annotation>, Annotation> map;

        public Annotations() {
            this(new HashMap<>());
        }

        public Annotations(Map<Class<Annotation>, Annotation> annotations) {
            this.map = annotations;
        }

        public Annotations<Annotation> clone() {
            final Map<Class<Annotation>, Annotation> cloneMap = new HashMap<>(map);
            Iterator<Map.Entry<Class<Annotation>, Annotation>> iter = map.entrySet().iterator();
            while (iter.hasNext()) {
                final Map.Entry<Class<Annotation>, Annotation> v = iter.next();
                cloneMap.put(v.getKey(), v.getValue());
            }

            return new Annotations<>(cloneMap);
        }
    }
}
