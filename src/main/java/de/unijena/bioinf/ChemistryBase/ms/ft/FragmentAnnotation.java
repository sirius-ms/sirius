package de.unijena.bioinf.ChemistryBase.ms.ft;

public final class FragmentAnnotation<T> {

    private final int id;
    private final Class<T> klass;
    int capa;

    FragmentAnnotation(int id, int capa, Class<T> klass) {
        this.id = id;
        this.klass = klass;
        this.capa = capa;
    }

    public T get(Fragment vertex) {
        return (T) (vertex.getAnnotation(id));
    }

    public Class<T> getAnnotationType() {
        return klass;
    }

    public T getOrCreate(Fragment vertex) {
        final T obj = get(vertex);
        if (obj == null) {
            try {
                final T newObj = klass.newInstance();
                vertex.setAnnotation(id, capa, newObj);
                return newObj;
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        } else return obj;
    }

    public void set(Fragment vertex, T obj) {
        vertex.setAnnotation(id, capa, obj);
    }

}
