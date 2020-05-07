package de.unijena.bioinf.ms.rest.client.utils;

import org.apache.http.client.methods.HttpUriRequest;

import java.io.IOException;
import java.net.URISyntaxException;

@FunctionalInterface
public interface HTTPSupplier<T extends HttpUriRequest> {

    /**
     * Gets a result.
     *
     * @return a result
     */
    T get() throws IOException, URISyntaxException;
}
