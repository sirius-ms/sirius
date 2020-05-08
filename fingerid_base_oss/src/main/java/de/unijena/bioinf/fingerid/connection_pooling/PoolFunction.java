package de.unijena.bioinf.fingerid.connection_pooling;


import java.io.IOException;
import java.sql.SQLException;

/**
 * Represents a function that accepts one argument and produces a result.
 *
 * <p>This is a <a href="package-summary.html">functional interface</a>
 *
 * @param <R> the type of the result of the function
 * @since 1.8
 */
@FunctionalInterface
public interface PoolFunction<T, R> {

    /**
     * Applies this function to the given argument.
     *
     * @param t the function argument
     * @return the function result
     */
    R apply(PooledConnection<T> t) throws SQLException, IOException;
}
