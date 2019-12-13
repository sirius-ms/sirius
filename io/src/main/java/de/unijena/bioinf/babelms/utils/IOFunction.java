package de.unijena.bioinf.babelms.utils;

import java.io.IOException;

@FunctionalInterface
public interface IOFunction<A,B> {
    B apply(A a) throws IOException;
}
