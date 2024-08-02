package matching.datastructures;

/**
 * An object of this class represents an pair of two objects.<br>
 * These two objects can differ in their type.
 *
 * @param <T> the type of the first object which is contained in this pair
 * @param <E> the type of the second object which is contained in this pair
 */
public class Pair<T,E> {

    /**
     * The first object contained in this pair.
     */
    private T object1;

    /**
     * The second object contained in this pair.
     */
    private E object2;

    /**
     * Constructs a new {@link Pair} object.
     *
     * @param object1 the object that {@link #object1} will be set to
     * @param object2 the object that {@link #object2} will be set to
     */
    public Pair(T object1, E object2){
        this.object1 = object1;
        this.object2 = object2;
    }

    /**
     * Returns the first object which is contained in this Pair object.
     *
     * @return the first object of this Pair object
     */
    public T getObject1(){
        return this.object1;
    }

    /**
     * Returns the second objects which is contained in this Pair object.
     *
     * @return the second object of this Pair object
     */
    public E getObject2(){
        return this.object2;
    }
}
