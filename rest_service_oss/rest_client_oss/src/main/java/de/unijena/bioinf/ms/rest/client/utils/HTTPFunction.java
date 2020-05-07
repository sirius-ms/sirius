package de.unijena.bioinf.ms.rest.client.utils;

import org.apache.http.client.methods.HttpUriRequest;

import java.io.IOException;
import java.net.URISyntaxException;

@FunctionalInterface
public interface HTTPFunction<A, B extends HttpUriRequest> {
    B apply(A a) throws IOException, URISyntaxException;
}

