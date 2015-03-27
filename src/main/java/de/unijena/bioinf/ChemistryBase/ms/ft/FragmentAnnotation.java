package de.unijena.bioinf.ChemistryBase.ms.ft;

public final class FragmentAnnotation<T> {

    protected final int id;
    protected final Class<T> klass;
    int capa;
    FragmentAnnotation<? extends T> alias;

    FragmentAnnotation(int id, int capa, Class<T> klass) {
        this.id = id;
        this.klass = klass;
        this.capa = capa;
        this.alias = null;
    }

    <S extends T> FragmentAnnotation(FragmentAnnotation<S> prev, Class<T> newOne) {
        this.id = prev.id;
        this.klass = newOne;
        this.capa = prev.capa;
        this.alias = prev;
    }

    public T get(Fragment vertex) {
        return (T) (vertex.getAnnotation(id));
    }

    public Class<T> getAnnotationType() {
        return klass;
    }

    public boolean isAlias() {
        return alias!=null;
    }

    public Class<? extends T> getAliasType() {
        return alias.getAnnotationType();
    }

    public FragmentAnnotation<? extends T> getAlias() {
        return alias;
    }

    public T getOrCreate(Fragment vertex) {
        final T obj = get(vertex);
        if (obj == null) {
            try {
                final T newObj = (alias != null ? alias.getAnnotationType() : klass).newInstance();
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
        if (alias != null) throw new UnsupportedOperationException("Cannot set values of alias annotations for alias. Use '" + alias.getAnnotationType().getSimpleName() + "' instead of '" + klass.getSimpleName() + "'");
        vertex.setAnnotation(id, capa, obj);
    }

}
