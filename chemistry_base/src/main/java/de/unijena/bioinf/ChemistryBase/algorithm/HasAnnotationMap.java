package de.unijena.bioinf.ChemistryBase.algorithm;

import com.google.common.base.Function;

import java.util.Map;

public interface HasAnnotationMap {

    /**
     * We have to keep this method public. However, when calling it from outside
     * the map should never get modified!
     */
    Map<Class<?>, Object> getAnnotations();

    default <T> boolean setAnnotation(Class<T> klass, T value) {
        return getAnnotations().put(klass, value)==value;
    }

    default <T> T AddOrReplaceAnnotation(Class<T> klass, T addAnno, Function<T,T> replaceFunctor) {
        final Map<Class<?>, Object> anos = getAnnotations();
        Object o = anos.get(klass);
        final T out = o==null ? addAnno : replaceFunctor.apply((T)o);
        anos.put(klass,out);
        return out;
    }

    default <T> T getOrCreateAnnotation(Class<T> klass, Functor0<T> functor) {
        final Map<Class<?>, Object> anos = getAnnotations();
        Object o = anos.get(klass);
        final T out = o==null ? functor.call() : (T)o;
        anos.put(klass, out);
        return out;
    }

    default <T> T getAnnotation(Class<T> klass, T defaultValue) {
        Object o = getAnnotations().get(klass);
        if (o==null) return defaultValue;
        else return (T)o;
    }

    default <T> T getAnnotationOrThrow(Class<T> klass) {
        Object o = getAnnotations().get(klass);
        if (o==null) throw new NullPointerException("No annotation '" + klass.getName());
        else return (T)o;
    }



}
