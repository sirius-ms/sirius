package de.unijena.bioinf.FragmentationTreeConstruction.model;

public final class PeakAnnotation<T> {

    private final int id;
    private final Class<T> klass;

    PeakAnnotation(int id, Class<T> klass) {
        this.id = id;
        this.klass = klass;
    }

    public T get(ProcessedPeak peak) {
        return (T)(peak.getAnnotation(id));
    }

    public T getOrCreate(ProcessedPeak peak) {
        final T obj = get(peak);
        if (obj == null) {
            try {
                final T newObj = klass.newInstance();
                peak.setAnnotation(id,  newObj);
                return newObj;
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        } else return obj;
    }

    public void set(ProcessedPeak peak, T obj) {
        peak.setAnnotation(id,  obj);
    }

}
