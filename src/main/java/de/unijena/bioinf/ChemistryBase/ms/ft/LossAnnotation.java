package de.unijena.bioinf.ChemistryBase.ms.ft;

public final class LossAnnotation<T> {

    private final int id;
    private final Class<T> klass;
    int capa;
    LossAnnotation<? extends T> alias;

    LossAnnotation(int id, int capa, Class<T> klass) {
        this.id = id;
        this.klass = klass;
        this.capa = capa;
        this.alias = null;
    }

    <S extends T> LossAnnotation(LossAnnotation<S> prev, Class<T> newOne) {
        this.id = prev.id;
        this.klass = newOne;
        this.capa = prev.capa;
        this.alias = prev;
    }

    public T get(Loss loss) {
        return (T) (loss.getAnnotation(id));
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

    public LossAnnotation<? extends T> getAlias() {
        return alias;
    }

    public T getOrCreate(Loss loss) {
        final T obj = get(loss);
        if (obj == null) {
            try {
                final T newObj = (alias != null ? alias.getAnnotationType() : klass).newInstance();
                loss.setAnnotation(id, capa, newObj);
                return newObj;
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        } else return obj;
    }

    public void set(Loss loss, T obj) {
        if (alias != null) throw new UnsupportedOperationException("Cannot set values of alias annotations for alias. Use '" + alias.getAnnotationType().getSimpleName() + "' instead of '" + klass.getSimpleName() + "'");
        loss.setAnnotation(id, capa, obj);
    }

}
