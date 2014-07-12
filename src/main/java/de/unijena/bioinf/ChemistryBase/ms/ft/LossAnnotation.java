package de.unijena.bioinf.ChemistryBase.ms.ft;

public final class LossAnnotation<T> {

    private final int id;
    private final Class<T> klass;
    int capa;

    LossAnnotation(int id, int capa, Class<T> klass) {
        this.id = id;
        this.klass = klass;
        this.capa = capa;
    }

    public T get(Loss loss) {
        return (T) (loss.getAnnotation(id));
    }

    public T getOrCreate(Loss loss) {
        final T obj = get(loss);
        if (obj == null) {
            try {
                final T newObj = klass.newInstance();
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
        loss.setAnnotation(id, capa, obj);
    }

}
