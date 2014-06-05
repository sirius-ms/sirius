package de.unijena.bioinf.FragmentationTreeConstruction.model.graph;

public final class VertexAnnotation<T> {

    private final int id;
    private final Class<T> klass;

    VertexAnnotation(int id, Class<T> klass) {
        this.id = id;
        this.klass = klass;
    }

    public T get(FTVertex vertex) {
        return (T)(vertex.getAnnotation(id));
    }

    public T getOrCreate(FTVertex vertex) {
        final T obj = get(vertex);
        if (obj == null) {
            try {
                final T newObj = klass.newInstance();
                vertex.setAnnotation(id,  newObj);
                return newObj;
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        } else return obj;
    }

    public void set(FTVertex vertex, T obj) {
        vertex.setAnnotation(id,  obj);
    }

}
