package de.unijena.bioinf.babelms.utils;

import java.io.IOException;

@FunctionalInterface
public interface IOSupplier<T> {

    /**
     * Gets a result.
     *
     * @return a result
     */
    T get() throws IOException;
}
