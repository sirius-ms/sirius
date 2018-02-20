package de.unijena.bioinf.ChemistryBase.algorithm;

/**
 * Guava collection does not have a class for a 0-argument function. Such a function is quite useful in combination
 * with lambdas, e.g. to write "lamda" something.
 * The closest example for this is Callable, however, Callable does throws an exception.
 */
public interface Functor0<T> {

    public T call();

}
